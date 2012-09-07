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

import com.google.protoeditor.psi.ProtoSyntaxStatement;
import com.google.protoeditor.psi.ProtoSyntaxValue;

import com.intellij.lang.annotation.AnnotationHolder;

public class SyntaxStatementAnnotator implements ProtoElementAnnotator<ProtoSyntaxStatement> {

  @Override
  public void annotate(ProtoSyntaxStatement statement, AnnotationHolder annotationHolder) {
    ProtoSyntaxValue syntaxValue = statement.getProtoSyntaxValue();
    if (syntaxValue == null || !"\"proto2\"".equals(syntaxValue.getText())) {
      annotationHolder.createErrorAnnotation(
          statement,
          "illegal syntax value, expected \"proto2\"");
    }
  }
}

