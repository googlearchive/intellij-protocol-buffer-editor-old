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

import com.google.protoeditor.highlighting.ProtoSyntaxHighlighter;
import com.google.protoeditor.lex.ProtoTextAttributes;
import com.google.protoeditor.psi.ProtoAbstractIntegerLiteral;
import com.google.protoeditor.psi.ProtoDefinitionBody;
import com.google.protoeditor.psi.ProtoElement;
import com.google.protoeditor.psi.ProtoElementWithName;
import com.google.protoeditor.psi.ProtoEnumConstant;
import com.google.protoeditor.psi.ProtoEnumDefinition;
import com.google.protoeditor.psi.ProtoFile;
import com.google.protoeditor.psi.ProtoFileOptionStatement;
import com.google.protoeditor.psi.ProtoGroupDefinition;
import com.google.protoeditor.psi.ProtoHexLiteral;
import com.google.protoeditor.psi.ProtoIntegerLiteral;
import com.google.protoeditor.psi.ProtoKeyword;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoMessageProperty;
import com.google.protoeditor.psi.ProtoMessageTypeReference;
import com.google.protoeditor.psi.ProtoNamedElement;
import com.google.protoeditor.psi.ProtoPackageStatement;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoServiceDefinition;
import com.google.protoeditor.psi.ProtoSimpleProperty;
import com.google.protoeditor.psi.ProtoSyntaxStatement;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// TODO: Clean up this class when implementing syntax highlighting for new features.
// Fix old comments, and implement old TODOs.
public class ProtoAnnotator implements Annotator {

  //TODO: show gutter marker for classes generated from proto classes
  //TODO: add completion for keywords
  //TODO: resolve message names from other proto-files and from classpath
  //TODO: move members refactoring (to move elements between files)
  //TODO: show "reassign ID" as intention
  //TODO: convert some warnings to inspections
  //TODO: add intention for assigning id when missing id or =id for property and enum constant
  //TODO: accept "," in constant list, provide intention to convert to ";"
  //TODO: add refactoring: convert group to message

  private final Map<Class, ProtoElementAnnotator> annotators;
  private final ProtoValidator validator;
  private static final List<String> JAVA_KEYWORDS = Arrays.asList("abstract",
      "continue", "for", "new",
      "switch",
      "assert", "default", "goto",
      "package", "synchronized",
      "boolean", "do", "if", "private",
      "this", "break", "double",
      "implements", "protected",
      "throw", "byte", "else",
      "import", "public", "throws",
      "case", "enum", "instanceof",
      "return", "transient", "catch",
      "extends", "int", "short",
      "try", "char", "final",
      "interface", "static", "void",
      "class", "finally", "long",
      "strictfp", "volatile", "const",
      "float", "native", "super",
      "while");

  public ProtoAnnotator() {
    super();
    validator = getProtoValidator();
    annotators = validator.getAnnotators();
  }

  private ProtoValidator getProtoValidator() {
    final ProtoValidatorExtn[] extensions = Extensions.getExtensions(ProtoValidatorExtn.EP_NAME);
    if (extensions.length == 0) {
      return new ProtoValidator();
    }
    return (ProtoValidator) extensions[0];
  }

  @SuppressWarnings("unchecked")
  private <E> void annotateElement(
      Class<E> clazz, PsiElement element, AnnotationHolder annotationHolder) {
    E e = clazz.cast(element);
    ((ProtoElementAnnotator<E>) annotators.get(clazz)).annotate(e, annotationHolder);
  }

  @Override
  public void annotate(@NotNull PsiElement psiElement,
                       AnnotationHolder annotationHolder) {
    if (psiElement instanceof ProtoSyntaxStatement) {
      annotateElement(ProtoSyntaxStatement.class, psiElement, annotationHolder);
    } else if (psiElement instanceof ProtoFileOptionStatement) {
      annotateElement(ProtoFileOptionStatement.class, psiElement, annotationHolder);
    }
    if (psiElement instanceof ProtoKeyword) {
      ProtoKeyword keyword = (ProtoKeyword) psiElement;

      highlightKeyword(keyword, annotationHolder);
    }
    if (psiElement instanceof ProtoEnumConstant) {
      ProtoEnumConstant constant = (ProtoEnumConstant) psiElement;
      annotateName(constant, annotationHolder,
                   ProtoTextAttributes.ATTR_ENUM_CONSTANT_NAME);
      checkName(constant.getNameElement(), annotationHolder, true);
    }
    if (psiElement instanceof ProtoEnumDefinition) {
      ProtoEnumDefinition enumDefinition = (ProtoEnumDefinition) psiElement;
      annotateName(enumDefinition, annotationHolder, ProtoTextAttributes.ATTR_ENUM_NAME);
      //TODO: Find out if enumDefinition names cannot be java keywords and handle it accordingly.
    }
    if (psiElement instanceof ProtoGroupDefinition) {
      ProtoGroupDefinition groupDefinition = (ProtoGroupDefinition) psiElement;
      validator.checkGroupNameCase(groupDefinition, annotationHolder);
      annotateName(groupDefinition, annotationHolder,
                   ProtoTextAttributes.ATTR_GROUP_NAME);
    }
    if (psiElement instanceof ProtoSimpleProperty) {
      ProtoSimpleProperty property = (ProtoSimpleProperty) psiElement;

      validator.checkDefaultValueType(property, annotationHolder);
      checkName(property.getNameElement(), annotationHolder, false);
    }
    if (psiElement instanceof ProtoMessageProperty) {
      ProtoMessageProperty messageProperty = (ProtoMessageProperty) psiElement;

      checkName(messageProperty.getNameElement(), annotationHolder, false);
    }
    if (psiElement instanceof ProtoMessageDefinition) {
      ProtoMessageDefinition messageDefinition = (ProtoMessageDefinition) psiElement;

      if (messageDefinition.getDefinitionBody() != null) {
        validator.checkPropertyIds(messageDefinition, annotationHolder);
      }
      PsiElement nameElement = messageDefinition.getNameElement();
      if (nameElement != null) {
        checkName(nameElement, annotationHolder, true,
                  messageDefinition.getName());
        Annotation anno = annotationHolder
            .createInfoAnnotation(nameElement, null);
        anno.setTextAttributes(ProtoTextAttributes.ATTR_MESSAGE_NAME);
      }
    }
    if (psiElement instanceof ProtoDefinitionBody) {
      ProtoDefinitionBody def = (ProtoDefinitionBody) psiElement;

      validator.checkProperties(def, annotationHolder);
      validator.checkEnums(def, annotationHolder);
    }
    if (psiElement instanceof ProtoFile) {
      ProtoFile protoFile = (ProtoFile) psiElement;

      validator.checkDuplicateNames(protoFile, annotationHolder);
      validator.checkUnusedMessages(protoFile, annotationHolder);
      validator.checkDuplicatePackageStatements(protoFile, annotationHolder);
    }
    if (psiElement instanceof ProtoMessageTypeReference) {
      ProtoMessageTypeReference ref = (ProtoMessageTypeReference) psiElement;

      validator.checkMessageTypeReference(ref, annotationHolder);
    }
    if (psiElement instanceof ProtoServiceDefinition) {
      ProtoServiceDefinition serviceDefinition = (ProtoServiceDefinition) psiElement;

      validator.checkServiceDefinition(serviceDefinition, annotationHolder);
      checkName(serviceDefinition.getNameElement(), annotationHolder, true);
      annotateName(serviceDefinition, annotationHolder,
          ProtoTextAttributes.ATTR_SERVICE_NAME);

    }
    if (psiElement instanceof ProtoRpcDefinition) {
      ProtoRpcDefinition rpcDefinition = (ProtoRpcDefinition) psiElement;

      validator.checkRpcDefinition(rpcDefinition, annotationHolder);
      annotateName(rpcDefinition, annotationHolder,
          ProtoTextAttributes.ATTR_RPC_NAME);
      checkName(rpcDefinition.getNameElement(), annotationHolder, false);
    }
    if (psiElement instanceof ProtoIntegerLiteral) {
      ProtoAbstractIntegerLiteral literal = (ProtoAbstractIntegerLiteral) psiElement;

      validator.checkIntegerLiteralValue(literal, annotationHolder);
    }
    if (psiElement instanceof ProtoProperty) {
      ProtoProperty property = (ProtoProperty) psiElement;

      validator.checkPropertyId(property, annotationHolder);
      validator.checkParentNameClash(property, annotationHolder);
      annotateName(property, annotationHolder,
                   ProtoTextAttributes.ATTR_PROPERTY_NAME);
    }
    if (psiElement instanceof ProtoPackageStatement) {
      ProtoPackageStatement packageStatement = (ProtoPackageStatement) psiElement;

      validator.checkPackageStatement(packageStatement, annotationHolder);
    }
    if (psiElement instanceof ProtoHexLiteral) {
      final ProtoHexLiteral hexLiteral = (ProtoHexLiteral) psiElement;

      validator.checkHexLiteral(hexLiteral, annotationHolder);
    }
    if (psiElement instanceof LeafPsiElement) {
      LeafPsiElement leaf = (LeafPsiElement) psiElement;

      validator.checkLeafs(leaf, annotationHolder);
    }
    // TODO: Add check for checking if extensions lower bound  is less than extensions
    // upper bound.
  }

  private void annotateName(ProtoNamedElement el,
                            AnnotationHolder annotationHolder, TextAttributesKey attr) {
    ProtoElement use = el.getNameElement();
    if (use == null) {
      use = el;
    }
    Annotation anno = annotationHolder.createInfoAnnotation(use, null);
    anno.setTextAttributes(attr);
  }

  private void checkName(ProtoElementWithName nameElement,
                         AnnotationHolder annotationHolder, boolean caseSensitive) {
    if (nameElement == null) {
      return;
    }
    checkName(nameElement, annotationHolder, caseSensitive, nameElement.getName());
  }

  private void checkName(PsiElement nameElement,
                         AnnotationHolder annotationHolder, boolean caseSensitive,
                         String name) {
    if (name != null) {
      String useName = caseSensitive ? name : name.toLowerCase();
      if (JAVA_KEYWORDS.contains(useName)) {
        annotationHolder.createErrorAnnotation(nameElement,
            "illegal use of Java keyword '" + useName
                + "' for identifier name");
      } else if (name.equals("TextLength")) {
        annotationHolder.createErrorAnnotation(nameElement,
            "illegal use of reserved name '" + name + "'");
      }
    }
  }

  private void highlightKeyword(ProtoKeyword keyword,
                                AnnotationHolder annotationHolder) {
    TextAttributesKey highlightInfo = ProtoSyntaxHighlighter.getAttributesForIdentifier(
        keyword.getKeywordText());
    if (highlightInfo != null) {
      Annotation anno = annotationHolder.createInfoAnnotation(keyword, null);
      anno.setTextAttributes(highlightInfo);
    }
  }
}
