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

package com.google.protoeditor.psi;

import com.google.protoeditor.ProtoFileType;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProtoMessageTypeReference extends AbstractProtoElement
    implements ProtoElement, PsiReference {

  private static final String PROTOCOL_MESSAGE_FQN
      = "com.google.io.protocol.ProtocolMessage";

  public ProtoMessageTypeReference(ASTNode astNode) {
    super(astNode);
  }

  public
  @Nullable
  String getReferencedName() {
    ASTNode node = getIdentifierNode();
    return node == null ? "" : node.getText();
  }

  private
  @Nullable
  ASTNode getIdentifierNode() {
    return getNode().findChildByType(ProtoTokenTypes.IDENTIFIER);
  }

  public PsiReference getReference() {
    return this;
  }

  public PsiElement getElement() {
    return this;
  }

  public TextRange getRangeInElement() {
    return new TextRange(0, getTextLength());
  }

  @Nullable
  public PsiElement resolve() {
    String referencedName = getReferencedName();
    if (referencedName == null) {
      return null;
    }

    SingleElementResolver resolver = new SingleElementResolver(
        referencedName);
    resolver.find();
    return resolver.getElement();
  }

  //TODO: Investigate if Safe Delete will correctly find usages.
  public Object[] getVariants() {
    PossibleResolutionsFinder finder = new PossibleResolutionsFinder();
    finder.find();
    Set<ProtoMessageDefinition> defs = finder.getDefResolutions();
    Set<PsiClass> classes = finder.getClassResolutions();
    List<PsiElement> els = new ArrayList<PsiElement>(defs.size() + classes.size());
    Set<String> usedNames = new HashSet<String>(els.size());
    for (ProtoMessageDefinition definition : defs) {
      if (usedNames.add(definition.getName())) {
        els.add(definition);
      }
    }
    for (PsiClass psiClass : classes) {
      if (usedNames.add(psiClass.getName())) {
        els.add(psiClass);
      }
    }

    return els.toArray(new Object[els.size()]);
  }

  private boolean isProtoMessageSubclass(PsiClass cls) {
    return cls != null && ProtoPsiTools.isSuperclass(cls, PROTOCOL_MESSAGE_FQN);
  }

  public String getCanonicalText() {
    return getReferencedName();
  }

  public PsiElement handleElementRename(String newElementName)
      throws IncorrectOperationException {
    setReferencedName(newElementName);
    return this;
  }

  public PsiElement bindToElement(PsiElement element)
      throws IncorrectOperationException {
    if (element instanceof ProtoMessageDefinition) {
      ProtoMessageDefinition def = (ProtoMessageDefinition) element;

      setReferencedName(def.getName());

    } else if (element instanceof PsiClass) {
      PsiClass psiClass = (PsiClass) element;
      setReferencedName(psiClass.getName());
    }

    return this;
  }

  private void setReferencedName(String name) throws
                                              IncorrectOperationException {
    ASTNode oldNode = getIdentifierNode();
    if (oldNode == null) {
      throw new IncorrectOperationException("no identifier node");
    }
    ASTNode newNode = ProtoChangeTools.createIdentifierFromText(
        getProject(), name);
    oldNode.getTreeParent().replaceChild(oldNode, newNode);
  }

  @SuppressWarnings({"SimplifiableConditionalExpression"})
  public boolean isReferenceTo(PsiElement element) {
    PsiElement resolved = resolve();
    return resolved == null ? false : resolved.equals(element);
  }

  public boolean isSoft() {
    return false;
  }

  private abstract class ResolutionFinder {

    public void find() {
      ProtoFile protoFile = PsiTreeUtil.getParentOfType(
          ProtoMessageTypeReference.this, ProtoFile.class);
      if (protoFile == null) {
        return;
      }
      for (ProtoMessageDefinition def : protoFile
          .getMessageDefinitions()) {
        if (!handleDef(def)) {
          return;
        }
      }

      PsiManager mgr = getManager();
      final String ourPkgName = ProtoPsiTools.getContainingPackage(protoFile);
      final Set<String> pkgNames = new LinkedHashSet<String>();
      final Set<String> clsNames = new LinkedHashSet<String>();
      if (ourPkgName != null) {
        pkgNames.add(ourPkgName);
      }

      for (PsiImportList list : protoFile.getImports()) {
        for (PsiImportStatement statement : list
            .getImportStatements()) {
          String fqn = statement.getQualifiedName();
          if (statement.isOnDemand()) {
            pkgNames.add(fqn);
          } else {
            clsNames.add(fqn);
          }
        }
      }

      final FileDocumentManager docMgr = FileDocumentManager.getInstance();
      final PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(getProject());

      for (Module module : ModuleManager.getInstance(getProject())
          .getModules()) {
        ModuleRootManager mrm = ModuleRootManager.getInstance(module);
        MyContentIterator iterator = new MyContentIterator(
            docMgr, psiMgr, pkgNames, clsNames);
        mrm.getFileIndex().iterateContent(iterator);
        if (iterator.wasStopped()) {
          return;
        }
      }

      final JavaPsiFacade javaPsiMgr = JavaPsiFacade.getInstance(getProject());
      GlobalSearchScope scope = getResolveScope();
      if (ourPkgName != null) {
        PsiPackage psiPackage = javaPsiMgr.findPackage(ourPkgName);
        if (psiPackage != null) {
          for (PsiClass cls : psiPackage.getClasses(scope)) {
            if (isProtoMessageSubclass(cls)) {
              if (!handleClass(cls)) {
                return;
              }
            }
          }
        }
      }

      for (PsiImportList list : protoFile.getImports()) {
        for (PsiImportStatement statement : list
            .getImportStatements()) {
          PsiElement resolved = statement.resolve();
          if (resolved instanceof PsiPackage) {
            PsiPackage psiPackage = (PsiPackage) resolved;

            for (PsiClass cls : psiPackage.getClasses(scope)) {
              if (isProtoMessageSubclass(cls)) {
                if (!handleClass(cls)) {
                  return;
                }
              }
            }
          } else if (resolved instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) resolved;

            clsNames.add(psiClass.getQualifiedName());
            if (isProtoMessageSubclass(psiClass)) {
              if (!handleClass(psiClass)) {
                return;
              }
            }
          }
        }
      }
    }

    protected abstract boolean handleDef(ProtoMessageDefinition def);

    protected abstract boolean handleClass(PsiClass cls);

    private class MyContentIterator implements ContentIterator {

      private final FileDocumentManager docMgr;
      private final PsiDocumentManager psiMgr;
      private final Set<String> pkgNames;
      private final Set<String> clsNames;
      private boolean stopped = false;

      public MyContentIterator(FileDocumentManager docMgr,
                               PsiDocumentManager psiMgr, Set<String> pkgNames,
                               Set<String> clsNames) {
        this.docMgr = docMgr;
        this.psiMgr = psiMgr;
        this.pkgNames = pkgNames;
        this.clsNames = clsNames;
      }

      public boolean processFile(VirtualFile fileOrDir) {
        if (!(fileOrDir.getFileType() instanceof ProtoFileType)) {
          return true;
        }
        Document doc = docMgr.getDocument(fileOrDir);
        if (doc == null) {
          return true;
        }
        PsiFile psiFile = psiMgr.getPsiFile(doc);
        if (!(psiFile instanceof ProtoFile)) {
          return true;
        }
        ProtoFile protoFile = (ProtoFile) psiFile;
        ProtoPackageStatement pkgStmt = protoFile.getPackageStatement();
        if (pkgStmt == null) {
          return true;
        }
        ProtoPackageNameReference pkgRef = pkgStmt.getPackageNameReference();
        if (pkgRef == null) {
          return true;
        }
        String referencedPackageName = pkgRef.getReferencedPackageName();
        if (referencedPackageName != null) {
          if (pkgNames.contains(referencedPackageName)) {
            for (ProtoMessageDefinition def : protoFile
                .getMessageDefinitions()) {
              if (!handleDef(def)) {
                stopped = true;
                return false;
              }
            }
          } else {
            for (ProtoMessageDefinition def : protoFile
                .getMessageDefinitions()) {
              if (clsNames.contains(referencedPackageName + "."
                                    + def.getName())) {
                if (!handleDef(def)) {
                  stopped = true;
                  return false;
                }
              }
            }
          }
        }
        return true;
      }

      public boolean wasStopped() {
        return stopped;
      }
    }
  }

  private class PossibleResolutionsFinder extends ResolutionFinder {

    private Set<ProtoMessageDefinition> defResolutions
        = new LinkedHashSet<ProtoMessageDefinition>();
    private Set<PsiClass> classResolutions = new LinkedHashSet<PsiClass>();

    public Set<PsiClass> getClassResolutions() {
      return classResolutions;
    }

    public Set<ProtoMessageDefinition> getDefResolutions() {
      return defResolutions;
    }

    protected boolean handleDef(ProtoMessageDefinition def) {
      defResolutions.add(def);
      return true;
    }

    protected boolean handleClass(PsiClass cls) {
      classResolutions.add(cls);
      return true;
    }
  }

  private class SingleElementResolver extends ResolutionFinder {

    private String name;
    private PsiElement element = null;

    public SingleElementResolver(String name) {
      this.name = name;
    }

    public PsiElement getElement() {
      return element;
    }

    protected boolean handleDef(ProtoMessageDefinition def) {
      String name = def.getName();
      if (name != null && name.equals(this.name)) {
        element = def;
        return false;
      } else {
        return true;
      }
    }

    protected boolean handleClass(PsiClass cls) {
      String name = cls.getName();
      if (name != null && name.equals(this.name)) {
        element = cls;
        return false;
      } else {
        return true;
      }
    }
  }
}
