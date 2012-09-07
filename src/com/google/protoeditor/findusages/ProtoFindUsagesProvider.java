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

package com.google.protoeditor.findusages;

import com.google.protoeditor.lex.ProtoElementTypes;
import com.google.protoeditor.lex.ProtoTokenTypes;
import com.google.protoeditor.psi.ProtoElementWithName;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoServiceDefinition;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtoFindUsagesProvider implements FindUsagesProvider {

  public boolean mayHaveReferences(IElementType token, short searchContext) {
    if ((searchContext & UsageSearchContext.IN_CODE) != 0
        && (token == ProtoElementTypes.MESSAGE_TYPE_REFERENCE)) {
      return true;
    }
    if ((searchContext & UsageSearchContext.IN_COMMENTS) != 0 && (
        token == ProtoTokenTypes.END_OF_LINE_COMMENT || token == ProtoTokenTypes.C_STYLE_COMMENT)) {
      return true;
    }
    if ((searchContext & UsageSearchContext.IN_STRINGS) != 0
        && token == ProtoTokenTypes.STRING_LITERAL) {
      return true;
    }
    return false;
  }

  @Nullable
  public WordsScanner getWordsScanner() {
    return new ProtoWordsScanner();
  }

  public boolean canFindUsagesFor(PsiElement psiElement) {
    return psiElement instanceof ProtoMessageDefinition;
  }

  @Nullable
  public String getHelpId(PsiElement psiElement) {
    return null;
  }

  @NotNull
  public String getType(PsiElement psiElement) {
    if (psiElement instanceof ProtoMessageDefinition) {
      return "message";
    }
    if (psiElement instanceof ProtoServiceDefinition) {
      return "service";
    }
    if (psiElement instanceof ProtoProperty) {
      return "property";
    }
    if (psiElement instanceof ProtoRpcDefinition) {
      return "rpc";
    }
    return "";
  }

  @NotNull
  public String getDescriptiveName(PsiElement psiElement) {
    return getNodeText(psiElement, false);
  }

  @NotNull
  public String getNodeText(PsiElement psiElement, boolean useFullName) {
    if (psiElement instanceof ProtoElementWithName) {
      ProtoElementWithName el = (ProtoElementWithName) psiElement;

      String name = el.getName();
      if (name != null) {
        return name;
      }
    }
    return "<unknown>";
  }

}
