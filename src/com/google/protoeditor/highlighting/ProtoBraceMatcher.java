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

package com.google.protoeditor.highlighting;

import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProtoBraceMatcher implements PairedBraceMatcher {

  private static final BracePair[] BRACE_PAIRS = new BracePair[]{
      new BracePair(ProtoTokenTypes.LBRACKET, ProtoTokenTypes.RBRACKET, false),
      new BracePair(ProtoTokenTypes.LBRACE, ProtoTokenTypes.RBRACE, true),
      new BracePair(ProtoTokenTypes.LPAR, ProtoTokenTypes.RPAR, false),
      new BracePair(ProtoTokenTypes.LT, ProtoTokenTypes.GT, false),
  };

  public BracePair[] getPairs() {
    return BRACE_PAIRS;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    // TODO: add better impl to find matching token before brace
    return openingBraceOffset;
  }

  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType,
                                                 @Nullable IElementType contextType) {
    return true; // docs say always returning true is okay
  }
}
