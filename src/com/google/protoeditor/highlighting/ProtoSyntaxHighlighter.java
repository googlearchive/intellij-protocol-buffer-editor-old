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

package com.google.protoeditor.highlighting;

import com.google.protoeditor.lex.ProtoLexer;
import com.google.protoeditor.lex.ProtoTextAttributes;
import com.google.protoeditor.lex.ProtoTokenTypes;
import com.google.protoeditor.parsing.ProtoKeywords;

import com.intellij.ide.highlighter.JavaFileHighlighter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.module.LanguageLevelUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ProtoSyntaxHighlighter extends SyntaxHighlighterBase {

  public static class ProtoSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
      return new ProtoSyntaxHighlighter(virtualFile != null ?
          LanguageLevelUtil.getLanguageLevelForFile(virtualFile) : LanguageLevel.HIGHEST);
    }
  }

  private Map<IElementType, TextAttributesKey> attributes
      = new HashMap<IElementType, TextAttributesKey>();

  public static TextAttributesKey getAttributesForIdentifier(String tokenText) {
    ProtoKeywords protoKeyword = ProtoKeywords.from(tokenText);
    return protoKeyword != null ? protoKeyword.getTextAttributesKey() : null;
  }

  private JavaFileHighlighter javaHighlighter;

  public ProtoSyntaxHighlighter(LanguageLevel level) {
    javaHighlighter = new JavaFileHighlighter(level);
    initialize();
  }

  private void initialize() {
    ProtoTextAttributes.initialize();

    attributes.put(ProtoTokenTypes.C_STYLE_COMMENT, ProtoTextAttributes.ATTR_BLOCK_COMMENT);
    attributes.put(ProtoTokenTypes.END_OF_LINE_COMMENT, ProtoTextAttributes.ATTR_LINE_COMMENT);

    attributes.put(ProtoTokenTypes.FLOAT_LITERAL, ProtoTextAttributes.ATTR_NUMBER);
    attributes.put(ProtoTokenTypes.INTEGER_LITERAL, ProtoTextAttributes.ATTR_NUMBER);
    attributes.put(ProtoTokenTypes.STRING_LITERAL, ProtoTextAttributes.ATTR_STRING);

    attributes.put(ProtoTokenTypes.LBRACE, ProtoTextAttributes.ATTR_BRACES);
    attributes.put(ProtoTokenTypes.RBRACE, ProtoTextAttributes.ATTR_BRACES);
    attributes.put(ProtoTokenTypes.RBRACKET, ProtoTextAttributes.ATTR_BRACKETS);
    attributes.put(ProtoTokenTypes.RBRACKET, ProtoTextAttributes.ATTR_BRACKETS);
    attributes.put(ProtoTokenTypes.LT, ProtoTextAttributes.ATTR_BRACKETS);
    attributes.put(ProtoTokenTypes.GT, ProtoTextAttributes.ATTR_BRACKETS);

    attributes.put(ProtoTokenTypes.EQ, ProtoTextAttributes.ATTR_OPERATION_SIGN);

    attributes.put(ProtoTokenTypes.LPAR, ProtoTextAttributes.ATTR_PARENS);
    attributes.put(ProtoTokenTypes.RPAR, ProtoTextAttributes.ATTR_PARENS);

    attributes.put(ProtoTokenTypes.BAD_CHARACTER, ProtoTextAttributes.ATTR_BAD_CHARACTER);

    attributes.put(ProtoTokenTypes.LANGUAGE_LITERAL, ProtoTextAttributes.ATTR_JAVA_LITERAL);
  }

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return new ProtoLexer();
  }

  @NotNull
  @Override
  public TextAttributesKey[] getTokenHighlights(IElementType iElementType) {
    TextAttributesKey key = attributes.get(iElementType);
    if (key == null) {
      TextAttributesKey[] highlights = javaHighlighter
          .getTokenHighlights(iElementType);
      if (highlights.length > 0) {
        return highlights;
      }
      return new TextAttributesKey[0];
    } else {
      return new TextAttributesKey[]{key};
    }
  }
}
