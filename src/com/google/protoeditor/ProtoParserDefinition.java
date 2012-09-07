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

package com.google.protoeditor;


import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoLexer;
import com.google.protoeditor.lex.ProtoTokenTypes;
import com.google.protoeditor.parsing.ProtoParser;
import com.google.protoeditor.psi.ProtoBooleanLiteral;
import com.google.protoeditor.psi.ProtoCustomOptionName;
import com.google.protoeditor.psi.ProtoCustomOptionValue;
import com.google.protoeditor.psi.ProtoDefaultValue;
import com.google.protoeditor.psi.ProtoDefinitionBody;
import com.google.protoeditor.psi.ProtoEnumBody;
import com.google.protoeditor.psi.ProtoEnumConstant;
import com.google.protoeditor.psi.ProtoEnumDefinition;
import com.google.protoeditor.psi.ProtoEnumProperty;
import com.google.protoeditor.psi.ProtoEnumPropertyType;
import com.google.protoeditor.psi.ProtoEnumValue;
import com.google.protoeditor.psi.ProtoExtendDefinition;
import com.google.protoeditor.psi.ProtoExtensionsLowerBound;
import com.google.protoeditor.psi.ProtoExtensionsStatement;
import com.google.protoeditor.psi.ProtoExtensionsUpperBound;
import com.google.protoeditor.psi.ProtoFile;
import com.google.protoeditor.psi.ProtoFileOptionStatement;
import com.google.protoeditor.psi.ProtoFloatLiteral;
import com.google.protoeditor.psi.ProtoGroupDefinition;
import com.google.protoeditor.psi.ProtoHexLiteral;
import com.google.protoeditor.psi.ProtoImportStatement;
import com.google.protoeditor.psi.ProtoImportValue;
import com.google.protoeditor.psi.ProtoIntegerLiteral;
import com.google.protoeditor.psi.ProtoJavaLiteral;
import com.google.protoeditor.psi.ProtoKeyword;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoMessageProperty;
import com.google.protoeditor.psi.ProtoMessageTypeReference;
import com.google.protoeditor.psi.ProtoNameElement;
import com.google.protoeditor.psi.ProtoOption;
import com.google.protoeditor.psi.ProtoOptionValue;
import com.google.protoeditor.psi.ProtoPackageNameReference;
import com.google.protoeditor.psi.ProtoPackageStatement;
import com.google.protoeditor.psi.ProtoPropertyId;
import com.google.protoeditor.psi.ProtoPropertyModifier;
import com.google.protoeditor.psi.ProtoRpcBody;
import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoRpcInputType;
import com.google.protoeditor.psi.ProtoRpcReturnType;
import com.google.protoeditor.psi.ProtoServiceBody;
import com.google.protoeditor.psi.ProtoServiceDefinition;
import com.google.protoeditor.psi.ProtoSimpleProperty;
import com.google.protoeditor.psi.ProtoSimplePropertyType;
import com.google.protoeditor.psi.ProtoStringLiteral;
import com.google.protoeditor.psi.ProtoSyntaxStatement;
import com.google.protoeditor.psi.ProtoSyntaxValue;
import com.google.protoeditor.psi.ProtoUserDefinedProperty;
import com.google.protoeditor.psi.ProtoUserDefinedPropertyType;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;

import org.jetbrains.annotations.NotNull;

public class ProtoParserDefinition implements ParserDefinition {

  @Override
  @NotNull
  public Lexer createLexer(Project project) {
    return new ProtoLexer();
  }

  @Override
  @NotNull
  public PsiParser createParser(Project project) {
    return new ProtoParser();
  }

  @Override
  public IFileElementType getFileNodeType() {
    return ProtoElementTypes.FILE;
  }

  @Override
  @NotNull
  public TokenSet getWhitespaceTokens() {
    return TokenSet.create(ProtoTokenTypes.WHITE_SPACE);
  }

  @Override
  @NotNull
  public TokenSet getCommentTokens() {
    return TokenSet.create(ProtoTokenTypes.C_STYLE_COMMENT,
                           ProtoTokenTypes.END_OF_LINE_COMMENT);
  }

  @Override
  @NotNull
  public TokenSet getStringLiteralElements() {
    return TokenSet.create(ProtoTokenTypes.STRING_LITERAL);
  }

  @Override
  @NotNull
  public PsiElement createElement(ASTNode astNode) {
    IElementType elementType = astNode.getElementType();
    if (elementType == ProtoElementTypes.SIMPLE_PROPERTY) {
      return new ProtoSimpleProperty(astNode);

    } else if (elementType == ProtoElementTypes.ENUM_PROPERTY) {
      return new ProtoEnumProperty(astNode);

    } else if (elementType == ProtoElementTypes.NAME) {
      return new ProtoNameElement(astNode);

    } else if (elementType == ProtoElementTypes.ENUM_DEFINITION) {
      return new ProtoEnumDefinition(astNode);

    } else if (elementType == ProtoElementTypes.ENUM_CONSTANT) {
      return new ProtoEnumConstant(astNode);

    } else if (elementType == ProtoElementTypes.ENUM_BODY) {
      return new ProtoEnumBody(astNode);

    } else if (elementType == ProtoElementTypes.SERVICE_BODY) {
      return new ProtoServiceBody(astNode);

    } else if (elementType == ProtoElementTypes.RPC_BODY) {
      return new ProtoRpcBody(astNode);

    } else if (elementType == ProtoElementTypes.GROUP_DEFINITION) {
      return new ProtoGroupDefinition(astNode);

    } else if (elementType == ProtoElementTypes.MESSAGE_DEFINITION) {
      return new ProtoMessageDefinition(astNode);

    } else if (elementType == ProtoElementTypes.EXTEND_DEFINITION) {
      return new ProtoExtendDefinition(astNode);

    } else if (elementType == ProtoElementTypes.MESSAGE_PROPERTY) {
      return new ProtoMessageProperty(astNode);

    } else if (elementType == ProtoElementTypes.MESSAGE_TYPE_REFERENCE) {
      return new ProtoMessageTypeReference(astNode);

    } else if (elementType == ProtoElementTypes.NUMERIC_ID) {
      return new ProtoPropertyId(astNode);

    } else if (elementType == ProtoElementTypes.OPTION ||
        elementType == ProtoElementTypes.MESSAGE_OPTION) {
      return new ProtoOption(astNode);

    } else if (elementType == ProtoElementTypes.OPTION_VALUE) {
      return new ProtoOptionValue(astNode);

    } else if (elementType == ProtoElementTypes.PROPERTY_MODIFIER) {
      return new ProtoPropertyModifier(astNode);

    } else if (elementType == ProtoElementTypes.PROPERTY_TYPE) {
      return new ProtoSimplePropertyType(astNode);

    } else if (elementType == ProtoElementTypes.ENUM_PROPERTY_TYPE) {
      return new ProtoEnumPropertyType(astNode);

    } else if (elementType == ProtoElementTypes.DEFAULT_VALUE) {
      return new ProtoDefaultValue(astNode);

    } else if (elementType == ProtoElementTypes.INTEGER_LITERAL) {
      return new ProtoIntegerLiteral(astNode);

    } else if (elementType == ProtoElementTypes.HEX_LITERAL) {
      return new ProtoHexLiteral(astNode);

    } else if (elementType == ProtoElementTypes.FLOAT_LITERAL) {
      return new ProtoFloatLiteral(astNode);

    } else if (elementType == ProtoElementTypes.STRING_LITERAL) {
      return new ProtoStringLiteral(astNode);

    } else if (elementType == ProtoElementTypes.ENUM_VALUE) {
      return new ProtoEnumValue(astNode);

    } else if (elementType == ProtoElementTypes.DEFINITION_BODY) {
      return new ProtoDefinitionBody(astNode);

    } else if (elementType == ProtoElementTypes.SERVICE_DEFINITION) {
      return new ProtoServiceDefinition(astNode);

    } else if (elementType == ProtoElementTypes.RPC_DEFINITION) {
      return new ProtoRpcDefinition(astNode);

    } else if (elementType == ProtoElementTypes.RPC_INPUT_TYPE) {
      return new ProtoRpcInputType(astNode);

    } else if (elementType == ProtoElementTypes.RPC_RETURN_TYPE) {
      return new ProtoRpcReturnType(astNode);

    } else if (elementType == ProtoElementTypes.KEYWORD) {
      return new ProtoKeyword(astNode);

    } else if (elementType == ProtoElementTypes.BOOLEAN_LITERAL) {
      return new ProtoBooleanLiteral(astNode);

    } else if (elementType == ProtoElementTypes.PACKAGE_STATEMENT) {
      return new ProtoPackageStatement(astNode);

    } else if (elementType == ProtoElementTypes.PACKAGE_NAME) {
      return new ProtoPackageNameReference(astNode);

    } else if (elementType == ProtoElementTypes.LANGUAGE_LITERAL) {
      return new ProtoJavaLiteral(astNode);

    } else if (elementType == ProtoElementTypes.SYNTAX_STATEMENT) {
      return new ProtoSyntaxStatement(astNode);

    } else if (elementType == ProtoElementTypes.SYNTAX_VALUE) {
      return new ProtoSyntaxValue(astNode);

    } else if (elementType == ProtoElementTypes.IMPORT_STATEMENT) {
      return new ProtoImportStatement(astNode);

    } else if (elementType == ProtoElementTypes.IMPORT_VALUE) {
      return new ProtoImportValue(astNode);

    } else if (elementType == ProtoElementTypes.FILE_OPTION_STATEMENT) {
      return new ProtoFileOptionStatement(astNode);

    } else if (elementType == ProtoElementTypes.USER_DEFINED_PROPERTY_TYPE) {
      return new ProtoUserDefinedPropertyType(astNode);

    } else if (elementType == ProtoElementTypes.USER_DEFINED_PROPERTY) {
      return new ProtoUserDefinedProperty(astNode);

    } else if (elementType == ProtoElementTypes.EXTENSIONS_STATEMENT) {
      return new ProtoExtensionsStatement(astNode);

    } else if (elementType == ProtoElementTypes.EXTENSIONS_LOWER_BOUND) {
       return new ProtoExtensionsLowerBound(astNode);

    } else if (elementType == ProtoElementTypes.EXTENSIONS_UPPER_BOUND) {
       return new ProtoExtensionsUpperBound(astNode);

    } else if (elementType == ProtoElementTypes.CUSTOM_OPTION_VALUE) {
       return new ProtoCustomOptionValue(astNode);

    } else if (elementType == ProtoElementTypes.CUSTOM_OPTION_NAME) {
      return new ProtoCustomOptionName(astNode);

    } else {
      throw new IllegalArgumentException("unknown parsed type "
                                         + elementType + " for " + astNode);
    }
  }

  @Override
  public PsiFile createFile(FileViewProvider viewProvider) {
    return new ProtoFile(viewProvider);
  }

  @Override
  public SpaceRequirements spaceExistanceTypeBetweenTokens(
      ASTNode left, ASTNode right) {
    Lexer lexer = createLexer(left.getPsi().getProject());
    return LanguageUtil.canStickTokensTogetherByLexer(left, right, lexer);
  }
}
