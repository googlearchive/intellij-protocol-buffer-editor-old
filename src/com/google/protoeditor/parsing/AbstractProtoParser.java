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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractProtoParser {

  protected static final ImmutableList<IElementType> VALID_TOKENS_TO_BODY = ImmutableList.of(
    ProtoTokenTypes.LBRACE, ProtoTokenTypes.RBRACE, ProtoTokenTypes.SEMICOLON);

  /**
   * Returns the fully qualified name for the given identifier.
   *
   * @param outerName Name of the enclosing namespace, like message name.
   * @param name Name of the inner identifier.
   * @return  Fully qualified name for the identifier.
   */
  protected static String toFQName(String outerName, String name) {
    return Strings.isNullOrEmpty(outerName) ? name : outerName + "." + name;
  }

  String parseNextTokenAsKeyword(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();

    String keyword = builder.getTokenText();
    if (ProtoKeywords.isKeyword(keyword)) {
      builder.advanceLexer();
      marker.done(ProtoElementTypes.KEYWORD);
      return keyword;
    }
    marker.error("Expected Keyword");
    return null;
  }

  protected void parseOptionalSemicolon(PsiBuilder builder) {
    if (builder.getTokenType() == ProtoTokenTypes.SEMICOLON) {
      builder.advanceLexer();
    }
  }

  protected boolean parseOptional(PsiBuilder builder, IElementType token) {
    if (builder.getTokenType() == token) {
      builder.advanceLexer();
      return true;
    }
    return false;
  }

  protected boolean parseKeyword(PsiBuilder builder, ProtoKeywords keyword) {
    if (!isNextTokenKeyword(builder, keyword)) {
      builder.error("expected '" + keyword + "'");
      return false;
    } else {
      parseNextTokenAsKeyword(builder);
    }
    return true;
  }

  /**
   * Advances the lexer if the current token matches the expected token.
   *
   * @param builder PsiBuilder which points to the current token being parsed.
   * @param token The type of token that is expected.
   * @param expected The text representation of the expected token.
   * @return Returns true if expected token found else false.
   */
  protected boolean parseExpected(PsiBuilder builder, IElementType token, String expected) {
    if (builder.getTokenType() != token) {
      builder.error("expected '" + expected + "', but got '" + builder.getTokenText() + "'");
      return false;
    }
    builder.advanceLexer();
    return true;
  }

  void eatUntil(PsiBuilder builder, IElementType... types) {
    List<IElementType> list = Arrays.asList(types);
    while (!builder.eof() && !list.contains(builder.getTokenType())) {
      builder.advanceLexer();
    }
  }

  protected boolean parseUpToSemicolon(PsiBuilder builder, boolean necessary) {
    if (builder.getTokenType() != ProtoTokenTypes.SEMICOLON) {
      if (necessary) {
        builder.error("expected ';'");
        return false;
      }
      eatUntil(builder, ProtoTokenTypes.SEMICOLON, ProtoTokenTypes.RBRACE);
      if (!builder.eof() && builder.getTokenType() == ProtoTokenTypes.SEMICOLON) {
        builder.advanceLexer();
      }
    } else {
      builder.advanceLexer();
    }
    return true;
  }

  public void parseUpTo(PsiBuilder builder, ImmutableList<IElementType> expectedTokens)
  {
    while (!builder.eof() && !expectedTokens.contains(builder.getTokenType())) {
      builder.advanceLexer();
    }
  }

  boolean isLiteral(IElementType expectedLiteral, String expectedText) {
    return expectedLiteral == ProtoTokenTypes.FLOAT_LITERAL
           || expectedLiteral == ProtoTokenTypes.INTEGER_LITERAL
           || expectedLiteral == ProtoTokenTypes.HEX_LITERAL
           || expectedLiteral == ProtoTokenTypes.STRING_LITERAL
           || (expectedLiteral == ProtoTokenTypes.IDENTIFIER
               && (expectedText.equals("true") || expectedText.equals("false")));
  }

  void parseName(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    builder.advanceLexer();
    marker.done(ProtoElementTypes.NAME);
  }

  void parseLiteral(
      PsiBuilder builder, IElementType expectedLiteral, String tokenText) {
    PsiBuilder.Marker marker = builder.mark();
    if (isKeyword(expectedLiteral, tokenText, ProtoKeywords.TRUE)
        || isKeyword(expectedLiteral, tokenText, ProtoKeywords.FALSE)) {
      parseNextTokenAsKeyword(builder);
      marker.done(ProtoElementTypes.BOOLEAN_LITERAL);
    } else {
      builder.advanceLexer();
      if (expectedLiteral == ProtoTokenTypes.INTEGER_LITERAL) {
        marker.done(ProtoElementTypes.INTEGER_LITERAL);
      } else if (expectedLiteral == ProtoTokenTypes.HEX_LITERAL) {
        marker.done(ProtoElementTypes.HEX_LITERAL);
      } else if (expectedLiteral == ProtoTokenTypes.FLOAT_LITERAL) {
        marker.done(ProtoElementTypes.FLOAT_LITERAL);
      } else if (expectedLiteral == ProtoTokenTypes.STRING_LITERAL) {
        marker.done(ProtoElementTypes.STRING_LITERAL);
      } else {
        throw new IllegalArgumentException("Unknown literal " + expectedLiteral);
      }
    }
  }

  boolean isNextTokenKeyword(PsiBuilder builder, ProtoKeywords keyword) {
    return isKeyword(builder.getTokenType(), builder.getTokenText(), keyword);
  }

  boolean isKeyword(IElementType token, String tokenText, ProtoKeywords keyword) {
    return token == ProtoTokenTypes.IDENTIFIER && keyword.match(tokenText);
  }

  protected boolean parseTokenAsElement(PsiBuilder builder, IElementType tokenType,
      IElementType elementType, String expectedText) {
    if (builder.getTokenType() == tokenType) {
      PsiBuilder.Marker nameMark = builder.mark();
      builder.advanceLexer();
      nameMark.done(elementType);
      return true;
    } else {
      builder.error(expectedText);
      return false;
    }
  }

}
