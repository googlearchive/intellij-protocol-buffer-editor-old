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

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.JavaHighlightInfoTypes;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;

import java.awt.*;

public class ProtoTextAttributes {

  public static TextAttributesKey ATTR_MODIFIER;
  public static TextAttributesKey ATTR_DECLARATION;
  public static TextAttributesKey ATTR_TYPE;
  public static TextAttributesKey ATTR_LITERAL;
  public static TextAttributesKey ATTR_STRING;
  public static TextAttributesKey ATTR_NUMBER;
  public static TextAttributesKey ATTR_BLOCK_COMMENT;
  public static TextAttributesKey ATTR_LINE_COMMENT;
  public static TextAttributesKey ATTR_BRACES;
  public static TextAttributesKey ATTR_PARENS;
  public static TextAttributesKey ATTR_BRACKETS;
  public static TextAttributesKey ATTR_OPERATION_SIGN;

  public static TextAttributesKey ATTR_PROPERTY_NAME;
  public static TextAttributesKey ATTR_RPC_NAME;
  public static TextAttributesKey ATTR_SERVICE_NAME;
  public static TextAttributesKey ATTR_ENUM_NAME;
  public static TextAttributesKey ATTR_MESSAGE_NAME;
  public static TextAttributesKey ATTR_GROUP_NAME;
  public static TextAttributesKey ATTR_ENUM_CONSTANT_NAME;

  public static TextAttributesKey ATTR_BAD_CHARACTER;

  public static TextAttributesKey ATTR_JAVA_LITERAL;

  static {
    TextAttributes attr = new TextAttributes();
    attr.setBackgroundColor(Color.LIGHT_GRAY);
    attr.setFontType(Font.ITALIC);
    ATTR_JAVA_LITERAL = TextAttributesKey
        .createTextAttributesKey("PROTO.LANGUAGE_LITERAL",
            attr);
  }

  public static final TextAttributesKey ATTR_CORE_ID;

  static {
    TextAttributes attr = new TextAttributes();
    attr.setEffectType(EffectType.LINE_UNDERSCORE);
    attr.setEffectColor(Color.BLACK);
    ATTR_CORE_ID = TextAttributesKey
        .createTextAttributesKey("PROTO.CORE_ID", attr);
  }

  private static TextAttributesKey copyTextAttributesKey(
      String type, TextAttributesKey key, EditorColorsScheme scheme) {
    TextAttributes attributes = getAttributes(key, scheme);
    if (attributes == null) {
      return TextAttributesKey.createTextAttributesKey(type);
    }
    return TextAttributesKey.createTextAttributesKey(type, attributes);
  }

  private static TextAttributesKey createTextAttributesKey(
      String type, HighlightInfoType info, EditorColorsScheme scheme) {
    return copyTextAttributesKey(type, info.getAttributesKey(), scheme);
  }

  public static final TextAttributes getAttributes(TextAttributesKey key,
      EditorColorsScheme scheme) {
    return scheme.getAttributes(key);
  }

  public static void setColorsFrom(EditorColorsScheme scheme) {

    ATTR_MODIFIER = createTextAttributesKey(
        "PROTO.MODIFIER", JavaHighlightInfoTypes.JAVA_KEYWORD, scheme);
    ATTR_DECLARATION = createTextAttributesKey(
        "PROTO.KEYWORD", JavaHighlightInfoTypes.JAVA_KEYWORD, scheme);
    ATTR_TYPE = createTextAttributesKey(
        "PROTO.TYPE", JavaHighlightInfoTypes.JAVA_KEYWORD, scheme);
    ATTR_LITERAL = createTextAttributesKey(
        "PROTO.LITERAL", JavaHighlightInfoTypes.JAVA_KEYWORD, scheme);
    ATTR_STRING = copyTextAttributesKey(
        "PROTO.STRING", SyntaxHighlighterColors.STRING, scheme);
    ATTR_NUMBER = copyTextAttributesKey(
        "PROTO.NUMBER", SyntaxHighlighterColors.NUMBER, scheme);
    ATTR_BLOCK_COMMENT = copyTextAttributesKey(
        "PROTO.BLOCK_COMMENT", SyntaxHighlighterColors.JAVA_BLOCK_COMMENT, scheme);
    ATTR_LINE_COMMENT = copyTextAttributesKey(
        "PROTO.LINE_COMMENT", SyntaxHighlighterColors.LINE_COMMENT, scheme);
    ATTR_BRACES = copyTextAttributesKey(
        "PROTO.BRACES", SyntaxHighlighterColors.BRACES, scheme);
    ATTR_PARENS = copyTextAttributesKey(
        "PROTO.PARENS", SyntaxHighlighterColors.PARENTHS, scheme);
    ATTR_BRACKETS = copyTextAttributesKey(
        "PROTO.BRACKETS", SyntaxHighlighterColors.BRACKETS, scheme);
    ATTR_OPERATION_SIGN = copyTextAttributesKey(
        "PROTO.OPERATION_SIGN", SyntaxHighlighterColors.OPERATION_SIGN, scheme);

    ATTR_PROPERTY_NAME = TextAttributesKey.createTextAttributesKey("PROTO.PROPERTY_NAME",
        getAttributes(CodeInsightColors.INSTANCE_FIELD_ATTRIBUTES, scheme));
    ATTR_RPC_NAME = TextAttributesKey.createTextAttributesKey("PROTO.RPC_NAME",
        getAttributes(CodeInsightColors.METHOD_DECLARATION_ATTRIBUTES, scheme));
    ATTR_SERVICE_NAME = TextAttributesKey.createTextAttributesKey("PROTO.SERVICE_NAME",
        getAttributes(CodeInsightColors.CLASS_NAME_ATTRIBUTES, scheme));
    ATTR_ENUM_NAME = TextAttributesKey.createTextAttributesKey("PROTO.ENUM_NAME",
        getAttributes(CodeInsightColors.CLASS_NAME_ATTRIBUTES, scheme));
    ATTR_MESSAGE_NAME = TextAttributesKey.createTextAttributesKey("PROTO.MESSAGE_NAME",
        getAttributes(CodeInsightColors.CLASS_NAME_ATTRIBUTES, scheme));
    ATTR_GROUP_NAME = TextAttributesKey.createTextAttributesKey("PROTO.GROUP_NAME",
        getAttributes(CodeInsightColors.CLASS_NAME_ATTRIBUTES, scheme));
    ATTR_ENUM_CONSTANT_NAME = TextAttributesKey.createTextAttributesKey("PROTO.ENUM_CONSTANT_NAME",
        getAttributes(CodeInsightColors.STATIC_FIELD_ATTRIBUTES, scheme));

    ATTR_BAD_CHARACTER = TextAttributesKey.createTextAttributesKey("PROTO.BAD_CHARACTER",
        getAttributes(HighlighterColors.BAD_CHARACTER, scheme));
  }

  public static final EditorColorsListener EDITOR_COLORS_LISTENER = new EditorColorsListener() {
    @Override
    public void globalSchemeChange(EditorColorsScheme editorColorsScheme) {
      setColorsFrom(editorColorsScheme);
    }
  };

  public static void initialize() {
    EditorColorsManager manager = EditorColorsManager.getInstance();
    setColorsFrom(manager.getGlobalScheme());
    manager.addEditorColorsListener(EDITOR_COLORS_LISTENER);
  }
}
