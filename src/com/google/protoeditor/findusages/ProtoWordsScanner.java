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

import com.google.protoeditor.lex.ProtoLexer;
import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;

public class ProtoWordsScanner extends DefaultWordsScanner {

  public ProtoWordsScanner() {
    super(new ProtoLexer(), TokenSet.create(ProtoTokenTypes.IDENTIFIER),
          TokenSet.create(ProtoTokenTypes.C_STYLE_COMMENT,
                          ProtoTokenTypes.END_OF_LINE_COMMENT),
          TokenSet.create(ProtoTokenTypes.IDENTIFIER));
  }
}
