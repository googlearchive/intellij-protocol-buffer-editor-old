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

package com.google.protoeditor.structureview;

import com.google.protoeditor.psi.ProtoDefinitionBody;
import com.google.protoeditor.psi.ProtoDefinitionBodyOwner;
import com.google.protoeditor.psi.ProtoElement;
import com.google.protoeditor.psi.ProtoElementWithName;
import com.google.protoeditor.psi.ProtoEnumDefinition;
import com.google.protoeditor.psi.ProtoFile;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoServiceDefinition;

import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProtoStructureViewTreeElement extends PsiTreeElementBase {

  public ProtoStructureViewTreeElement(ProtoElement psiElement) {
    super(psiElement);
  }

  public Collection getChildrenBase() {
    List<ProtoElement> els = new ArrayList<ProtoElement>();
    PsiElement element = getElement();
    if (element instanceof ProtoFile) {
      ProtoFile protoFile = (ProtoFile) element;
      els.addAll(protoFile.getTopLevelDefinitions());

    } else if (element instanceof ProtoDefinitionBodyOwner) {
      ProtoDefinitionBodyOwner bodyOwner = (ProtoDefinitionBodyOwner) element;
      ProtoDefinitionBody body = bodyOwner.getDefinitionBody();
      if (body != null) {
        for (PsiElement child : body.getChildren()) {
          if (child instanceof ProtoEnumDefinition
              || child instanceof ProtoProperty) {
            els.add((ProtoElement) child);
          }
        }
      }
    } else if (element instanceof ProtoServiceDefinition) {
      ProtoServiceDefinition serviceDefinition = (ProtoServiceDefinition) element;
      els.addAll(serviceDefinition.getRpcDefinitions());

    } else if (element instanceof ProtoEnumDefinition) {
      ProtoEnumDefinition protoEnumDefinition = (ProtoEnumDefinition) element;
      els.addAll(protoEnumDefinition.getConstants());
    }
    List<ProtoStructureViewTreeElement> list = new ArrayList<ProtoStructureViewTreeElement>();
    for (ProtoElement el : els) {
      list.add(new ProtoStructureViewTreeElement(el));
    }
    return list;
  }

  public String getPresentableText() {
    PsiElement el = getElement();
    if (el instanceof ProtoElementWithName) {
      ProtoElementWithName protoNamedElement = (ProtoElementWithName) el;

      return protoNamedElement.getName();
    } else if (el instanceof PsiFile) {
      return ((PsiFile) el).getName();
    }
    return "<unknown>";
  }
}
