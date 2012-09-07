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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.psi.tree.IElementType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProtoOptionParser extends AbstractProtoParser {

  public static final String INVALID_DEFAULT_VALUE = "invalid default value";
  public static final String REPEATED_FIELD_WITH_DEFAULTS =
      "repeated fields can't have defaults.";

  /**
   * @param builder
   * @param outerElementType
   *
   * option ::= "option" optionname "=" constant ";"
   */
  public boolean parseOption(PsiBuilder builder, IElementType outerElementType) {
    PsiBuilder.Marker optionMark = builder.mark();
    parseKeyword(builder, ProtoKeywords.OPTION);

    if (!parseOptionName(builder, outerElementType == ProtoElementTypes.MESSAGE_OPTION)) {
      builder.error("expected valid option name");
      optionMark.drop();
      return false;
    }

    if (!parseExpected(builder, ProtoTokenTypes.EQ, "=")) {
      builder.error("expected '='");
      optionMark.drop();
      return false;
    }

    IElementType expectedLiteral = builder.getTokenType();
    String expectedLiteralText = builder.getTokenText();
    if (isLiteral(expectedLiteral, expectedLiteralText)) {
      PsiBuilder.Marker litMarker = builder.mark();
      parseLiteral(builder, expectedLiteral, expectedLiteralText);
      litMarker.done(ProtoElementTypes.OPTION_VALUE);

    } else if (expectedLiteral == ProtoTokenTypes.IDENTIFIER) {
      PsiBuilder.Marker litMarker = builder.mark();
      builder.advanceLexer();
      litMarker.done(ProtoElementTypes.OPTION_VALUE);

    } else {
      builder.error("expected valid option value");

    }
    if (!parseExpected(builder, ProtoTokenTypes.SEMICOLON, ";")) {
      optionMark.done(outerElementType);
      return false;
    }
    optionMark.done(outerElementType);
    return true;
  }

  /**
   * @param builder
   * @param startWithParens
   * @return true if optionname is parsed correctly else false
   *
   * optionname ::= identifier("."identifier)*
   */
  private boolean parseOptionName(PsiBuilder builder, boolean startWithParens) {
    // TODO: need to to parse '.' as token to get this right
    boolean hasParens;
    if (startWithParens) {
      if (parseExpected(builder, ProtoTokenTypes.LPAR, "(")) {
        hasParens = true;
      } else {
        return false;
      }
    } else {
      hasParens = parseOptional(builder, ProtoTokenTypes.LPAR);
    }
    if (!parseTokenAsElement(
        builder,
        ProtoTokenTypes.IDENTIFIER,
        ProtoElementTypes.NAME,
        "expected option name")) {
      return false;
    }
    if (hasParens) {
      if (!parseExpected(builder, ProtoTokenTypes.RPAR, ")")) {
        return false;
      }
    }
    return true;
  }

  /**
   * Fields in proto message defintion can be annotated with "Field Options.". Proto language
   * defines the following field options :-
   *
   * default (eg :- optional int32 result_per_page = 3 [default = 10];)
   * deprecated ( eg :- optional int32 old_field = 6 [deprecated=true];)
   * packed ( eg:- repeated int32 samples = 4 [packed=true];)
   * ctype  ( eg:- optional string foo = 4 [ctype=CORD];)
   * weak (eg :- optional message<StatMessage> Stats = 10  [weak=true];)
   * custom ( eg :- optional int32 foo = 1 [(my_field_option) = 4.5];)
   *
   * For details, see https://developers.google.com/protocol-buffers/docs/proto#options
   */
  public static enum FieldOption {
    DEFAULT(ProtoKeywords.DEFAULT),
    DEPRECATED(ProtoKeywords.DEPRECATED),
    PACKED(ProtoKeywords.PACKED),
    CTYPE(ProtoKeywords.CTYPE),
    WEAK(ProtoKeywords.WEAK),
    LAZY(ProtoKeywords.LAZY),
    CUSTOM_OPTIONS(new ImmutableMap.Builder<IElementType, String>()
        .put(ProtoTokenTypes.LPAR, "(")
        .build());

    private final ImmutableMap<IElementType, String> expectedTokens;

    FieldOption(ProtoKeywords keyword) {
      this.expectedTokens = new ImmutableMap.Builder<IElementType, String>()
          .put(ProtoTokenTypes.IDENTIFIER, keyword.getText())
          .build();
    }

    FieldOption(ImmutableMap<IElementType, String> expectedTokens) {
      this.expectedTokens = expectedTokens;
    }
  }

  /**
   * Parses all the field options, that are used to annotate the fields defined inside
   * a message.
   *
   * @param builder PsiBuilder, for building PSI tree.
   * @param modifier Field modifier for the current field.
   * @param enumConstants Enum constants defined in this message.
   * @param validFieldOptions List of acceptable field options.
   *
   * fieldOptionList = "[" fieldOption ( "," fieldOption )* "]"
   */
  public boolean parseFieldOptions(PsiBuilder builder,
      String modifier,
      Set<String> enumConstants,
      Collection<String> validFieldOptions) {
    parseExpected(builder, ProtoTokenTypes.LBRACKET, "[");

    if (!parseOption(builder, modifier, enumConstants, validFieldOptions)) {
      builder.error("expected default, deprecated, packed or custom option");
      return false;
    }

    while (!builder.eof() && builder.getTokenType() != ProtoTokenTypes.RBRACKET
        && !VALID_TOKENS_TO_BODY.contains(builder.getTokenType())) {
      if (!parseExpected(builder, ProtoTokenTypes.COMMA, ",")) {
        builder.error("Expected ','");
        builder.advanceLexer();
      } else {
        if (parseOption(builder, modifier, enumConstants, validFieldOptions)) {
          break;
        } else {
          builder.error("expected default, deprecated, packed or custom option");
        }
      }
    }

    if (!parseExpected(builder, ProtoTokenTypes.RBRACKET, "]")) {
      builder.error("Expected ',' or ']'");
      return false;
    }
    return true;
  }

  public boolean matchOption(PsiBuilder builder, String option) {
    boolean matched = true;
    PsiBuilder.Marker optionMarker = builder.mark();
    if ((builder.getTokenType() == ProtoTokenTypes.IDENTIFIER
        && (builder.getTokenText().equals(option))
        || (builder.getTokenType() == ProtoTokenTypes.LPAR && builder.getTokenText().equals(option)
            && option.equals("("))
        || Strings.isNullOrEmpty(option))) {
      builder.advanceLexer();
    } else {
      matched = false;
    }
    optionMarker.rollbackTo();
    return matched;
  }

  protected boolean parseOption(PsiBuilder builder, String modifier,
      Set<String> enumConstants, Collection<String> validFieldOptions) {
    boolean matched = false;
    for (String fieldOption : validFieldOptions) {
      if (matchOption(builder, fieldOption)) {
        if (!parseFieldOption(fieldOption, builder, modifier, enumConstants)) {
          return false;
        }
        matched = true;
        break;
      }
    }
    return matched;
  }

  /**
   * Parses a single FieldOption.
   *
   * @param fieldOption FieldOption, to be parsed
   * @param builder PsiBuilder, for building PSI tree.
   * @param modifier Field modifier for the current field
   * @param enumConstants Enum constants defined in this message.
   *
   * fieldOption ::= defaultOption | deprecatedOption | packedOption | ctypeOption | lazyOption
   *                 | weakOption | customOptions | {custom field options}
   */
  protected boolean parseFieldOption(String fieldOption,
      PsiBuilder builder,
      String modifier,
      Set<String> enumConstants) {
    if (fieldOption.equals(ProtoKeywords.DEFAULT.getText())) {
      return parseDefaultValue(builder, modifier, enumConstants);
    } else if (fieldOption.equals("(")) {
      return parseCustomOptions(builder);
    }
    if (!parseStringOption(builder, fieldOption)) {
      builder.error("Unknown field option");
      return false;
    }
    return true;
  }

  /**
   * Parses default value. For ex :-
   *
   * optional int32 result_per_page = 3 [default = 10];
   */
  private boolean parseDefaultValue(PsiBuilder builder,
      String modifier,
      Set<String> enumConstants) {
    parseNextTokenAsKeyword(builder);
    if (!parseExpected(builder, ProtoTokenTypes.EQ, "=")) {
      return false;
    }

    IElementType expectedLiteral = builder.getTokenType();
    String expectedLiteralText = builder.getTokenText();
    if (enumConstants == null) {
      if (!isLiteral(expectedLiteral, expectedLiteralText)) {
        // In proto2, enums can be defined in imported files or can be defined later in the message
        // as independent type. In these cases, we see an identifier but we don't find any matching
        // enum definition to check if it is a valid value.
        if (builder.getTokenType() == ProtoTokenTypes.IDENTIFIER) {
          PsiBuilder.Marker defaultValMarker = builder.mark();
          builder.advanceLexer();
          defaultValMarker.done(ProtoElementTypes.DEFAULT_VALUE);
        } else {
          builder.error("expected default value");
          return false;
        }
      } else {
        PsiBuilder.Marker litMarker = builder.mark();
        parseLiteral(builder, expectedLiteral, expectedLiteralText);
        litMarker.done(ProtoElementTypes.DEFAULT_VALUE);
      }
    } else {
      if (!enumConstants.contains(expectedLiteralText)) {
        builder.error(INVALID_DEFAULT_VALUE);
        return false;
      } else {
        PsiBuilder.Marker litMarker = builder.mark();
        parseName(builder);
        litMarker.done(ProtoElementTypes.DEFAULT_VALUE);
      }
    }

    if (ProtoKeywords.REPEATED.match(modifier)) {
      builder.error(REPEATED_FIELD_WITH_DEFAULTS);
      return false;
    }
    return true;
  }

  /**
   * Parses custom field option. For ex :-
   *
   * optional int32 foo = 1 [(my_field_option) = 4.5];
   */
  private boolean parseCustomOptions(PsiBuilder builder) {
    parseExpected(builder, ProtoTokenTypes.LPAR, "(");
    if (builder.getTokenType() != ProtoTokenTypes.IDENTIFIER) {
      builder.error("expected option name");
      return false;
    } else {
      PsiBuilder.Marker marker = builder.mark();
      builder.advanceLexer();
      marker.done(ProtoElementTypes.CUSTOM_OPTION_NAME);
    }
    if (!parseExpected(builder, ProtoTokenTypes.RPAR, ")")
        || !parseExpected(builder, ProtoTokenTypes.EQ, "=")) {
      return false;
    }

    IElementType expectedLiteral = builder.getTokenType();
    String expectedLiteralText = builder.getTokenText();
    if (!isLiteral(expectedLiteral, expectedLiteralText)) {
      builder.error("expected custom option value");
      return false;
    } else {
      PsiBuilder.Marker litMarker = builder.mark();
      parseLiteral(builder, expectedLiteral, expectedLiteralText);
      litMarker.done(ProtoElementTypes.CUSTOM_OPTION_VALUE);
    }
    return true;
  }

  public ProtoOptionProvider[] getOptionProvider() {
    List<ProtoOptionProvider> extensions = Lists.newArrayList(
        (ProtoOptionProvider) new ProtoOptionProviderImpl());
    extensions.addAll(Lists.newArrayList(Extensions.getExtensions(ProtoOptionProvider.EP_NAME)));

    return extensions.toArray(new ProtoOptionProvider[] {});
  }

  public Set<String> getAllOptions() {
    ProtoOptionProvider[] providers = getOptionProvider();
    Set<String> options = Sets.newHashSet();
    for(ProtoOptionProvider provider : providers) {
      options.addAll(provider.getAllOptions());
    }
    options.addAll(FieldOption.DEFAULT.expectedTokens.values());
    options.addAll(FieldOption.CUSTOM_OPTIONS.expectedTokens.values());
    return options;
  }

  /**
   * Parses field option with string expected values. In the default case, parses
   * packed, deprecated, lazy, weak, ctype. For ex :-
   *
   * repeated int32 samples = 4 [packed=true];
   * or
   * optional int32 old_field = 6 [deprecated=true];
   * or
   * optional int32 not_sure_filed = 7 [lazy=true]
   */
  private boolean parseStringOption(PsiBuilder builder, String expectedName) {
    if (builder.getTokenType() == ProtoTokenTypes.IDENTIFIER
        && builder.getTokenText().equals(expectedName)) {
      parseNextTokenAsKeyword(builder);
    } else {
      builder.error("Expected one of " + expectedName);
    }
    if (!parseExpected(builder, ProtoTokenTypes.EQ, "=")) {
      return false;
    }

    ProtoOptionProvider[] optionProviders = getOptionProvider();
    List<String> expectedValues = Lists.newArrayList();
    for (ProtoOptionProvider optionProvider : optionProviders) {
      expectedValues.addAll(optionProvider.getOptionsFor(expectedName));
    }
    if (builder.getTokenType() == ProtoTokenTypes.IDENTIFIER
        && expectedValues.contains(builder.getTokenText())) {
      builder.advanceLexer();
    } else {
      builder.error("Expected one of " + expectedValues);
      return false;
    }

    boolean canHaveMultipleValues = false;
    for (ProtoOptionProvider optionProvider : optionProviders) {
      canHaveMultipleValues = canHaveMultipleValues
          || optionProvider.hasMultipleOptions(expectedName);
    }

    if (canHaveMultipleValues) {
      while (!builder.eof() && builder.getTokenType() != ProtoTokenTypes.RBRACKET
          && builder.getTokenType() != ProtoTokenTypes.SEMICOLON) {
        if (builder.getTokenType() == ProtoTokenTypes.COMMA) {
          PsiBuilder.Marker moreOpts = builder.mark();
          builder.advanceLexer();
          if (builder.getTokenType() == ProtoTokenTypes.IDENTIFIER
              && expectedValues.contains(builder.getTokenText())) {
            builder.advanceLexer();
            moreOpts.drop();
          } else {
            moreOpts.rollbackTo();
            break;
          }
        }
      }
    }
    return true;
  }
}
