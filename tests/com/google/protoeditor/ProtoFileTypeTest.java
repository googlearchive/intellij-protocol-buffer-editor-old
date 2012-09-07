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

package com.google.protoeditor;

import com.google.protoeditor.psi.ProtoFile;
import com.google.protoeditor.psi.ProtoTestCase;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.ModuleTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;

public class ProtoFileTypeTest extends ModuleTestCase {

  private static final String TEST_PROTO = "test.proto";

  private IdeaProjectTestFixture fixture;
  private LightVirtualFile testFile;

  @Override
  protected void setUp() throws Exception {
    System.setProperty("idea.platform.prefix", "Idea");
    ProtoTestCase.setIfNotSet("idea.load.plugins.id", "com.google.protoeditor");
    super.setUp();

    TestFixtureBuilder<IdeaProjectTestFixture> builder =
        IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder();
    fixture = builder.getFixture();
    fixture.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    fixture.tearDown();
    super.tearDown();
  }

  public void testProtoFileRecognized() throws Exception {
    testFile = new ProtoLightVirtualFile(TEST_PROTO, "");

    assertNotNull(testFile);
    PsiFile psiFile =
        PsiManager.getInstance(fixture.getProject()).findFile(testFile);
    assertNotNull(psiFile);
    assertTrue("File is recognized as proto file", psiFile instanceof ProtoFile);
  }
}
