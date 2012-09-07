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

import com.google.protoeditor.ProtoFileType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;

public class ProtoChangeTools {

  public static ASTNode createMessageFromText(Project project, String text) {
    ProtoMessageDefinition def = getDummyFile(project, text).getMessageDefinitions().get(0);
    return def.getNode();
  }

  public static ASTNode createLiteralFromText(Project project, String literal) {
    ProtoFile file = getDummyFile(project,
                                  "service dummy { option o = " + literal + "; }");
    ProtoServiceDefinition msg = file.getServiceDefinitions().get(0);
    ProtoOption option = msg.getOptions().get(0);
    ProtoLiteral literalEl = option.getValueLiteral();
    return literalEl.getNode();
  }

  private static ProtoFile getDummyFile(Project project, String text) {
    Language lang = ProtoFileType.instance().getLanguage();
    ParserDefinition def = LanguageParserDefinitions.INSTANCE.forLanguage(lang);
    assert def != null;

    return (ProtoFile) PsiFileFactory.getInstance(project).createFileFromText(
        "dummy." + ProtoFileType.instance().getDefaultExtension(), text);
  }

  public static ASTNode createIdentifierFromText(Project project,
                                                 String identifierText) {
    ProtoFile file = getDummyFile(project,
                                  "parsed message " + identifierText + " { }");
    return file.getMessageDefinitions().get(0).getNameElement().getNode();
  }
}
