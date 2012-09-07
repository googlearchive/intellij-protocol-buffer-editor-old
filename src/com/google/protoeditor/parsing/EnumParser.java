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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;

import java.util.Set;

/**
 * Parses enum definition. Enums can be defined inside a message body or can be defined
 * independently of message types. For ex :-
 *
 * message SearchRequest {
 *   enum Corpus {
 *     UNIVERSAL = 0;
 *     WEB = 1;
 *     IMAGES = 2;
 *   }
 *   optional Corpus corpus = 4 [default = UNIVERSAL];
 *  }
 *
 * or
 *
 * enum Corpus {
 *     UNIVERSAL = 0;
 *     WEB = 1;
 *     IMAGES = 2;
 * }
 *
 * For details, see https://developers.google.com/protocol-buffers/docs/proto#enum
 */
public class EnumParser extends AbstractProtoParser {

  private final String messageNamespace;

  public EnumParser() {
    this("");
  }

  public EnumParser(String messageNamespace) {
    this.messageNamespace = messageNamespace;
  }

  /**
   * Parses enum definition.
   *
   * @param builder PsiBuilder for building PSI tree.
   * @return Enum definition encapsulated in a {@link Pair}. The first element in the pair
   *         is the enum name and second element is a set of names of enum constants.
   *
   * enum ::= "enum" identifier "{" ( option | enumField | ";" )* "}"
   */
  public Pair<String, Set<String>> parseEnum(PsiBuilder builder) {
    PsiBuilder.Marker enumMark = builder.mark();
    parseKeyword(builder, ProtoKeywords.ENUM);
    Set<String> enumConstants = Sets.newHashSet();
    String enumName = null;

    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected enum name");
      parseUpTo(builder, VALID_TOKENS_TO_BODY);
    } else {
      enumName = toFQName(messageNamespace, builder.getTokenText());
      parseName(builder);
    }

    parseExpected(builder, ProtoTokenTypes.LBRACE, "{");

    PsiBuilder.Marker enumBodyMark = builder.mark();
    while (true) {
      if (builder.eof()) {
        builder.error("expected '}'");
        break;
      } else if (builder.getTokenType() == ProtoTokenTypes.RBRACE) {
        builder.advanceLexer();
        parseOptional(builder, ProtoTokenTypes.SEMICOLON);
        break;
      } else if (isNextTokenKeyword(builder, ProtoKeywords.OPTION)) {
        if (!(new ProtoOptionParser().parseOption(builder, ProtoElementTypes.OPTION))) {
          break;
        }
      }
      else {
        String enumConstantName = parseEnumConstant(builder);
        if (enumConstantName == null) {
          break;
        }
        enumConstants.add(enumConstantName);
      }
    }
    enumBodyMark.done(ProtoElementTypes.ENUM_BODY);
    enumMark.done(ProtoElementTypes.ENUM_DEFINITION);

    if (enumName == null) {
      return null;
    }
    return new Pair<String, Set<String>>(enumName, enumConstants);
  }

  /**
   * @param builder
   * @return
   *
   * enumField ::= identifier "=" constant ";"
   */
  private String parseEnumConstant(PsiBuilder builder) {
    String enumConstantName = null;
    PsiBuilder.Marker constMark = builder.mark();

    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected enum constant name");
      constMark.drop();
      return null;
    } else {
      enumConstantName = builder.getTokenText();
      parseName(builder);
    }
    if (!parseExpected(builder, ProtoTokenTypes.EQ, "=")) {
      builder.error("Expected '='");
    }

    IElementType expectedLiteral = builder.getTokenType();
    String expectedLiteralText = builder.getTokenText();
    if (expectedLiteral != ProtoTokenTypes.INTEGER_LITERAL
        && expectedLiteral != ProtoTokenTypes.HEX_LITERAL) {
      builder.error("expected integer or hex constant");
      constMark.drop();
      return null;
    } else {
      PsiBuilder.Marker idMarker = builder.mark();
      parseLiteral(builder, expectedLiteral, expectedLiteralText);
      idMarker.done(ProtoElementTypes.ENUM_VALUE);
    }

    if (builder.getTokenType() == ProtoTokenTypes.LBRACKET) {

      ProtoOptionParser optionParser = new ProtoOptionParser();
      optionParser.parseFieldOptions(builder, "", null,
          Lists.newArrayList(optionParser.getAllOptions()));
    }

    if (builder.getTokenType() != ProtoTokenTypes.SEMICOLON
        && builder.getTokenType() != ProtoTokenTypes.COMMA) {
      builder.error("expected ';' or ','");
    } else {
      builder.advanceLexer();
    }
    constMark.done(ProtoElementTypes.ENUM_CONSTANT);
    return enumConstantName;
  }
}
