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

package com.google.protoeditor.parsing;

import com.google.common.collect.ImmutableMap;
import com.google.protoeditor.lex.ProtoTextAttributes;

import com.intellij.openapi.editor.colors.TextAttributesKey;

import java.util.EnumSet;

public enum ProtoKeywords {

  SERVICE("service", ProtoTextAttributes.ATTR_DECLARATION),
  OPTION("option", ProtoTextAttributes.ATTR_DECLARATION),
  ENUM("enum", ProtoTextAttributes.ATTR_DECLARATION),
  RPC("rpc", ProtoTextAttributes.ATTR_DECLARATION),
  RETURNS("returns", ProtoTextAttributes.ATTR_DECLARATION),
  MESSAGE("message", ProtoTextAttributes.ATTR_DECLARATION),
  REQUIRED("required", ProtoTextAttributes.ATTR_MODIFIER),
  OPTIONAL("optional", ProtoTextAttributes.ATTR_MODIFIER),
  REPEATED("repeated", ProtoTextAttributes.ATTR_MODIFIER),
  DEFAULT("default", ProtoTextAttributes.ATTR_DECLARATION),
  GROUP("group", ProtoTextAttributes.ATTR_DECLARATION),
  BOOL("bool", ProtoTextAttributes.ATTR_TYPE),
  BOOLEAN("boolean", ProtoTextAttributes.ATTR_TYPE),
  INT32("int32", ProtoTextAttributes.ATTR_TYPE),
  INT64("int64", ProtoTextAttributes.ATTR_TYPE),
  UINT32("uint32", ProtoTextAttributes.ATTR_TYPE),
  UINT64("uint64", ProtoTextAttributes.ATTR_TYPE),
  FIXED32("fixed32", ProtoTextAttributes.ATTR_TYPE),
  FIXED64("fixed64", ProtoTextAttributes.ATTR_TYPE),
  SFIXED32("sfixed32", ProtoTextAttributes.ATTR_TYPE),
  SINT32("sint32", ProtoTextAttributes.ATTR_TYPE),
  SINT64("sint64", ProtoTextAttributes.ATTR_TYPE),
  SFIXED64("sfixed64", ProtoTextAttributes.ATTR_TYPE),
  FLOAT("float", ProtoTextAttributes.ATTR_TYPE),
  BYTES("bytes", ProtoTextAttributes.ATTR_TYPE),
  STRING("string", ProtoTextAttributes.ATTR_TYPE),
  DOUBLE("double", ProtoTextAttributes.ATTR_TYPE),
  TRUE("true", ProtoTextAttributes.ATTR_LITERAL),
  FALSE("false", ProtoTextAttributes.ATTR_LITERAL),
  PARSED("parsed", ProtoTextAttributes.ATTR_MODIFIER),
  PACKAGE("package", ProtoTextAttributes.ATTR_DECLARATION),
  CLASS("class", ProtoTextAttributes.ATTR_DECLARATION),
  SYNTAX("syntax", ProtoTextAttributes.ATTR_DECLARATION),
  IMPORT("import", ProtoTextAttributes.ATTR_DECLARATION),
  EXTEND("extend", ProtoTextAttributes.ATTR_DECLARATION),
  PYTHON("python", ProtoTextAttributes.ATTR_DECLARATION),
  CPLUSPLUSHEADER("c++header", ProtoTextAttributes.ATTR_DECLARATION),
  JAVA("java", ProtoTextAttributes.ATTR_DECLARATION),
  EXTENSIONS("extensions", ProtoTextAttributes.ATTR_DECLARATION),
  TO("to", ProtoTextAttributes.ATTR_MODIFIER),
  MAX("max", ProtoTextAttributes.ATTR_LITERAL),
  DEPRECATED("deprecated", ProtoTextAttributes.ATTR_DECLARATION),
  PACKED("packed", ProtoTextAttributes.ATTR_DECLARATION),
  CTYPE("ctype", ProtoTextAttributes.ATTR_DECLARATION),
  JTYPE("jtype", ProtoTextAttributes.ATTR_DECLARATION),
  WEAK("weak", ProtoTextAttributes.ATTR_DECLARATION),
  DPLOPTS("dplopts", ProtoTextAttributes.ATTR_DECLARATION),
  LAZY("lazy", ProtoTextAttributes.ATTR_DECLARATION);


  private static final EnumSet TYPE_KEYWORDS =
      EnumSet.of(BOOL, BOOLEAN, INT32, INT64, UINT32, UINT64,
          FIXED32, FIXED64, SFIXED32, SFIXED64, BYTES, STRING, FLOAT, DOUBLE, SINT32, SINT64);

  private static final ImmutableMap<String, ProtoKeywords> KEYWORD_BY_TEXT;

  static {
    ImmutableMap.Builder<String, ProtoKeywords> builder = ImmutableMap.builder();

    for (ProtoKeywords keyword : ProtoKeywords.values()) {
      builder.put(keyword.getText(), keyword);
    }

    KEYWORD_BY_TEXT = builder.build();
  }

  public static boolean isKeyword(String type) {
    return from(type) != null;
  }

  public static ProtoKeywords from(String s) {
    return KEYWORD_BY_TEXT.get(s);
  }

  public static boolean isTypeKeyword(String s) {
    ProtoKeywords keyword = from(s);

    return keyword != null && TYPE_KEYWORDS.contains(keyword);
  }

  private final String text;
  private final TextAttributesKey textAttributesKey;

  ProtoKeywords(String text, TextAttributesKey textAttributesKey) {
    this.text = text;
    this.textAttributesKey = textAttributesKey;
  }

  public String getText() {
    return text;
  }

  public TextAttributesKey getTextAttributesKey() {
    return textAttributesKey;
  }

  public boolean match(String s) {
    return text.equals(s);
  }
}
