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

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiImportList;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProtoFile extends PsiFileBase implements ProtoElement {

  public ProtoFile(FileViewProvider fileViewProvider) {
    super(fileViewProvider, ProtoFileType.instance().getLanguage());
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return ProtoFileType.instance();
  }

  public ProtoPackageStatement getPackageStatement() {
    return ProtoPsiTools.findDirectChildOfType(this, ProtoPackageStatement.class);
  }

  public List<ProtoPackageStatement> getPackageStatements() {
    return ProtoPsiTools.findDirectChildrenOfType(this, ProtoPackageStatement.class);
  }

  public ProtoSyntaxStatement getSyntaxStatement() {
      return ProtoPsiTools.findDirectChildOfType(this, ProtoSyntaxStatement.class);
  }

  public List<ProtoImportStatement> getImportStatements() {
    return ProtoPsiTools.findDirectChildrenOfType(this, ProtoImportStatement.class);
  }

  public List<ProtoFileOptionStatement> getFileOptionStatements() {
    return ProtoPsiTools.findDirectChildrenOfType(this, ProtoFileOptionStatement.class);
  }

  public List<ProtoMessageDefinition> getMessageDefinitions() {
    return ProtoPsiTools.findDirectChildrenOfType(this, ProtoMessageDefinition.class);
  }

  public List<ProtoServiceDefinition> getServiceDefinitions() {
    return ProtoPsiTools.findDirectChildrenOfType(this, ProtoServiceDefinition.class);
  }

  public List<ProtoToplevelDefinition> getTopLevelDefinitions() {
    return ProtoPsiTools.findDirectChildrenOfType(this, ProtoToplevelDefinition.class);
  }

  public ProtoMessageDefinition getMessageDefinition(String referencedName) {
    for (ProtoMessageDefinition def : getMessageDefinitions()) {
      String name = def.getName();
      if (name == null) {
        continue;
      }
      if (name.equals(referencedName)) {
        return def;
      }
    }
    return null;
  }



  public List<PsiImportList> getImports() {
    List<PsiImportList> list = new ArrayList<PsiImportList>();
    for (ProtoJavaLiteral literal : ProtoPsiTools
        .findDirectChildrenOfType(this, ProtoJavaLiteral.class)) {
      list.addAll(ProtoPsiTools.findDirectChildrenOfType(literal, PsiImportList.class));
    }
    return list;
  }

  public List<PsiErrorElement> getErrorElements() {
    return ProtoPsiTools.findDirectChildrenOfType(this, PsiErrorElement.class);
  }
}
