/**
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.protoeditor.index;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protoeditor.ProtoFileType;
import com.google.protoeditor.psi.ProtoFile;
import com.google.protoeditor.psi.ProtoNamedElement;
import com.google.protoeditor.psi.ProtoToplevelDefinition;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;

import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Builds a symbol index for proto files. Implements IntelliJ's FileBasedIndexExtension and adds
 * named elements in proto files to IntelliJ's index
 */
// TODO: Need to change this from FileBasedIndex to StubBasedIndex, since PsiFile is used here.
public class ProtoSymbolIndex extends
    FileBasedIndexExtension<ProtoSymbolIndex.ProtoIndexKey, ProtoSymbolIndex.ProtoIndexValue> {

  public enum ProtoSymbolType {
    TOP_LEVEL_DEFINTION,
    NAMED_ELEMENT
  }

  /**
   * Key for storing an entry in the index. Proto symbol type, name and file name uniquely identify
   * a proto index entry.
   */
  public static class ProtoIndexKey {

    /**
     * Deserializes ProtoIndexKey from the given DataInput stream.
     *
     * @param in DataInput to read from
     * @return Deserialzed ProtoIndexKey
     * @throws IOException
     */
    public static ProtoIndexKey readFrom(DataInput in) throws IOException {
      String typeName = in.readUTF();
      ProtoSymbolType symbolType = ProtoSymbolType.valueOf(typeName);
      String symbolName = in.readUTF();
      String filePath = in.readUTF();
      return new ProtoIndexKey(symbolType, symbolName, filePath);
    }

    private final ProtoSymbolType type;
    private final String symbolName;
    private final String filePath;

    public ProtoIndexKey(ProtoSymbolType type, String symbolName, String filePath) {
      this.type = type;
      this.symbolName = symbolName;
      this.filePath = filePath;
    }

    public ProtoSymbolType getType() {
      return type;
    }

    public String getSymbolName() {
      return symbolName;
    }

    public String getFilePath() {
      return filePath;
    }

    /**
     * Serializes ProtoIndexKey to the given DataOutput stream.
     *
     * @param out DataOutput, to write to
     * @throws IOException
     */
    public void serialize(DataOutput out) throws IOException {
      out.writeUTF(type.name());
      out.writeUTF(symbolName);
      out.writeUTF(filePath);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ProtoIndexKey that = (ProtoIndexKey) o;

      if (filePath != null ? !filePath.equals(that.filePath) : that.filePath != null) {
        return false;
      }
      if (symbolName != null ? !symbolName.equals(that.symbolName) : that.symbolName != null) {
        return false;
      }
      if (type != that.type) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = type != null ? type.hashCode() : 0;
      result = 31 * result + (symbolName != null ? symbolName.hashCode() : 0);
      result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return "ProtoIndexKey{" +
          "type=" + type +
          ", symbolName='" + symbolName + '\'' +
          ", filePath='" + filePath + '\'' +
          '}';
    }
  }

  /**
   * Information about a proto symbol saved in index.
   * <p>
   * Saves the path from the root PsiFile to the proto symbol
   */
  public static class ProtoIndexValue {

    /**
     * Deserializes ProtoIndexValue from the given DataInput stream.
     *
     * @param in DataInput to read from
     * @return Deserialzed ProtoIndexValue
     * @throws IOException
     */
    public static ProtoIndexValue readFrom(DataInput in) throws IOException {
      int size = in.readInt();
      List<Integer> nodePath = Lists.newArrayListWithExpectedSize(size);
      for (int i = 0; i < size; i++) {
        nodePath.add(in.readInt());
      }
      return new ProtoIndexValue(nodePath);
    }

    private final List<Integer> pathFromRoot;

    public ProtoIndexValue(List<Integer> pathFromRoot) {
      this.pathFromRoot = pathFromRoot;
    }

    public List<Integer> getPathFromRoot() {
      return pathFromRoot;
    }

    /**
     * Serializes ProtoIndexValue to the given DataOutput stream.
     *
     * @param out DataOutput, to write to.
     * @throws IOException
     */
    public void serialize(DataOutput out) throws IOException {
      out.writeInt(pathFromRoot.size());
      for (Integer nodeOffset : pathFromRoot) {
        out.writeInt(nodeOffset);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ProtoIndexValue that = (ProtoIndexValue) o;

      return (pathFromRoot == null && that.pathFromRoot == null)
          || pathFromRoot.equals(that.pathFromRoot);
    }

    @Override
    public int hashCode() {
      return (pathFromRoot != null ? pathFromRoot.hashCode() : 0);
    }

    @Override
    public String toString() {
      return "ProtoIndexValue{" +
          ", pathFromRoot=" + pathFromRoot +
          '}';
    }
  }

  /**
   * DataIndexer for proto files.
   */
  private static class ProtoDataIndexer implements
      DataIndexer<ProtoIndexKey, ProtoIndexValue, FileContent> {

    /**
     * Returns a map containing all the named elements in a proto file.
     *
     * @param inputData FileContent, passed in by IntelliJ.
     */
    @NotNull
    @Override
    public Map<ProtoIndexKey, ProtoIndexValue> map(FileContent inputData) {
      Map<ProtoIndexKey, ProtoIndexValue> protoSymbols = Maps.newHashMap();
      Project project = inputData.getProject();
      VirtualFile vFile = inputData.getFile();
      PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
      if (! (psiFile instanceof ProtoFile)) {
        throw new IllegalStateException(
            "Proto Indexer should never be called for non-proto files.\n");
      } else {
        ProtoFile protoFile = (ProtoFile) psiFile;
        addAllProtoSymbols(protoFile, protoSymbols);
      }
      return protoSymbols;
    }

    private void addAllProtoSymbols(final ProtoFile protoFile,
        final Map<ProtoIndexKey, ProtoIndexValue> protoSymbols) {

      PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
        public void visitElement(PsiElement element) {
          super.visitElement(element);

          if (element instanceof ProtoNamedElement) {
            addProtoSymbol(element, protoFile, ProtoSymbolType.NAMED_ELEMENT, protoSymbols);
          }
          if (element instanceof ProtoToplevelDefinition) {
            addProtoSymbol(element, protoFile, ProtoSymbolType.TOP_LEVEL_DEFINTION, protoSymbols);
          }
        }
      };
      protoFile.accept(visitor);
    }

    private void addProtoSymbol(PsiElement element, ProtoFile protoFile,
        ProtoSymbolType symbolType, Map<ProtoIndexKey, ProtoIndexValue> protoSymbols) {
      ProtoNamedElement namedElement = (ProtoNamedElement) element;
      String name = namedElement.getName();
      if (Strings.isNullOrEmpty(name)) {
        return;
      }
      protoSymbols.put(new ProtoIndexKey(symbolType, name, protoFile.getVirtualFile().getPath()),
          new ProtoIndexValue(makePathFromRoot(protoFile, element)));
    }

    private List<Integer> makePathFromRoot(ProtoFile protoFile, PsiElement element) {
      List<Integer> path = Lists.newArrayList();
      PsiElement child = element;
      PsiElement parent;
      while (child != protoFile) {
        parent = child.getParent();
        int childIndex = Arrays.<PsiElement>asList(parent.getChildren()).indexOf(child);
        path.add(0, childIndex);
        child = parent;
      }

      PsiElement root = protoFile;
      for (Integer i : path) {
        root = root.getChildren()[i];
      }
      assert root == element;
      return path;
    }
  }

  public static final ID<ProtoIndexKey, ProtoIndexValue> NAME = ID.create("ProtoSymbolIndex");

  @Override
  public ID<ProtoIndexKey, ProtoIndexValue> getName() {
    return NAME;
  }

  /**
   * {@inheritDoc}
   * <p>
   * DataIndexer for proto files indexes all the proto symbols and also saves path to that symbol
   * in the PSI tree for fast retrieval.
   */
  @Override
  public DataIndexer<ProtoIndexKey, ProtoIndexValue, FileContent> getIndexer() {
    return new ProtoDataIndexer();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the KeyDescriptor for proto index.
   */
  @Override
  public KeyDescriptor<ProtoIndexKey> getKeyDescriptor() {
    return new KeyDescriptor<ProtoIndexKey>() {
      @Override
      public void save(DataOutput out, ProtoIndexKey value) throws IOException {
        value.serialize(out);
      }

      @Override
      public ProtoIndexKey read(DataInput in) throws IOException {
        return ProtoIndexKey.readFrom(in);
      }

      @Override
      public int getHashCode(ProtoIndexKey value) {
        return value.hashCode();
      }

      @Override
      public boolean isEqual(ProtoIndexKey val1, ProtoIndexKey val2) {
        return val1.equals(val2);
      }
    };
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns DataExternalizer that saves and reads ProtoIndexValue.
   */
  @Override
  public DataExternalizer<ProtoIndexValue> getValueExternalizer() {
    return new DataExternalizer<ProtoIndexValue>() {
      @Override
      public void save(DataOutput out, ProtoIndexValue value) throws IOException {
        value.serialize(out);
      }

      @Override
      public ProtoIndexValue read(DataInput in) throws IOException {
        return ProtoIndexValue.readFrom(in);
      }
    };
  }

  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return new FileBasedIndex.InputFilter() {
      @Override
      public boolean acceptInput(VirtualFile file) {
        return file.getFileType() instanceof ProtoFileType;
      }
    };
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 1;
  }
}
