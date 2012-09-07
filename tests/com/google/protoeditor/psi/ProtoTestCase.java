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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.protoeditor.ProtoLightVirtualFile;
import com.google.protoeditor.validation.ProtoValidator;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;

import org.easymock.EasyMock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Common code for proto-editor tests
 */
public class ProtoTestCase extends IdeaTestCase {

  public static final String TEST_PROTO = "test.proto";

  protected IdeaProjectTestFixture fixture;
  protected ProtoFile protoFile;
  protected VirtualFile virtualTestFile;

  @Override
  protected void setUp() throws Exception {
    System.setProperty("idea.platform.prefix", "Idea");
    setIfNotSet("idea.load.plugins.id", "com.google.protoeditor");
    super.setUp();

    TestFixtureBuilder<IdeaProjectTestFixture> builder =
        IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder();
    fixture = builder.getFixture();
    fixture.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    protoFile = null;
    if (virtualTestFile != null) {
      virtualTestFile.delete(this);
    }
    fixture.tearDown();
    super.tearDown();
  }

  protected void writeTestProto(String... lines) throws IOException {
    if (virtualTestFile == null) {
      virtualTestFile = new ProtoLightVirtualFile(TEST_PROTO, Joiner.on("\n").join(lines));
    }
    protoFile = openProtoFile(virtualTestFile);
  }

  public static void setIfNotSet(String property, String value) {
    String prop = System.getProperty(property);
    if (prop.equals("")) {
      System.setProperty(property, value);
    }
  }

  protected void assertCheckEnum(
      ProtoMessageDefinition foo, boolean isError, String expectedMessage) {
    AnnotationHolder annotationHolder = EasyMock.createMock(AnnotationHolder.class);
    if (isError) {
      EasyMock.expect(
          annotationHolder.createErrorAnnotation(
              EasyMock.isA(PsiElement.class),
              EasyMock.eq(expectedMessage)))
          .andReturn(new Annotation(0, 0, null, "", ""))
          .times(2);
    } else {
      EasyMock.expect(
          annotationHolder.createWarningAnnotation(
              EasyMock.isA(PsiElement.class),
              EasyMock.eq(expectedMessage)))
          .andReturn(new Annotation(0, 0, null, "", ""))
          .times(2);
    }
    EasyMock.replay(annotationHolder);
    ProtoValidator protoValidator = new ProtoValidator();
    protoValidator.checkEnums(foo.getDefinitionBody(), annotationHolder);
    EasyMock.verify(annotationHolder);
  }

  protected ProtoFile openProtoFile(File fileContainingProto) throws FileNotFoundException {
    return openProtoFile(LocalFileSystem.getInstance().findFileByIoFile(fileContainingProto));
  }

  protected ProtoFile openProtoFile(VirtualFile fileContainingProto) throws FileNotFoundException {
    if (fileContainingProto == null || !fileContainingProto.exists()) {
      throw new FileNotFoundException("Proto file " + fileContainingProto.getPath()
          + " doesn't exist.");
    }
    assertNotNull(fileContainingProto);
    fileContainingProto.refresh(true, true);
    PsiFile psiFile =
        PsiManager.getInstance(fixture.getProject()).findFile(fileContainingProto);
    assertNotNull(psiFile);
    return (ProtoFile) psiFile;
  }

  protected ProtoMessageDefinition getOnlyMessage(
      ProtoFile protoFile, String expectedMessageName) {
    List<ProtoToplevelDefinition> defs = protoFile.getTopLevelDefinitions();
    ProtoMessageDefinition def = (ProtoMessageDefinition) Iterables.getOnlyElement(defs);
    assertEquals(expectedMessageName, def.getName());
    return def;
  }

  protected ProtoMessageDefinition getMessageByName(
      ProtoFile protoFile, final String expectedMessageName) {
    List<ProtoToplevelDefinition> defs = protoFile.getTopLevelDefinitions();
    ProtoMessageDefinition def = (ProtoMessageDefinition) Iterables.getOnlyElement(Iterables.filter(
        defs,
        new Predicate<ProtoToplevelDefinition>() {

          public boolean apply(ProtoToplevelDefinition topLevelDefinition) {
            return expectedMessageName.equals(topLevelDefinition.getName());
          }
        }));
    return def;
  }

  protected Iterable<ProtoEnumDefinition> getTopLevelEnumerations() {
    List<ProtoToplevelDefinition> defs = protoFile.getTopLevelDefinitions();
    Iterable<ProtoToplevelDefinition> enums = Iterables
        .filter(defs, new Predicate<ProtoToplevelDefinition>() {

          public boolean apply(ProtoToplevelDefinition topLevelDefinition) {
            return (topLevelDefinition instanceof ProtoEnumDefinition);
          }
        });

    return Iterables.transform(enums,
        new Function<ProtoToplevelDefinition, ProtoEnumDefinition>() {

          @Override
          public ProtoEnumDefinition apply(
              @Nullable ProtoToplevelDefinition protoToplevelDefinition) {
            return (ProtoEnumDefinition) protoToplevelDefinition;
          }
        });

  }

  protected ProtoEnumDefinition getOnlyTopLevelEnumeration() {
    return Iterables.getOnlyElement(getTopLevelEnumerations());
  }

  protected ProtoExtensionsStatement getOnlyExtensionsStatement(ProtoMessageDefinition message) {
     return Iterables.getOnlyElement(message.getDefinitionBody().getExtensionsDeclarations());
  }

  protected ProtoProperty getOnlyProperty(ProtoMessageDefinition foo) {
    return Iterables.getOnlyElement(foo.getDefinitionBody().getProperties());
  }

  protected List<ProtoEnumConstant> getEnumConstants(ProtoEnumDefinition foo) {
    return foo.getConstants();
  }

  protected ProtoServiceDefinition getServiceDefinitions() {
    return Iterables.getOnlyElement(protoFile.getServiceDefinitions());
  }

  protected void assertPackageName(ProtoFile protoFile, String expectedPackageName) {
    assertEquals(
        expectedPackageName,
        protoFile.getPackageStatement().getPackageNameReference().getCanonicalText());
  }

  protected void assertSimpleProperty(
      ProtoProperty prop, String expectedName, String expectedType) {
    assertEquals(expectedName, prop.getName());
    ProtoSimplePropertyType typeElement = ((ProtoSimpleProperty) prop).getTypeElement();
    assertNotNull(typeElement);
    assertEquals(expectedType, typeElement.getType().getIdentifierText());
  }

  protected void assertEnumProperty(
      ProtoProperty prop, String expectedName, String expectedType) {
    assertEnumProperty(prop, expectedName, expectedType, null);
  }

  protected void assertEnumProperty(
      ProtoProperty prop, String expectedName, String expectedType, String expectedDefaultValue) {
    assertEquals(expectedName, prop.getName());
    ProtoEnumPropertyType typeElement =
        ((ProtoEnumProperty) prop).getTypeElement();
    assertNotNull(typeElement);
    assertEquals(expectedType, typeElement.getType());

    if (expectedDefaultValue != null) {
      ProtoDefaultValue defaultValue =
          ((ProtoEnumProperty) prop).getDefaultValue();

      assertNotNull(defaultValue);
      ProtoNameElement enumNameElement = defaultValue.getEnumNameElement();
      assertNotNull(enumNameElement);
      assertEquals(expectedDefaultValue, enumNameElement.getText());
    }
  }

  protected void assertError(AbstractProtoElement prop, String expectedError) {
    assertEquals(expectedError, prop.getError());
  }

  protected void assertNoError(AbstractProtoElement prop) {
    assertNull(prop.getError());
  }

  protected void assertNoParseErrors(final ProtoFile file) {
    PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
      @Override public void visitElement(PsiElement element) {
        super.visitElement(element);
        if (element instanceof AbstractProtoElement) {
          String error = ((AbstractProtoElement) element).getError();
          VirtualFile vFile = file.getVirtualFile();
          int lineNumber = -1;
          String filename = null;
          if (vFile != null) {
            filename = vFile.getPath();
            Document doc = FileDocumentManager.getInstance().getDocument(vFile);
            lineNumber = doc.getLineNumber(element.getTextOffset());
          }

          assertNull("Parse error encountered for " + ((AbstractProtoElement) element).getName()
              + (lineNumber != -1 ? " at line "  + lineNumber : "")
              + (filename != null ? " in file " + filename : "")
              + "\nERROR::" + error, error);
        }
      }
    };
    file.accept(visitor);
  }

  public void assertAnyOrder(Iterable<String> actual, Iterable<String> expected) {
    HashMultiset<String> actualSet = HashMultiset.create(actual);
    HashMultiset<String> expectedSet = HashMultiset.create(expected);
    String failureMessage = "Expected List: " + expectedSet
        + " does not match actual list: " + actualSet;
    assertEquals(failureMessage, expectedSet, actualSet);
  }
}
