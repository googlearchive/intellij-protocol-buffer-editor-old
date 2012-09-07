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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtoHexLiteral extends ProtoAbstractIntegerLiteral {

  private static final Pattern HEX_PREFIX = Pattern.compile("^0[xX]");

  public ProtoHexLiteral(com.intellij.lang.ASTNode astNode) {
    super(astNode);
  }

  protected String createStringValue(long val) {
    return "0x" + Long.toString(val, 16);
  }

  public boolean hasValidValue() {
    String hexStr = getText();
    int index = indexAfterPrefix(hexStr);
    if (index == -1) {
      return false;
    }
    hexStr = removeHexPrefix(hexStr, index);
    try {
      Long.parseLong(hexStr, 16);
      return true;
    } catch (NumberFormatException e1) {
      return false;
    }
  }

  public long getIntValue() throws IllegalStateException {
    String hexStr = getText();
    int index = indexAfterPrefix(hexStr);
    if (index == -1) {
      throw new IllegalStateException("literal does not have valid prefix");  
    }
    hexStr = removeHexPrefix(hexStr, index);

    long val;
    try {
      val = Long.parseLong(hexStr, 16);
    } catch (NumberFormatException e1) {
      throw new IllegalStateException("literal does not have valid value");
    }
    return val;
  }

  private String removeHexPrefix(String hexStr, int index) {
    return hexStr.substring(index);
  }

  private int indexAfterPrefix(String hexStr) {
    Matcher m = HEX_PREFIX.matcher(hexStr);
    if (!m.find()) {
      return -1;
    }
    return m.group().length();
  }
}
