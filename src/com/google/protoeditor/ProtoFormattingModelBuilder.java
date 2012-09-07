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

import com.google.protoeditor.formatting.ProtoBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingDocumentModel;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the formatting model for the protocol buffer file.
 */
public class ProtoFormattingModelBuilder implements FormattingModelBuilder {

  @NotNull
  @Override
  public FormattingModel createModel(PsiElement element, CodeStyleSettings settings) {
    return new ProtoFormattingModel(element, settings);
  }

  @Override
  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null; // null is default
  }

  private static class ProtoFormattingModel implements FormattingModel {

    private FormattingModel myModel;

    public ProtoFormattingModel(PsiElement element, CodeStyleSettings settings) {
      myModel = FormattingModelProvider.createFormattingModelForPsiFile(
          element.getContainingFile(),
          new ProtoBlock(element.getNode(), null, null, null, settings),
          settings);
    }

    @NotNull
    @Override
    public Block getRootBlock() {
      return myModel.getRootBlock();
    }

    @NotNull
    @Override
    public FormattingDocumentModel getDocumentModel() {
      return myModel.getDocumentModel();
    }

    @Override
    public TextRange replaceWhiteSpace(TextRange textRange, String whiteSpace) {
      return myModel.replaceWhiteSpace(textRange, whiteSpace);
    }

    @Override
    public TextRange shiftIndentInsideRange(TextRange range, int indent) {
      return myModel.shiftIndentInsideRange(range, indent);
    }

    @Override
    public void commitChanges() {
      myModel.commitChanges();
    }
  }
}
