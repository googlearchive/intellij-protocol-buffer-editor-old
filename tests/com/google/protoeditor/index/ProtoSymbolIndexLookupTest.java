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

import com.google.protoeditor.psi.ProtoTestCase;

import com.intellij.ide.caches.FileContent;
import com.intellij.navigation.NavigationItem;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.IndexingDataKeys;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ProtoSymbolIndexLookupTest extends ProtoTestCase {

  private FileContent fc;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    saveTestProto();
    fc = new FileContent(protoFile.getVirtualFile());
    fc.putUserData(IndexingDataKeys.PROJECT, getProject());
    FileBasedIndex.getInstance().indexFileContent(getProject(), fc);
  }

  public void testCorrectProtoNamesReturnedByIndex() throws Exception {
    List<String> topLevelSymbols = Arrays.asList(new ProtoSymbolIndexLookup().getProtoNames(
        ProtoSymbolIndex.ProtoSymbolType.TOP_LEVEL_DEFINTION, getProject()));
    assertTrue(topLevelSymbols.contains("SearchResponse"));
    assertTrue(topLevelSymbols.contains("Result"));
    assertTrue(topLevelSymbols.contains("Response"));

    List<String> allSymbols = Arrays.asList(new ProtoSymbolIndexLookup().getProtoNames(
        ProtoSymbolIndex.ProtoSymbolType.NAMED_ELEMENT, getProject()));
    assertTrue(allSymbols.contains("SearchResponse"));
    assertTrue(allSymbols.contains("Result"));
    assertTrue(allSymbols.contains("Response"));
    assertTrue(allSymbols.contains("Bar"));
    assertTrue(allSymbols.contains("PEAR"));
    assertTrue(allSymbols.contains("result"));

  }

  public void testCorrectProtoNavigationElementsReturnedByIndex() throws Exception {
    List<NavigationItem> messageEl = Arrays.asList(
        new ProtoSymbolIndexLookup().getProtoItemsByName(
        "SearchResponse", getProject(), false,
        ProtoSymbolIndex.ProtoSymbolType.TOP_LEVEL_DEFINTION, fc.getVirtualFile()));
    assertEquals(1, messageEl.size());
    assertEquals("SearchResponse", messageEl.get(0).getName());

    List<NavigationItem> symbolEl = Arrays.asList(
        new ProtoSymbolIndexLookup().getProtoItemsByName(
        "PEAR", getProject(), false,
        ProtoSymbolIndex.ProtoSymbolType.NAMED_ELEMENT, fc.getVirtualFile()));
    assertEquals(1, symbolEl.size());
    assertEquals("PEAR", symbolEl.get(0).getName());
  }

  private void saveTestProto() throws IOException {
    writeTestProto(
        "message SearchResponse {",
        "  message Result {",
        "    enum Response {",
        "      PEAR = 1;",
        "    }",
        "    optional Response Bar = 1 [default = PEAR];",
        "  }",
        "  repeated Result result = 1;",
        "}");

  }
}
