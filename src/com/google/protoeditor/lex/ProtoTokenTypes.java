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

package com.google.protoeditor.lex;

import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;

public final class ProtoTokenTypes {

  public static final IElementType LT = new ProtoElementType("LT");
  public static final IElementType STRING_LITERAL = new ProtoElementType("STRING_LITERAL");
  public static final IElementType EQ = new ProtoElementType("EQ");
  public static final IElementType IDENTIFIER = new ProtoElementType("IDENTIFIER");
  public static final IElementType END_OF_LINE_COMMENT =
      new ProtoElementType("END_OF_LINE_COMMENT");
  public static final IElementType SEMICOLON = new ProtoElementType("SEMICOLON");
  public static final IElementType LBRACKET = new ProtoElementType("LBRACKET");
  public static final IElementType RPAR = new ProtoElementType("RPAR");
  public static final IElementType LBRACE = new ProtoElementType("LBRACE");
  public static final IElementType RBRACE = new ProtoElementType("RBRACE");
  public static final IElementType INTEGER_LITERAL = new ProtoElementType("INTEGER_LITERAL");
  public static final IElementType FLOAT_LITERAL = new ProtoElementType("FLOAT_LITERAL");
  public static final IElementType GT = new ProtoElementType("GT");
  public static final IElementType LPAR = new ProtoElementType("LPAR");
  public static final IElementType WHITE_SPACE = new ProtoElementType("WHITE_SPACE");
  public static final IElementType RBRACKET = new ProtoElementType("RBRACKET");
  public static final IElementType C_STYLE_COMMENT = new ProtoElementType("C_STYLE_COMMENT");
  public static final IElementType BAD_CHARACTER = new ProtoElementType("BAD_CHARACTER");
  public static final IElementType LANGUAGE_LITERAL = new ProtoElementType("LANGUAGE_LITERAL");
  public static final IElementType HEX_LITERAL = new ProtoElementType("HEX_LITERAL");
  public static final IElementType LANGUAGE_LITERAL_CONTENT =
      new ProtoElementType("LANGUAGE_LITERAL_CONTENT");
  public static final IElementType JAVA_LITERAL = JavaElementType.IMPORT_LIST;
  public static final IElementType PACKAGE_STATEMENT = new ProtoElementType("PACKAGE_STATEMENT");
  public static final IElementType COMMA = new ProtoElementType("COMMA");
  public static final IElementType EOF = new ProtoElementType("EOF");

  private ProtoTokenTypes() {
  }


}
