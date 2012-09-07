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

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.Nullable;

/**
 * ProtoElement that encapsulates the value of a custom field option. For instance,
 * the string "(my_field_option) = 4.5" in the following example :-
 *
 * optional int32 foo = 1 [(my_field_option) = 4.5];
 */
public class ProtoCustomOptionValue extends AbstractProtoElement {

  public ProtoCustomOptionValue(ASTNode astNode) {
    super(astNode);
  }

  @Nullable public ProtoLiteral getValueElement() {
    return PsiTreeUtil.getChildOfType(this, ProtoLiteral.class);
  }

  @Nullable public ProtoNameElement getEnumNameElement() {
    return PsiTreeUtil.getChildOfType(this, ProtoNameElement.class);
  }
}
