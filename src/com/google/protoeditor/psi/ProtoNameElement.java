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

package com.google.protoeditor.psi;

import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.ASTNode;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.Nullable;

public class ProtoNameElement
    extends AbstractProtoElement
    implements ProtoElement, ProtoElementWithName {

  public ProtoNameElement(ASTNode astNode) {
    super(astNode);
  }

  @Nullable
  public String getName() {
    ASTNode node = getIdentifierNode();
    return node == null ? getText() : node.getText();
  }

  public void setName(String name) throws IncorrectOperationException {
    ASTNode nameNode = getIdentifierNode();
    if (nameNode == null) {
      throw new IncorrectOperationException("no name element");
    }

    nameNode.getTreeParent().replaceChild(nameNode, ProtoChangeTools
        .createIdentifierFromText(getProject(), name));
  }

  @Nullable
  public ASTNode getIdentifierNode() {
    return getNode().findChildByType(ProtoTokenTypes.IDENTIFIER);
  }
}
