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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser for {@code message}. {@code extends} and {@code extend}
 * sections of protocol buffer definition file.
 */
public class MessageParser extends AbstractProtoParser {

  public static final String MISSING_MODIFIER_ERROR =
      "missing 'required', 'optional', or 'repeated'";
  public static final String EXPECTED_PROPERTY_TYPE = "expected property type";
  public static final String EXPECTED_PROPERTY_NAME = "expected property name";

  private static final ImmutableList<IElementType> GROUP_BODY_VALID_TOKENS =
      ImmutableList.of(
          ProtoTokenTypes.RBRACE, ProtoTokenTypes.SEMICOLON,
          ProtoTokenTypes.LBRACE, ProtoTokenTypes.LBRACKET);


  private final String messageNamespace;

  /** Names of already parsed messages. */
  private final Set<String> messageNames;

  /** Names of already parsed enums */
  private Map<String, Set<String>> enums;

  public MessageParser(Set<String> messageNames, Map<String, Set<String>> enums) {
    this("", messageNames, enums);
  }

  MessageParser(String messageNamespace, Set<String> messageNames, Map<String, Set<String>> enums) {
    this.messageNamespace = messageNamespace;
    this.messageNames = messageNames;
    this.enums = enums;
  }

  /**
   * Parses the message definition from the proto file. For instance parses the following 
   * string :-
   *
   * message PhoneNumber {
   *   required string number = 1;
   *   optional PhoneType type = 2 [default = HOME];
   * }
   *
   * @param builder PsiBuilder for building PSI tree.
   * @return name of parsed message or empty string.
   *
   * message ::= "message" identifier messageBody
   * 
   */
  public String parseMessageDefinition(PsiBuilder builder) {
    String msgName = "";
    PsiBuilder.Marker messageMarker = builder.mark();

    parseOptionalKeyword(builder, ProtoKeywords.PARSED);
    boolean errorOccured = false;
    if ((!isNextTokenKeyword(builder, ProtoKeywords.MESSAGE)
        && !isNextTokenKeyword(builder, ProtoKeywords.CLASS))
        || (parseNextTokenAsKeyword(builder) == null)) {
      builder.error("expected '" + ProtoKeywords.MESSAGE + "' or '"
                    + ProtoKeywords.CLASS + "' or ' keyword");
      errorOccured = true;
    }

    if (!errorOccured) {
      if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
        builder.error("expected message name");
        errorOccured = true;
      } else {
        msgName = toFQName(messageNamespace, builder.getTokenText());
        parseName(builder);
      }
    }

    if (errorOccured) {
      parseUpTo(builder, VALID_TOKENS_TO_BODY);
    }

    if (builder.getTokenType() == ProtoTokenTypes.LBRACE) {
      parseDefinitionBody(builder, msgName);
      parseOptionalSemicolon(builder);
    } else if (builder.getTokenType() == ProtoTokenTypes.SEMICOLON) {
      builder.advanceLexer();
    }

    messageMarker.done(ProtoElementTypes.MESSAGE_DEFINITION);
    return msgName;
  }

  /**
   * Parses the extend definition from the proto file. For instance parses the following
   * string :-
   *
   * extend Foo {
   *   optional int32 bar = 126;
   * }
   *
   * For details, see https://developers.google.com/protocol-buffers/docs/proto#extensions
   *
   * @param builder PsiBuilder for building PSI tree.
   */
   public void parseExtendDefinition(PsiBuilder builder) {
     parseExtendDefinition(builder, "");
   }

  /**
   * Parses the extend definition defined inside the given message. For instance parses the
   * following string :-
   *
   * message Baz {
   *   extend Foo {
   *     optional int32 bar = 126;
   *   }
   * }
   *
   * For details, see https://developers.google.com/protocol-buffers/docs/proto#extensions
   *
   * @param builder PsiBuilder for building PSI tree.
   * @param curMessageName Message name inside which extends is defined.
   *
   * extend ::= "extend" identifier messageBody
   */
  public void parseExtendDefinition(PsiBuilder builder, String curMessageName) {
    PsiBuilder.Marker extendMarker = builder.mark();

    parseNextTokenAsKeyword(builder);

    if (!isUserDefinedType(builder, curMessageName)) {
      builder.error("expected message name");
      parseUpTo(builder, VALID_TOKENS_TO_BODY);
    } else {
      parseUserDefinedType(builder);
    }

    if (builder.getTokenType() == ProtoTokenTypes.LBRACE) {
      parseDefinitionBody(builder, curMessageName);
      parseOptionalSemicolon(builder);
      extendMarker.done(ProtoElementTypes.EXTEND_DEFINITION);
    } else {
      builder.error("expected '{'");
      extendMarker.drop();
    }
  }

  /**
   * @param builder
   * @param currentMsgName
   *
   * messagebody ::= "{" (field | enum | message | extends |
   *                      extensions | group | option | ":" )* "}"
   */
  private void parseDefinitionBody(PsiBuilder builder, String currentMsgName) {
    parseExpected(builder, ProtoTokenTypes.LBRACE, "{");
    PsiBuilder.Marker bodyMarker = builder.mark();
    while (!builder.eof()
           && builder.getTokenType() != ProtoTokenTypes.RBRACE) {
      if (builder.getTokenType() == ProtoTokenTypes.SEMICOLON) {
        builder.advanceLexer();
        continue;
      }
      if (isNextTokenKeyword(builder, ProtoKeywords.ENUM)) {
        Pair<String, Set<String>> enumDefinition = new EnumParser(currentMsgName)
            .parseEnum(builder);
        if (enumDefinition != null) {
          enums.put(enumDefinition.getFirst(), enumDefinition.getSecond());
        }

      } else if (isNextTokenKeyword(builder, ProtoKeywords.MESSAGE)) {
        MessageParser messageParser = new MessageParser(
            currentMsgName, messageNames, enums);
        String messageName = messageParser.parseMessageDefinition(builder);
        messageNames.add(messageName);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.EXTEND)) {
        parseExtendDefinition(builder, currentMsgName);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.OPTION)) {
        new ProtoOptionParser().parseOption(builder, ProtoElementTypes.MESSAGE_OPTION);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.EXTENSIONS)) {
        parseExtensions(builder);

      } else if (isPropertyModifier(builder)){
        PsiBuilder.Marker propMarker = builder.mark();
        parseNextTokenAsKeyword(builder);

        IElementType nextToken = builder.getTokenType();
        String nextTokenText = builder.getTokenText();
        propMarker.rollbackTo();
        if (isKeyword(nextToken, nextTokenText, ProtoKeywords.GROUP)) {
          parseGroup(builder, currentMsgName);

        } else if (isKeyword(nextToken, nextTokenText, ProtoKeywords.MESSAGE)) {
          // this is only happening in proto1
          parseMessageProperty(builder);

        } else {
          parseSimpleOrEnumProperty(builder, currentMsgName);
        }
      } else {
        builder.error("Message body should contain a <modifier"
            + " fieldname>|<enum>|<message>|<extends>|<extensions>|<group>|<option>|\":\".");
        builder.advanceLexer();
      }
    }
    if (builder.eof()) {
      builder.error("expected '}'");
    } else {
      builder.advanceLexer();
    }
    bodyMarker.done(ProtoElementTypes.DEFINITION_BODY);
  }

  /**
   * Parses the "extensions" statement, defined inside message definition. For ex :-
   *
   * message Foo {
   *    extensions 100 to 199;
   *  }
   *
   * For details, see https://developers.google.com/protocol-buffers/docs/proto#extensions
   * 
   * @param builder PsiBuilder for building PSI tree.
   *
   * extensions ::= "extensions" intLit "to" ( intLit | "max" ) ";"
   */
  private void parseExtensions(PsiBuilder builder) {
    PsiBuilder.Marker extensionsMark = builder.mark();
    parseKeyword(builder, ProtoKeywords.EXTENSIONS);
    if (!parseExtensionsLowerBound(builder)
        || !parseKeyword(builder, ProtoKeywords.TO)
        || !parseExtensionsUpperBound(builder)) {
      extensionsMark.drop();
      return;
    }
    parseExpected(builder, ProtoTokenTypes.SEMICOLON, ";");
    extensionsMark.done(ProtoElementTypes.EXTENSIONS_STATEMENT);
  }

  private boolean parseExtensionsLowerBound(PsiBuilder builder) {
    IElementType expectedLiteral = builder.getTokenType();
    String expectedLiteralText = builder.getTokenText();
    if (expectedLiteral != ProtoTokenTypes.INTEGER_LITERAL) {
      builder.error("expected integer, lower bound for extensions");
      return false;
    }
    PsiBuilder.Marker lowerBoundMarker = builder.mark();
    parseLiteral(builder, expectedLiteral, expectedLiteralText);
    lowerBoundMarker.done(ProtoElementTypes.EXTENSIONS_LOWER_BOUND);
    return true;
  }

  private boolean parseExtensionsUpperBound(PsiBuilder builder) {
    if (isNextTokenKeyword(builder, ProtoKeywords.MAX)) {
      PsiBuilder.Marker upperBoundMarker = builder.mark();
      parseKeyword(builder, ProtoKeywords.MAX);
      upperBoundMarker.done(ProtoElementTypes.EXTENSIONS_UPPER_BOUND);
    } else if (builder.getTokenType() == ProtoTokenTypes.INTEGER_LITERAL) {
      PsiBuilder.Marker upperBoundMarker = builder.mark();
      parseLiteral(builder, ProtoTokenTypes.INTEGER_LITERAL, builder.getTokenText());
      upperBoundMarker.done(ProtoElementTypes.EXTENSIONS_UPPER_BOUND);
    } else {
      builder.error("expected integer, upper bound for extensions");
      return false;
    }
    return true;
  }

  private void parseOptionalKeyword(PsiBuilder builder, ProtoKeywords keyword) {
    if (isNextTokenKeyword(builder, keyword)) {
      parseNextTokenAsKeyword(builder);
    }
  }

  /**
   * @param builder
   * @param currentMsgName
   *
   * group ::= modifier "group" identifier "=" intLit (fieldOptions)? messageBody
   */
  private void parseGroup(PsiBuilder builder, String currentMsgName) {
    PsiBuilder.Marker groupMark = builder.mark();

    String modifier = parsePropertyModifier(builder);
    parseKeyword(builder, ProtoKeywords.GROUP);

    if (!parseNameAndNumber(builder)) {
      parseUpTo(builder, GROUP_BODY_VALID_TOKENS);
    }

    // Group can also have field options, eg:-
    //   optional group DEPRECATED_Manybox = 622 [deprecated=true] {
    if (builder.getTokenType() == ProtoTokenTypes.LBRACKET) {
      ProtoOptionParser optionParser = new ProtoOptionParser();
      optionParser.parseFieldOptions(builder, modifier, null,
          Lists.newArrayList(optionParser.getAllOptions()));
    }
    if (builder.getTokenType() != ProtoTokenTypes.LBRACE) {
      builder.error("expected '{'");
      groupMark.drop();
    } else {
      parseDefinitionBody(builder, currentMsgName);
      parseOptionalSemicolon(builder);
      groupMark.done(ProtoElementTypes.GROUP_DEFINITION);
    }
  }

  private void parseMessageProperty(PsiBuilder builder) {
    PsiBuilder.Marker msgMark = builder.mark();
    String modifier = parsePropertyModifier(builder);
    parseKeyword(builder, ProtoKeywords.MESSAGE);
    if (builder.getTokenType() != ProtoTokenTypes.LT) {
      builder.error("expected '<'");
    }
    builder.advanceLexer();
    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected message type");
    } else {
      PsiBuilder.Marker marker = builder.mark();
      builder.advanceLexer();
      marker.done(ProtoElementTypes.MESSAGE_TYPE_REFERENCE);
    }
    if (builder.getTokenType() != ProtoTokenTypes.GT) {
      builder.error("expected '>'");
    }
    builder.advanceLexer();
    parseNameAndNumber(builder);

    // Message property can also have field options, eg:-
    //   optional message <archives.ArchivesResultSummaryProto> ArchivesSummary = 352 [weak=true];
    if (builder.getTokenType() == ProtoTokenTypes.LBRACKET) {
      ProtoOptionParser optionParser = new ProtoOptionParser();
      optionParser.parseFieldOptions(builder, modifier, null,
          Lists.newArrayList(optionParser.getAllOptions()));
    }
    parseUpToSemicolon(builder, true);
    msgMark.done(ProtoElementTypes.MESSAGE_PROPERTY);
  }

  /**
   * Parses simple or enum property. For ex :-
   *
   * optional int32 result_per_page = 3 [default = 10];
   *
   * fieldType ::= type | userType | enumType
   * field ::= modifier fieldType identifier "=" intLit ( fieldOptionList )? ";"
   */
  private void parseSimpleOrEnumProperty(PsiBuilder builder, String currentMsgName) {
    PsiBuilder.Marker propMark = builder.mark();
    String modifier = parsePropertyModifier(builder);
    Set<String> enumConstants = null;
    IElementType propertyType = ProtoElementTypes.SIMPLE_PROPERTY;
    if (builder.getTokenType() == ProtoTokenTypes.IDENTIFIER) {
      if (ProtoKeywords.isTypeKeyword(builder.getTokenText())) {
        PsiBuilder.Marker marker = builder.mark();
        parseNextTokenAsKeyword(builder);
        marker.done(ProtoElementTypes.PROPERTY_TYPE);
      } else if (isEnumType(builder.getTokenText(), currentMsgName)) {
        enumConstants = getEnumConstants(builder.getTokenText(), currentMsgName);
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer();
        marker.done(ProtoElementTypes.ENUM_PROPERTY_TYPE);
        propertyType = ProtoElementTypes.ENUM_PROPERTY;
      } else if (isUserDefinedType(builder, currentMsgName)) {
        PsiBuilder.Marker marker = builder.mark();
        parseUserDefinedType(builder);
        marker.done(ProtoElementTypes.USER_DEFINED_PROPERTY_TYPE);
        propertyType = ProtoElementTypes.USER_DEFINED_PROPERTY;
      } else {
        builder.error(EXPECTED_PROPERTY_TYPE);
        propMark.drop();
        return;
      }
    } else {
      builder.error(EXPECTED_PROPERTY_NAME);
      propMark.drop();
      return;
    }

    if (!parseNameAndNumber(builder)) {
      builder.error("Expected identifier");
      propMark.drop();
      return;
    }
    if (builder.getTokenType() == ProtoTokenTypes.LBRACKET) {
      ProtoOptionParser optionParser = new ProtoOptionParser();
      optionParser.parseFieldOptions(builder, modifier, enumConstants,
          Lists.newArrayList(optionParser.getAllOptions()));
    }
    parseExpected(builder, ProtoTokenTypes.SEMICOLON, ";");
    propMark.done(propertyType);
  }

  private boolean isEnumType(String enumName, String curMessageName) {
    return enums.containsKey(enumName) ||
        enums.containsKey(toFQName(curMessageName, enumName)) ||
        enums.containsKey(toFQName(messageNamespace, enumName));
  }

  private Set<String> getEnumConstants(String enumName, String curMessageName) {
    if (enums.containsKey(toFQName(curMessageName, enumName))) {
      return enums.get(toFQName(curMessageName, enumName));
    } else if (enums.containsKey(toFQName(messageNamespace, enumName))) {
      return enums.get(toFQName(messageNamespace, enumName));
    } else if (enums.containsKey(enumName)) {
      return enums.get(enumName);
    }  else {
      return null;
    }
  }

  /**
   * Returns whether the next token is a user-defined message type. For instance,
   * in the following string "OtherProto" is a user defined message type.
   *
   * optional OtherProto other = 1;
   */
  private boolean isUserDefinedType(PsiBuilder builder, String currentMsgName) {
    IElementType token = builder.getTokenType();
    String tokenText = builder.getTokenText();

    if (messageNames.contains(tokenText) ||
        messageNames.contains(toFQName(currentMsgName, tokenText)) ||
        messageNames.contains(toFQName(messageNamespace, tokenText))) {
      return true;
    }

    // TODO: Check that the given type exists in the imported protos.
    // For now we assume that the current identifier is a valid message name.
    return token == ProtoTokenTypes.IDENTIFIER;
  }

  /**
   * Parses user-defined message type. For instance, in the following string "OtherProto" is a
   * user defined message type.
   *
   * optional OtherProto other = 1;
   */

  private void parseUserDefinedType(PsiBuilder builder) {
    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("Type name expected");
    } else {
      builder.advanceLexer();
    }
  }

  private boolean isPropertyModifier(PsiBuilder builder) {
    return isNextTokenKeyword(builder, ProtoKeywords.REQUIRED)
           || isNextTokenKeyword(builder, ProtoKeywords.OPTIONAL)
           || isNextTokenKeyword(builder, ProtoKeywords.REPEATED);
  }

  private String parsePropertyModifier(PsiBuilder builder) {
    String modifier = null;
    if (isPropertyModifier(builder)) {
      PsiBuilder.Marker marker = builder.mark();
      modifier = parseNextTokenAsKeyword(builder);
      marker.done(ProtoElementTypes.PROPERTY_MODIFIER);
    } else {
      builder.error(MISSING_MODIFIER_ERROR);
    }

    return modifier;
  }

  private boolean parseNameAndNumber(PsiBuilder builder) {
    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error(EXPECTED_PROPERTY_NAME);
      return false;
    } else {
      parseName(builder);
    }
    if (builder.getTokenType() != ProtoTokenTypes.EQ) {
      builder.error("expected '='");
      return false;
    } else {
      builder.advanceLexer();
      IElementType expectedLiteral = builder.getTokenType();
      String expectedLiteralText = builder.getTokenText();
      if (expectedLiteral != ProtoTokenTypes.INTEGER_LITERAL) {
        builder.error("expected property ID number");
        return false;
      } else {
        PsiBuilder.Marker idMarker = builder.mark();
        parseLiteral(builder, expectedLiteral, expectedLiteralText);
        idMarker.done(ProtoElementTypes.NUMERIC_ID);
      }
    }
    return true;
  }
}
