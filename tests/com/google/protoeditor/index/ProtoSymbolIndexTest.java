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

import com.google.common.collect.Lists;
import com.google.protoeditor.index.ProtoSymbolIndex.ProtoIndexKey;
import com.google.protoeditor.index.ProtoSymbolIndex.ProtoIndexValue;
import com.google.protoeditor.index.ProtoSymbolIndex.ProtoSymbolType;
import com.google.protoeditor.psi.ProtoNamedElement;
import com.google.protoeditor.psi.ProtoTestCase;

import com.intellij.psi.PsiElement;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.FileContentImpl;
import com.intellij.util.indexing.IndexingDataKeys;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ProtoSymbolIndexTest extends ProtoTestCase {

  public void testProtoIndexKeySerializerAndDeserializer() throws Exception {
    ProtoSymbolIndex.ProtoIndexKey indexKey = new ProtoSymbolIndex.ProtoIndexKey(
        ProtoSymbolIndex.ProtoSymbolType.NAMED_ELEMENT, "unittest",
        "/path/to/my/project/src/google3/a/b/c/rpc.proto");
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    DataOutputStream dataOut = new DataOutputStream(os);
    indexKey.serialize(dataOut);
    byte[] bytes = os.toByteArray();
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    ProtoSymbolIndex.ProtoIndexKey deserailzed = ProtoSymbolIndex.ProtoIndexKey.readFrom(
        new DataInputStream(is));
    assertEquals(indexKey, deserailzed);
  }

  public void testProtoIndexValueSerializerAndDeserializer() throws Exception {
    ProtoSymbolIndex.ProtoIndexValue indexKey = new ProtoSymbolIndex.ProtoIndexValue(
        Lists.<Integer>newArrayList(1, 2, 226));
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    DataOutputStream dataOut = new DataOutputStream(os);
    indexKey.serialize(dataOut);
    byte[] bytes = os.toByteArray();
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    ProtoSymbolIndex.ProtoIndexValue deserailzed = ProtoSymbolIndex.ProtoIndexValue.readFrom(
        new DataInputStream(is));
    assertEquals(indexKey, deserailzed);
  }

  public void testProtoSymbolsAddedToIndex() throws Exception {
    saveTestProto();
    String filePath = protoFile.getVirtualFile().getPath();
    ProtoIndexKey enumKey = new ProtoIndexKey(ProtoSymbolType.TOP_LEVEL_DEFINTION, "Response",
        filePath);
    ProtoIndexKey enumNameKey = new ProtoIndexKey(ProtoSymbolType.NAMED_ELEMENT, "Response",
        filePath);
    ProtoIndexKey messageKey = new ProtoIndexKey(ProtoSymbolType.TOP_LEVEL_DEFINTION, "Foo",
        filePath);
    ProtoIndexKey enumValueKey = new ProtoIndexKey(ProtoSymbolType.NAMED_ELEMENT, "YES",
        filePath);
    ProtoIndexKey messageFieldKey = new ProtoIndexKey(ProtoSymbolType.NAMED_ELEMENT, "answer",
        filePath);

    DataIndexer<ProtoIndexKey, ProtoIndexValue,FileContent> protoIndexer =
        new ProtoSymbolIndex().getIndexer();
    Map<ProtoIndexKey, ProtoSymbolIndex.ProtoIndexValue> protoSymbols = protoIndexer.map(
        getFileContentForProtoFile());
    assertEquals(7, protoSymbols.size());
    assertTrue(protoSymbols.keySet().contains(enumKey));
    assertTrue(protoSymbols.keySet().contains(enumNameKey));
    assertTrue(protoSymbols.keySet().contains(messageKey));
    assertTrue(protoSymbols.keySet().contains(enumValueKey));
    assertTrue(protoSymbols.keySet().contains(messageFieldKey));

    ProtoSymbolIndex.ProtoIndexValue enumIndexValue = protoSymbols.get(enumKey);
    assertEquals(1, enumIndexValue.getPathFromRoot().size());

    ProtoSymbolIndex.ProtoIndexValue messageIndexValue = protoSymbols.get(messageKey);
    assertEquals(1, messageIndexValue.getPathFromRoot().size());
  }

  public void testPathFromRootCalculatedCorrectly() throws Exception {
    saveTestProto();
    String filePath = protoFile.getVirtualFile().getPath();
    ProtoIndexKey enumKey = new ProtoIndexKey(ProtoSymbolType.TOP_LEVEL_DEFINTION, "Response",
        filePath);
    ProtoIndexKey messageFieldKey = new ProtoIndexKey(ProtoSymbolType.NAMED_ELEMENT, "answer",
        filePath);

    DataIndexer<ProtoIndexKey, ProtoIndexValue,FileContent> protoIndexer =
        new ProtoSymbolIndex().getIndexer();
    Map<ProtoIndexKey, ProtoSymbolIndex.ProtoIndexValue> protoSymbols = protoIndexer.map(
        getFileContentForProtoFile());
    assertTrue(protoSymbols.keySet().contains(enumKey));
    assertTrue(protoSymbols.keySet().contains(messageFieldKey));

    ProtoSymbolIndex.ProtoIndexValue enumIndexValue = protoSymbols.get(enumKey);
    assertEquals("Response", ((ProtoNamedElement)findElementInTree(enumIndexValue)).getName());


    ProtoSymbolIndex.ProtoIndexValue messageIndexValue = protoSymbols.get(messageFieldKey);
    assertEquals("answer", ((ProtoNamedElement)findElementInTree(messageIndexValue)).getName());
  }

  private FileContent getFileContentForProtoFile() throws Exception {
    FileContent fc = new FileContentImpl(protoFile.getVirtualFile());
    fc.putUserData(IndexingDataKeys.PROJECT, getProject());
    return fc;
  }

  private PsiElement findElementInTree(ProtoIndexValue indexValue) {
    List<Integer> enumPath = indexValue.getPathFromRoot();
    PsiElement enumElement = protoFile;
    for (Integer i : enumPath) {
      enumElement = enumElement.getChildren()[i];
    }
    return enumElement;
  }

  private void saveTestProto() throws IOException {
    writeTestProto(
        "package foo;",
        "enum Response {",
        "  YES = 0;",
        "  NO = 1;",
        "}",
        "message Foo {",
        "  optional Response answer = 1 [default = YES];",
        "}");
  }
}
