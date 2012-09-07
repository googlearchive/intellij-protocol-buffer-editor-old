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

public class ProtoIntegerLiteral extends ProtoAbstractIntegerLiteral {

  public ProtoIntegerLiteral(ASTNode astNode) {
    super(astNode);
  }

  protected String createStringValue(long val) {
    return Long.toString(val);
  }

  public boolean hasValidValue() {
    try {
      Long.parseLong(getText());
      return true;
    } catch (NumberFormatException e1) {
      return false;
    }
  }

  public long getIntValue() throws IllegalStateException {
    long val;
    try {
      val = Long.parseLong(getText());
    } catch (NumberFormatException e1) {
      throw new IllegalStateException("literal does not have valid value");
    }
    return val;
  }
}
