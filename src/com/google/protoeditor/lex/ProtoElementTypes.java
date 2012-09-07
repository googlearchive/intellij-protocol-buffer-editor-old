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

import com.google.protoeditor.ProtoFileType;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;

public final class ProtoElementTypes {

  public static final IElementType NAME = new ProtoElementType("NAME");
  public static final IElementType NUMERIC_ID = new ProtoElementType("NUMERIC_ID");
  public static final IElementType GROUP_DEFINITION = new ProtoElementType("GROUP_DEFINITION");
  public static final IElementType PROPERTY_MODIFIER = new ProtoElementType("PROPERTY_MODIFIER");
  public static final IElementType MESSAGE_TYPE_REFERENCE =
      new ProtoElementType("MESSAGE_TYPE_REFERENCE");
  public static final IElementType MESSAGE_PROPERTY = new ProtoElementType("MESSAGE_PROPERTY");
  public static final IElementType SIMPLE_PROPERTY = new ProtoElementType("SIMPLE_PROPERTY");
  public static final IElementType ENUM_PROPERTY = new ProtoElementType("ENUM_PROPERTY");
  public static final IElementType PROPERTY_TYPE = new ProtoElementType("PROPERTY_TYPE");
  public static final IElementType ENUM_PROPERTY_TYPE = new ProtoElementType("ENUM_PROPERTY_TYPE");
  public static final IElementType OPTION = new ProtoElementType("OPTION");
  public static final IElementType OPTION_VALUE = new ProtoElementType("OPTION_VALUE");
  public static final IElementType ENUM_DEFINITION = new ProtoElementType("ENUM_DEFINITION");
  public static final IElementType ENUM_CONSTANT = new ProtoElementType("ENUM_CONSTANT");
  public static final IFileElementType FILE =
      new IFileElementType("FILE", ProtoFileType.instance().getLanguage());
  public static final IElementType MESSAGE_DEFINITION = new ProtoElementType("MESSAGE_DEFINITION");
  public static final IElementType EXTEND_DEFINITION = new ProtoElementType("EXTEND_DEFINITION");
  public static final IElementType DEFAULT_VALUE = new ProtoElementType("DEFAULT_VALUE");
  public static final IElementType INTEGER_LITERAL = new ProtoElementType("INTEGER_LITERAL");
  public static final IElementType FLOAT_LITERAL = new ProtoElementType("FLOAT_LITERAL");
  public static final IElementType STRING_LITERAL = new ProtoElementType("STRING_LITERAL");
  public static final IElementType ENUM_VALUE = new ProtoElementType("ENUM_VALUE");
  public static final IElementType DEFINITION_BODY = new ProtoElementType("DEFINITION_BODY");
  public static final IElementType SERVICE_DEFINITION = new ProtoElementType("SERVICE_DEFINITION");
  public static final IElementType RPC_INPUT_TYPE = new ProtoElementType("RPC_INPUT_TYPE");
  public static final IElementType RPC_RETURN_TYPE = new ProtoElementType("RPC_INPUT_TYPE");
  public static final IElementType RPC_DEFINITION = new ProtoElementType("RPC_DEFINITION");
  public static final IElementType KEYWORD = new ProtoElementType("KEYWORD");
  public static final IElementType BOOLEAN_LITERAL = new ProtoElementType("BOOLEAN_LITERAL");
  public static final IElementType HEX_LITERAL = new ProtoElementType("HEX_LITERAL");
  public static final IElementType PACKAGE_STATEMENT = new ProtoElementType("PACKAGE_STATEMENT");
  public static final IElementType LANGUAGE_LITERAL = new ProtoElementType("LANGUAGE_LITERAL");
  public static final IElementType LANGUAGE_CONTENT = new ProtoElementType("LANGUAGE_CONTENT");
  public static final IElementType ENUM_BODY = new ProtoElementType("ENUM_BODY");
  public static final IElementType RPC_BODY = new ProtoElementType("RPC_BODY");
  public static final IElementType SERVICE_BODY = new ProtoElementType("SERVICE_BODY");
  public static final IElementType PACKAGE_NAME = new ProtoElementType("PACKAGE_NAME");
  public static final IElementType SYNTAX_STATEMENT = new ProtoElementType("SYNTAX_STATEMENT");
  public static final IElementType SYNTAX_VALUE = new ProtoElementType("SYNTAX_VALUE");
  public static final IElementType IMPORT_STATEMENT = new ProtoElementType("IMPORT_STATEMENT");
  public static final IElementType IMPORT_VALUE = new ProtoElementType("IMPORT_VALUE");
  public static final IElementType FILE_OPTION_STATEMENT =
      new ProtoElementType("FILE_OPTION_STATEMENT");
  public static final IElementType MESSAGE_OPTION = new ProtoElementType("MESSAGE_OPTION");
  public static final IElementType USER_DEFINED_PROPERTY_TYPE =
      new ProtoElementType("USER_DEFINED_PROPERTY_TYPE");
  public static final IElementType USER_DEFINED_PROPERTY =
      new ProtoElementType("USER_DEFINED_PROPERTY");
  public static final IElementType EXTENSIONS_STATEMENT =
      new ProtoElementType("EXTENSIONS_STATEMENT");
  public static final IElementType EXTENSIONS_LOWER_BOUND =
      new ProtoElementType("EXTENTIONS_LOWER_BOUND");
  public static final IElementType EXTENSIONS_UPPER_BOUND =
      new ProtoElementType("EXTENTIONS_UPPER_BOUND");
  public static final IElementType CUSTOM_OPTION_VALUE =
      new ProtoElementType("CUSTOM_OPTION_VALUE");
  public static final IElementType CUSTOM_OPTION_NAME =
      new ProtoElementType("CUSTOM_OPTION_NAME");

  private ProtoElementTypes() {
  }
}
