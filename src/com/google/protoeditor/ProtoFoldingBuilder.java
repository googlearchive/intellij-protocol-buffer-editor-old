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
import com.google.protoeditor.lex.ProtoTokenTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines rules for folding code in the protocol buffer files.
 */
public class ProtoFoldingBuilder implements FoldingBuilder {

  @Override
  public FoldingDescriptor[] buildFoldRegions(ASTNode node, Document document) {
    List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();
    appendDescriptors(node, document, descriptors);
    return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
  }

  private void appendDescriptors(final ASTNode node,
                                 final Document document,
                                 final List<FoldingDescriptor> descriptors) {
    IElementType type = node.getElementType();
    if (type == ProtoElementTypes.MESSAGE_DEFINITION
        || type == ProtoElementTypes.GROUP_DEFINITION
        || type == ProtoElementTypes.SERVICE_DEFINITION
        || type == ProtoElementTypes.ENUM_DEFINITION
        || type == ProtoElementTypes.RPC_DEFINITION) {
      ASTNode lbraceNode = node.findChildByType(ProtoTokenTypes.LBRACE);
      ASTNode rbraceNode = node.findChildByType(ProtoTokenTypes.RBRACE);
      if (lbraceNode != null && rbraceNode != null) {
        int lbraceStart = lbraceNode.getStartOffset();
        int rbraceStart = rbraceNode.getStartOffset();
        descriptors.add(new FoldingDescriptor(node, new TextRange(lbraceStart, rbraceStart + 1)));
      }
    } else if (type == ProtoTokenTypes.C_STYLE_COMMENT) {
      descriptors.add(new FoldingDescriptor(node, node.getTextRange()));
    }

    ASTNode child = node.getFirstChildNode();
    while (child != null) {
      appendDescriptors(child, document, descriptors);
      child = child.getTreeNext();
    }
  }

  @Override
  public String getPlaceholderText(ASTNode node) {
    if (node.getElementType() == ProtoTokenTypes.C_STYLE_COMMENT) {
      return "/.../";
    } else {
      return "{...}";
    }
  }

  @Override
  public boolean isCollapsedByDefault(ASTNode node) {
    return false;
  }
}
