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

import com.google.common.collect.Maps;
import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProtoParser extends AbstractProtoParser implements PsiParser {

  /**
   * @param root
   * @param builder
   * @return AST tree built after parsing the proto file.
   *
   * proto ::= message | extend | enum | import | package | option | syntax | service |  
   */
  @NotNull
  @Override
  public ASTNode parse(IElementType root, PsiBuilder builder) {
    Set<String> parsedMessageNames = new HashSet();
    Map<String, Set<String>> parsedEnums = Maps.newHashMap();
    PsiBuilder.Marker rootMarker = builder.mark();
    while (!builder.eof()) {
      if (builder.getTokenType() == ProtoTokenTypes.LANGUAGE_LITERAL) {
        parseLanguageLiteral(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.SERVICE)) {
        parseServiceDefinition(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.PACKAGE)) {
        parsePackageStatement(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.SYNTAX)) {
        parseSyntaxStatement(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.IMPORT)) {
        parseImportStatement(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.OPTION)) {
        parseFileOptionStatement(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.EXTEND)) {
        MessageParser messageParser = new MessageParser(parsedMessageNames, parsedEnums);
        messageParser.parseExtendDefinition(builder);

      } else if (isNextTokenKeyword(builder, ProtoKeywords.ENUM)) {
        Pair<String,Set<String>> enumDefinition = new EnumParser().parseEnum(builder);
        if (enumDefinition != null) {
          parsedEnums.put(enumDefinition.getFirst(), enumDefinition.getSecond());
        }

      } else if (isNextTokenKeyword(builder, ProtoKeywords.MESSAGE)
          || isNextTokenKeyword(builder, ProtoKeywords.PARSED)) {
        MessageParser messageParser = new MessageParser(parsedMessageNames, parsedEnums);
        String messageName = messageParser.parseMessageDefinition(builder);
        parsedMessageNames.add(messageName);
      } else {
        builder.error("Expected keyword");
        builder.advanceLexer();
      }
    }
    rootMarker.done(root);
    return builder.getTreeBuilt();
  }

  private void parseLanguageLiteral(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    if (!builder.eof()) {
      builder.advanceLexer();
    }
    if (parseNextTokenAsKeyword(builder) == null) {
      marker.error("Expected keyword");
      return;
    }
    marker.done(ProtoElementTypes.LANGUAGE_LITERAL);
  }

  private void parsePackageStatement(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    if (!parseKeyword(builder, ProtoKeywords.PACKAGE)
      || !parseTokenAsElement(builder,
            ProtoTokenTypes.IDENTIFIER,
            ProtoElementTypes.PACKAGE_NAME, "expected package name")) {
      marker.drop();
      return;
    }
    parseExpected(builder, ProtoTokenTypes.SEMICOLON, ";");
    marker.done(ProtoElementTypes.PACKAGE_STATEMENT);
  }

  private void parseSyntaxStatement(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    parseKeyword(builder, ProtoKeywords.SYNTAX);
    if (!parseExpected(builder, ProtoTokenTypes.EQ, "=")
      || !parseTokenAsElement(
            builder,
            ProtoTokenTypes.STRING_LITERAL,
            ProtoElementTypes.SYNTAX_VALUE,
            "expected syntax value")) {
      marker.drop();
      return;
    }
    parseExpected(builder, ProtoTokenTypes.SEMICOLON, ";");
    marker.done(ProtoElementTypes.SYNTAX_STATEMENT);
  }

  private void parseImportStatement(PsiBuilder builder) {
    PsiBuilder.Marker marker = builder.mark();
    if (!parseKeyword(builder, ProtoKeywords.IMPORT)
      || !parseTokenAsElement(
            builder,
            ProtoTokenTypes.STRING_LITERAL,
            ProtoElementTypes.IMPORT_VALUE,
            "expected import value")) {
      marker.drop();
      return;
    }
    parseExpected(builder, ProtoTokenTypes.SEMICOLON, ";");
    marker.done(ProtoElementTypes.IMPORT_STATEMENT);
  }

  /**
   * @param builder
   *
   * service ::= "service" identifier "{" (option | rpc | ";")* "}"
   */
  private void parseServiceDefinition(PsiBuilder builder) {
    PsiBuilder.Marker serviceMark = builder.mark();
    parseKeyword(builder, ProtoKeywords.SERVICE);

    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected service name");
      parseUpTo(builder, VALID_TOKENS_TO_BODY);
    } else {
      parseName(builder);
    }
    parseExpected(builder, ProtoTokenTypes.LBRACE, "{");

    PsiBuilder.Marker bodyMarker = builder.mark();
    boolean errorOccured = false;
    while (!errorOccured && !builder.eof() && builder.getTokenType() != ProtoTokenTypes.RBRACE) {

      if (builder.getTokenType() == ProtoTokenTypes.SEMICOLON) {
        builder.advanceLexer();
      } else if (isNextTokenKeyword(builder, ProtoKeywords.OPTION)) {
        parseRpcOption(builder);
      } else if (isNextTokenKeyword(builder, ProtoKeywords.RPC)) {
        parseRpc(builder);
      } else {
        builder.error("Expected option | rpc | semicolon");
        errorOccured = true;
      }
    }

    if (builder.eof()) {
      builder.error("expected '}'");
      bodyMarker.done(ProtoElementTypes.SERVICE_BODY);
    } else {
      parseExpected(builder, ProtoTokenTypes.RBRACE, "}");
      bodyMarker.done(ProtoElementTypes.SERVICE_BODY);
      parseOptionalSemicolon(builder);
    }

    serviceMark.done(ProtoElementTypes.SERVICE_DEFINITION);
  }

  /**
   * @param builder
   * @return
   *
   * rpc ::= "rpc" identifier parameter "r" parameter ( ";" | rpcbody )
   */
  private void parseRpc(PsiBuilder builder) {

    PsiBuilder.Marker rpcMark = builder.mark();
    parseKeyword(builder, ProtoKeywords.RPC);

    boolean errorOccured = false;
    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected rpc name");
      errorOccured = true;
    } else {
      parseName(builder);
    }
    if (!errorOccured
        && !readParenthesizedTypeReference(builder, ProtoElementTypes.RPC_INPUT_TYPE)) {
      errorOccured = true;
    }
    if (!errorOccured
        && !parseKeyword(builder, ProtoKeywords.RETURNS)) {
      errorOccured = true;
    }
    if (!errorOccured
        && !readParenthesizedTypeReference(builder, ProtoElementTypes.RPC_RETURN_TYPE)) {
      errorOccured = true;
    }

    if (errorOccured) {
      builder.error("Malformed rpc statement");
      parseUpTo(builder, VALID_TOKENS_TO_BODY);
      errorOccured = false;
    }
     if (builder.getTokenType() == ProtoTokenTypes.LBRACE) {
      builder.advanceLexer();
      parseRpcBody(builder);
      parseOptionalSemicolon(builder);
    } else if (builder.getTokenType() == ProtoTokenTypes.SEMICOLON) {
      builder.advanceLexer();
    } else {
      builder.error("expected '{' or ';'");
    }
    rpcMark.done(ProtoElementTypes.RPC_DEFINITION);
  }

  /**
   * @param builder
   *
   * Rpc body is optional.
   *
   * rpcbody ::= ("{" (option | ";")* "}")* (";")?
   *
   */
  private void parseRpcBody(PsiBuilder builder) {

    if (isNextTokenKeyword(builder, ProtoKeywords.RPC)) {
      builder.advanceLexer();
    }
    PsiBuilder.Marker bodyMarker = builder.mark();
    while (!builder.eof() && builder.getTokenType() != ProtoTokenTypes.RBRACE) {
      if (builder.getTokenType() == ProtoTokenTypes.SEMICOLON
          || !parseRpcOption(builder)) {
        builder.advanceLexer();
      }
    }
    if (builder.eof()) {
      builder.error("expected '}'");
    } else {
      assert builder.getTokenType() == ProtoTokenTypes.RBRACE;
      builder.advanceLexer();
    }
    parseOptionalSemicolon(builder);
    bodyMarker.done(ProtoElementTypes.RPC_BODY);
  }

  /**
   * @param builder
   * @param elementType
   * @return true if no error occured, else false.
   *
   * parameter = "(" userType ")"
   */
  private boolean readParenthesizedTypeReference(PsiBuilder builder,
                                              IElementType elementType) {
    if (builder.getTokenType() != ProtoTokenTypes.LPAR) {
      builder.error("expected '('");
      return false;
    } else {
      builder.advanceLexer();
    }
    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected input parameter type");
      return false;
    } else {
      PsiBuilder.Marker outermark = builder.mark();
      PsiBuilder.Marker marker = builder.mark();
      builder.advanceLexer();
      marker.done(ProtoElementTypes.MESSAGE_TYPE_REFERENCE);
      outermark.done(elementType);
    }
    if (builder.getTokenType() != ProtoTokenTypes.RPAR) {
      builder.error("expected ')'");
      return false;
    } else {
      builder.advanceLexer();
    }
    return true;
  }

  private void parseFileOptionStatement(PsiBuilder builder) {
    new ProtoOptionParser().parseOption(builder, ProtoElementTypes.FILE_OPTION_STATEMENT);
  }

  private boolean parseRpcOption(PsiBuilder builder) {
    return new ProtoOptionParser().parseOption(builder, ProtoElementTypes.OPTION);
  }
}
