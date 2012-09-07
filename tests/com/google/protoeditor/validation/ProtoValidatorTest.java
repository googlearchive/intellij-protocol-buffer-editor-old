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

package com.google.protoeditor.validation;

import com.google.protoeditor.psi.ProtoDefaultValue;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoServiceDefinition;
import com.google.protoeditor.psi.ProtoSimpleProperty;
import com.google.protoeditor.psi.ProtoTestCase;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import org.easymock.EasyMock;

import java.util.List;

/**
 * Test cases for {@link ProtoValidator}.
 */
public class ProtoValidatorTest extends ProtoTestCase {

  public void testCheckParentNameClashInProto2() throws Exception {

    writeTestProto("syntax = \"proto2\"",
                   "option java_api_version = 1;",
                   "option java_package = \"com.google.rules\";",
                   "option java_use_javaproto2 = true;",
                   "option java_java5_enums = true;",
                   "package com.google.rules;",
                   "message Link {",
                   "  required string link = 1;",
                   "  required string target = 2;",
                   "}");
    final ProtoValidator validator = new ProtoValidator();

    final AnnotationHolder annotationHolder = EasyMock.createMock(AnnotationHolder.class);
    EasyMock.replay(annotationHolder);
    protoFile.accept(new PsiRecursiveElementVisitor() {
      public void visitElement(PsiElement psiElement) {
        if (psiElement instanceof ProtoProperty) {
          validator.checkParentNameClash((ProtoProperty) psiElement, annotationHolder);
        }
        super.visitElement(psiElement);
      }
    });
    EasyMock.verify(annotationHolder);
  }

  public void testCheckParentNameClash() throws Exception {

    writeTestProto("message Link {",
                   "  required string link = 1;",
                   "  required string target = 2;",
                   "}");
    final ProtoValidator validator = new ProtoValidator();

    final AnnotationHolder annotationHolder = EasyMock.createMock(AnnotationHolder.class);
    final Annotation annotation = new Annotation(0, 0, HighlightSeverity.WARNING, "", "");
    EasyMock.expect(annotationHolder.createErrorAnnotation(
        EasyMock.isA(PsiElement.class),
        EasyMock.isA(String.class))).andReturn(annotation);
    EasyMock.replay(annotationHolder);
    protoFile.accept(new PsiRecursiveElementVisitor() {
      public void visitElement(PsiElement psiElement) {
        if (psiElement instanceof ProtoProperty) {
          validator.checkParentNameClash((ProtoProperty) psiElement, annotationHolder);
        }
        super.visitElement(psiElement);
      }
    });
    EasyMock.verify(annotationHolder);
  }

  public void testFQMessageNameIsOK() throws Exception {

    doTestFQMessageNameIsOK(
        "syntax = \"proto2\";",
        "package foo;",
        "message SearchResponse {",
        "  message Result {",
        "    required string Bar = 1;",
        "  }",
        "  repeated Result result = 1;",
        "}",
        "message SomeOtherMessage {",
        "  optional SearchResponse.Result result = 1;",
        "}");
  }

  public void testFQMessageNameIsOK2() throws Exception {
    doTestFQMessageNameIsOK(
        "parsed message Foo {",
        "  optional message<path.to.Metadata>",
        "      metadata = 1;",
        "  repeated message<path.to.Activity> activity = 2;",
        "  optional string continuation_token = 3;",
        "}");
  }

  public void testFQMessageNameIsOK3() throws Exception {
    doTestFQMessageNameIsOK(
        "import \"net/proto2/proto/descriptor.proto\";", // google/protobuf/descriptor.proto in O/S
        "extend google.protobuf.MessageOptions {",
        "  optional string my_option = 51234;",
        "}",
        "message MyMessage {",
        "  option (my_option) = \"Hello world!\";",
        "}");
  }

  private void doTestFQMessageNameIsOK(String ... lines) throws Exception {
    writeTestProto(lines);
    final ProtoValidator validator = new ProtoValidator();

    final AnnotationHolder annotationHolder = EasyMock.createMock(AnnotationHolder.class);
    EasyMock.replay(annotationHolder);
    protoFile.accept(new PsiRecursiveElementVisitor() {
      public void visitElement(PsiElement psiElement) {
        if (psiElement instanceof LeafPsiElement) {
          validator.checkLeafs((LeafPsiElement) psiElement, annotationHolder);
        }
        super.visitElement(psiElement);
      }
    });
    EasyMock.verify(annotationHolder);
  }

  public void testDefaultValueWithHexLiteral() throws Exception {
    writeTestProto("message MyMessage {",
        "  optional int32 name = 1 [ default = 0xFF ];" ,
        "  optional int32 new_name = 2 [ default = 0X1 ];" ,
        "}");
    assertNoParseErrors(protoFile);
  }

  public void testDefaultValueWithIntLiteral() throws Exception {
    writeTestProto("message MyMessage {",
        "  optional int32 name = 1 [ default = 1];" ,
        "}");
    assertNoParseErrors(protoFile);
  }

  public void testDefaultValueWithBadIntLiteral() throws Exception {
    writeTestProto("message MyMessage {",
        "  optional int32 name = 1 [ default = 0x1FH ];",
        "}");

    ProtoMessageDefinition message = getOnlyMessage(protoFile, "MyMessage");
    ProtoProperty prop = getOnlyProperty(message);
    assertError(prop, "expected ',', but got 'H'");
  }

  public void testRpcWithoutBody() throws Exception {
    writeTestProto("service MyService {",
        "  rpc myRpc (inputMsg) returns (ouputMsg);",
        "}");

    ProtoServiceDefinition foo = getServiceDefinitions();
    assertEquals(foo.getName(), "MyService");
    assertNoError(foo);
    List<ProtoRpcDefinition> rpcDefn = foo.getRpcDefinitions();
    assertOneElement(rpcDefn);
    assertNoError(rpcDefn.get(0));
  }

  public void testServiceWithoutBody() throws Exception {
    writeTestProto("service MyService;");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertError(foo, "expected '{', but got ';'");
  }

  public void testMessageWithoutBody() throws Exception {
    writeTestProto("message MyMessage;");

    ProtoMessageDefinition message = getOnlyMessage(protoFile, "MyMessage");
    assertNoError(message);
    assertNull(message.getDefinitionBody());
  }

  public void testDefaultValueWithValidUInt32() throws Exception {
    writeTestProto("message Foo {",
        "  optional uint32 someint = 1 [default = 10]",
        "}");
    final ProtoValidator validator = new ProtoValidator();

    final AnnotationHolder annotationHolder = EasyMock.createMock(AnnotationHolder.class);
    EasyMock.replay(annotationHolder);
    protoFile.accept(new PsiRecursiveElementVisitor() {
      public void visitElement(PsiElement psiElement) {
        if (psiElement instanceof ProtoSimpleProperty) {
          validator.checkDefaultValueType((ProtoSimpleProperty) psiElement, annotationHolder);
        }
        super.visitElement(psiElement);
      }
    });
    EasyMock.verify(annotationHolder);
  }

  public void testDefaultValueWithNegativeUInt32() throws Exception {
    writeTestProto("message Foo {",
        "  optional uint32 someint = 1 [default = -10]",
        "}");
    final ProtoValidator validator = new ProtoValidator();

    ProtoDefaultValue value = ((ProtoSimpleProperty) getOnlyProperty(
        protoFile.getMessageDefinition("Foo"))).getDefaultValue();
    final AnnotationHolder annotationHolder = EasyMock.createMock(AnnotationHolder.class);
    final Annotation annotation = new Annotation(0, 0, HighlightSeverity.WARNING, "", "");
    EasyMock.expect(annotationHolder.createErrorAnnotation(
        value, "cannot assign number to uint32")).andReturn(annotation);
    EasyMock.replay(annotationHolder);
    protoFile.accept(new PsiRecursiveElementVisitor() {
      public void visitElement(PsiElement psiElement) {
        if (psiElement instanceof ProtoSimpleProperty) {
          validator.checkDefaultValueType((ProtoSimpleProperty) psiElement, annotationHolder);
        }
        super.visitElement(psiElement);
      }
    });
    EasyMock.verify(annotationHolder);
  }
}
