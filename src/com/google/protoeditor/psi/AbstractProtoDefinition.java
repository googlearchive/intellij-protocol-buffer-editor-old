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

import com.google.protoeditor.ProtoeditorIcon;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * A common class for proto elements that are named and have definition body
 * like {@message} or {@code extend} or {@code enum}.
 */
public abstract class AbstractProtoDefinition extends AbstractProtoElement
    implements ProtoToplevelDefinition, ProtoDefinitionBodyOwner,
               ProtoNamedElement {

  public AbstractProtoDefinition(ASTNode astNode) {
    super(astNode);
  }

  public int getTextOffset() {
    ASTNode nameNode = getNameNode();
    return nameNode == null ? super.getTextOffset() : nameNode.getStartOffset();
  }

  @Override @Nullable
  public String getName() {
    ASTNode nameNode = getNameNode();
    return nameNode == null ? null : nameNode.getText();
  }

  @Nullable
  private ASTNode getNameNode() {
    ProtoNameElement element = getNameElement();
    return element == null ? null : element.getNode();
  }

  public PsiElement setName(String name) throws IncorrectOperationException {
    ASTNode oldNode = getNameNode();

    oldNode.getTreeParent().replaceChild(
        oldNode, ProtoChangeTools.createIdentifierFromText(getProject(), name));
    return this;
  }

  public ProtoDefinitionBody getDefinitionBody() {
    return ProtoPsiTools.findDirectChildOfType(this, ProtoDefinitionBody.class);
  }

  public Icon getIcon(int flags) {
    return ProtoeditorIcon.PROTOEDITOR_ICON;
  }

  @Override
  public ProtoNameElement getNameElement() {
    return ProtoPsiTools.findDirectChildOfType(this, ProtoNameElement.class);
  }

  public ItemPresentation getPresentation() {
    return new ProtoItemPresentation(this);
  }
}
