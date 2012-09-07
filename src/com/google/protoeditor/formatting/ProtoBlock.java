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

package com.google.protoeditor.formatting;

import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;
import com.google.protoeditor.psi.ProtoEnumProperty;
import com.google.protoeditor.psi.ProtoKeyword;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoSimpleProperty;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.impl.source.tree.JavaElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProtoBlock implements Block {

  private static final TokenSet BODY_TOKENS = createTokenSet(
      ProtoElementTypes.DEFINITION_BODY,
      ProtoElementTypes.ENUM_BODY,
      ProtoElementTypes.SERVICE_BODY,
      ProtoElementTypes.RPC_BODY);

  private static final TokenSet BODY_OWNER_TOKENS = createTokenSet(
      ProtoElementTypes.MESSAGE_DEFINITION,
      ProtoElementTypes.GROUP_DEFINITION,
      ProtoElementTypes.ENUM_DEFINITION,
      ProtoElementTypes.RPC_DEFINITION,
      ProtoElementTypes.SERVICE_DEFINITION);
  private static final TokenSet STATEMENT_LIKE_TOKENS = createTokenSet(
      ProtoElementTypes.OPTION,
      ProtoElementTypes.SIMPLE_PROPERTY,
      ProtoElementTypes.ENUM_PROPERTY,
      ProtoElementTypes.ENUM_CONSTANT,
      ProtoElementTypes.FILE_OPTION_STATEMENT,
      ProtoElementTypes.LANGUAGE_LITERAL,
      ProtoElementTypes.MESSAGE_PROPERTY,
      ProtoElementTypes.PACKAGE_STATEMENT,
      ProtoElementTypes.IMPORT_STATEMENT,
      ProtoElementTypes.SYNTAX_STATEMENT);

  private static TokenSet createTokenSet(IElementType... types) {
    return TokenSet.create(types);
  }

  private ASTNode myNode;

  private final CodeStyleSettings mySettings;

  private Alignment myAlignment;
  private Indent myIndent;
  private Wrap myWrap;
  private Alignment childAlignment = Alignment.createAlignment();
  private static final TokenSet LEFT_TOKENS = createTokenSet(
      ProtoTokenTypes.LT, ProtoTokenTypes.LBRACKET,
      ProtoTokenTypes.LPAR);
  private static final TokenSet RIGHT_TOKENS = createTokenSet(
      ProtoTokenTypes.GT, ProtoTokenTypes.RBRACKET,
      ProtoTokenTypes.RPAR);
  private List<Block> subBlocks = null;

  public ProtoBlock(final ASTNode node, final Alignment alignment,
                    final Indent indent, final Wrap wrap, final CodeStyleSettings settings) {
    myAlignment = alignment;
    myIndent = indent;
    myNode = node;
    myWrap = wrap;
    mySettings = settings;
  }

  public ASTNode getNode() {
    return myNode;
  }

  @NotNull
  @Override
  public TextRange getTextRange() {
    return myNode.getTextRange();
  }

  @NotNull
  @Override
  public List<Block> getSubBlocks() {
    if (subBlocks == null) {
      subBlocks = buildSubBlocks();
    }
    return subBlocks;
  }

  private List<Block> buildSubBlocks() {
    List<Block> blocks = new ArrayList<Block>();
    Wrap wrap = Wrap.createWrap(WrapType.NORMAL, true);
    Alignment alignment = getChildAlignment();
    Indent indentMe = getChildIndent();
    for (ASTNode child = myNode.getFirstChildNode();
         child != null; child = child.getTreeNext()) {

      IElementType childType = child.getElementType();
      if (isWhitespace(childType)
          || child.getTextRange().getLength() == 0) {
        continue;
      }
      if (childType == JavaElementType.IMPORT_STATEMENT) {
        blocks.add(new ReadOnlyBlock(child));
        continue;
      }
      blocks.add(new ProtoBlock(child, alignment, indentMe, wrap,
                                mySettings));
    }
    return Collections.unmodifiableList(blocks);
  }

  private Alignment getChildAlignment() {
    Alignment alignment;
    if (BODY_TOKENS.contains(myNode.getElementType())) {
      alignment = childAlignment;
    } else {
      alignment = null;
    }
    return alignment;
  }

  private Indent getChildIndent() {
    if (BODY_TOKENS.contains(myNode.getElementType())) {
      return Indent.getNormalIndent();
    } else {
      return Indent.getNoneIndent();
    }
  }

  @Nullable
  @Override
  public Wrap getWrap() {
    return myWrap;
  }

  @Nullable
  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Nullable
  @Override
  public Alignment getAlignment() {
    return myAlignment;
  }

  @Nullable
  @Override
  public Spacing getSpacing(Block child1, Block child2) {
    if (!(child1 instanceof ProtoBlock) || !(child2 instanceof ProtoBlock)) {
      return null;
    }

    ProtoBlock protoBlock1 = (ProtoBlock) child1;
    ProtoBlock protoBlock2 = (ProtoBlock) child2;

    ASTNode node1 = protoBlock1.getNode();
    IElementType type1 = node1.getElementType();
    ASTNode node2 = protoBlock2.getNode();
    IElementType type2 = node2.getElementType();

    if (isWhitespace(type1) || isWhitespace(type2)
        || node1.getTreeParent().getElementType()
           == ProtoElementTypes.LANGUAGE_LITERAL) {
      return null;
    }

    boolean firstIsStatementLike = STATEMENT_LIKE_TOKENS.contains(type1);
    boolean secondIsStatementLike = STATEMENT_LIKE_TOKENS.contains(type2);
    boolean oneIsStatementLike = firstIsStatementLike
                                 || secondIsStatementLike;
    boolean firstHasBody = BODY_OWNER_TOKENS.contains(type1);
    boolean secondHasBody = BODY_OWNER_TOKENS.contains(type2);
    boolean keepLineBreaks = mySettings.KEEP_LINE_BREAKS;
    if ((firstHasBody && secondHasBody)
        || ((firstHasBody || secondHasBody) && oneIsStatementLike)) {
      return Spacing.createSpacing(0, Integer.MAX_VALUE, 2,
                                   keepLineBreaks, 1);

    } else if (firstIsStatementLike && secondIsStatementLike) {
      return Spacing.createSpacing(0, Integer.MAX_VALUE, 1,
                                   keepLineBreaks, 1);

    } else if (type2 == ProtoTokenTypes.SEMICOLON
               || isLeftThing(type1) || isRightThing(type2)
               || type2 == ProtoTokenTypes.LT
               || (type2 == ProtoTokenTypes.LPAR
                   && !(node1.getPsi() instanceof ProtoKeyword))
               || (type1 == ProtoTokenTypes.COMMA
                   || type2 == ProtoTokenTypes.COMMA)
               || ((type1 == ProtoTokenTypes.EQ || type2 == ProtoTokenTypes.EQ)
                   && isChildOfBracketyThing(node1))) {
      return Spacing.createSpacing(0, 1, 0,
                                   keepLineBreaks, 1);

    } else if (type1 == ProtoTokenTypes.EQ || type2 == ProtoTokenTypes.EQ
               || isComment(type1) || isComment(type2)) {
      // allow custom spacing
      return Spacing.createSpacing(1, Integer.MAX_VALUE, 0,
                                   keepLineBreaks, 1);

    } else {
      return Spacing.createSpacing(1, 1, 0, keepLineBreaks, 1);
    }
  }

  private boolean isChildOfBracketyThing(ASTNode node1) {
    PsiElement childPsi = node1.getPsi();
    ProtoProperty prop = PsiTreeUtil.getParentOfType(childPsi,
                                                     ProtoSimpleProperty.class);
    if (prop == null) {
      prop = PsiTreeUtil.getParentOfType(childPsi,
                                         ProtoEnumProperty.class);
      if (prop == null) {
        return false;
      }
    }
    ASTNode cnode = prop.getNode().getFirstChildNode();
    for (; cnode != null; cnode = cnode.getTreeNext()) {
      if (PsiTreeUtil.isAncestor(cnode.getPsi(), childPsi, false)) {
        ASTNode dnode = cnode;
        while (dnode != null) {
          if (dnode.getElementType() == ProtoTokenTypes.LBRACKET) {
            return true;
          }
          dnode = dnode.getTreePrev();
        }
        break;
      }
    }
    return false;
  }

  private boolean isComment(IElementType type1) {
    return type1 == ProtoTokenTypes.END_OF_LINE_COMMENT
           || type1 == ProtoTokenTypes.C_STYLE_COMMENT;
  }

  private boolean isLeftThing(IElementType type) {
    return LEFT_TOKENS.contains(type);
  }

  private boolean isRightThing(IElementType type) {
    return RIGHT_TOKENS.contains(type);
  }

  private boolean isWhitespace(IElementType type1) {
    return type1 == ProtoTokenTypes.WHITE_SPACE
           || type1 == TokenType.WHITE_SPACE;
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(final int newChildIndex) {
    return new ChildAttributes(getChildIndent(), getChildAlignment());
  }

  @Override
  public boolean isIncomplete() {
    return isIncomplete(myNode);
  }

  private static boolean isIncomplete(ASTNode node) {
    ASTNode lastChild = node.getLastChildNode();
    while (lastChild != null && lastChild.getPsi() instanceof PsiWhiteSpace) {
      lastChild = lastChild.getTreePrev();
    }
    if (lastChild == null) {
      return false;
    }
    if (lastChild.getPsi() instanceof PsiErrorElement) {
      return true;
    }
    return isIncomplete(lastChild);
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null;
  }

  private static class ReadOnlyBlock implements Block {

    private final ASTNode child;

    public ReadOnlyBlock(ASTNode child) {
      this.child = child;
    }

    @NotNull
    @Override
    public TextRange getTextRange() {
      return child.getTextRange();
    }

    @NotNull
    @Override
    public List<Block> getSubBlocks() {
      return Collections.emptyList();
    }

    @Nullable
    @Override
    public Wrap getWrap() {
      return null;
    }

    @Nullable
    @Override
    public Indent getIndent() {
      return null;
    }

    @Nullable
    @Override
    public Alignment getAlignment() {
      return null;
    }

    @Nullable
    @Override
    public Spacing getSpacing(Block child1, Block child2) {
      return null;
    }

    @NotNull
    @Override
    public ChildAttributes getChildAttributes(final int newChildIndex) {
      return new ChildAttributes(null, null);
    }

    @Override
    public boolean isIncomplete() {
      return false;
    }

    @Override
    public boolean isLeaf() {
      return true;
    }
  }
}
