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

import com.google.common.collect.ImmutableSet;
import com.google.protoeditor.psi.ProtoBooleanLiteral;
import com.google.protoeditor.psi.ProtoFileOptionStatement;
import com.google.protoeditor.psi.ProtoLiteral;
import com.google.protoeditor.psi.ProtoNameElement;
import com.google.protoeditor.psi.ProtoOptionValue;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;

import java.util.Set;

public class FileOptionStatementAnnotator
    implements ProtoElementAnnotator<ProtoFileOptionStatement> {

  private final Set<String> recognizedOptions;

  private final Set<String> booleanOptions;

  public FileOptionStatementAnnotator() {
    recognizedOptions = ImmutableSet
        .of("java_multiple_files", "java_outer_classname", "optimize_for",
            "java_package", "java_generate_and_equals_hash", "cc_generic_services",
            "java_generic_services", "py_generic_services");

    booleanOptions = ImmutableSet
        .of("java_multiple_files", "java_generate_and_equals_hash", "cc_generic_services",
            "java_generic_services", "py_generic_services");
  }

  public FileOptionStatementAnnotator(Set<String> recognizedOptions,
      Set<String> booleanOptions) {
    this.recognizedOptions = recognizedOptions;
    this.booleanOptions = booleanOptions;
  }

  @Override
  public void annotate(ProtoFileOptionStatement statement, AnnotationHolder annotationHolder) {
    checkOptionName(statement.getNameElement(), annotationHolder);
    checkOptionValue(statement, annotationHolder);
  }

  protected void checkOptionValue(
      ProtoFileOptionStatement statement, AnnotationHolder annotationHolder) {
    String name = statement.getName();
    if (name == null) {
      return;
    }

    if (booleanOptions.contains(name)) {
      checkValueIsBoolean(statement.getName(), statement.getOptionValue(), annotationHolder);
    }
  }

  protected void highlightAsUnknownSymbol(Annotation anno) {
    anno.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
  }

  private void checkValueIsBoolean(
      String optionName,
      ProtoOptionValue optionValue,
      AnnotationHolder annotationHolder) {
    if (optionValue == null) {
      return;
    }
    
    boolean bad = false;
    ProtoLiteral literalValue = optionValue.getLiteralValue();
    if (literalValue == null) {
      String idValue = optionValue.getIdentifierValue();
      if (idValue != null) {
        bad = true;
      }
    } else {
      if (!(literalValue instanceof ProtoBooleanLiteral)) {
        bad = true;
      }
    }
    if (bad) {
      Annotation anno = annotationHolder.createErrorAnnotation(
          optionValue,
          "value of option '" + optionName + "' must be true or false");

      highlightAsUnknownSymbol(anno);
    }
  }

  private void checkOptionName(ProtoNameElement nameElement, AnnotationHolder annotationHolder) {
    if (nameElement == null) {
      return;
    }

    String name = nameElement.getName();
    if (!recognizedOptions.contains(name)) {
      annotationHolder.createWarningAnnotation(
          nameElement,
          "unrecognized option name " + name);
    }
  }
}
