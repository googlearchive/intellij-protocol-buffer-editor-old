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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.Nullable;

public class ProtoPackageNameReference
    extends AbstractProtoElement implements PsiReference {

  public ProtoPackageNameReference(ASTNode astNode) {
    super(astNode);
  }

  public
  @Nullable
  String getReferencedPackageName() {
    ASTNode nameNode = getPackageNameNode();
    if (nameNode == null) {
      return null;
    }
    return nameNode.getText();
  }

  public
  @Nullable
  ASTNode getPackageNameNode() {
    return getNode().findChildByType(ProtoTokenTypes.IDENTIFIER);
  }

  public PsiElement getElement() {
    return this;
  }

  public TextRange getRangeInElement() {
    return getTextRange();
  }

  public
  @Nullable
  PsiElement resolve() {
    return null;
  }

  public String getCanonicalText() {
    return getText();
  }

  public PsiElement handleElementRename(String newElementName)
      throws IncorrectOperationException {
    setReferencedName(newElementName);
    return this;
  }

  private void setReferencedName(String newElementName) {
    ASTNode oldNode = getPackageNameNode();
    ASTNode newNode = ProtoChangeTools
        .createIdentifierFromText(getProject(), newElementName);
    oldNode.getTreeParent().replaceChild(oldNode, newNode);
  }

  public PsiElement bindToElement(PsiElement element)
      throws IncorrectOperationException {
    PsiPackage pkg = (PsiPackage) element;
    setReferencedName(pkg.getQualifiedName());
    return this;
  }

  public boolean isReferenceTo(PsiElement element) {
    if (!(element instanceof PsiPackage)) {
      return false;
    }
    PsiPackage pkg = (PsiPackage) element;
    return pkg.getQualifiedName().equals(getReferencedPackageName());
  }

  public Object[] getVariants() {
    return new Object[0];
  }

  public boolean isSoft() {
    return false;
  }

}
