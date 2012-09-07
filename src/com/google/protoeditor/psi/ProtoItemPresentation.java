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

import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Collection;
import java.util.Collections;

class ProtoItemPresentation extends PsiTreeElementBase {

  private final ProtoElementWithName protoElement;

  public ProtoItemPresentation(ProtoElementWithName protoElement) {
    super(protoElement);
    this.protoElement = protoElement;
  }

  public Collection getChildrenBase() {
    return Collections.EMPTY_LIST;
  }

  public String getLocationString() {
    StringBuffer buf = new StringBuffer(20);
    if (!(protoElement instanceof ProtoToplevelDefinition)) {
      ProtoElement owner = getParentDefBodyOwner(protoElement);
      if (owner != null) {
        buf.append(owner.getName());
        ProtoElement parent = getParentDefBodyOwner(owner);
        while (parent != null) {
          buf.insert(0, ".");
          buf.insert(0, parent.getName());
          parent = getParentDefBodyOwner(parent);
        }
        buf.insert(0, "in ");
      }
    }
    boolean hadSomething = buf.length() != 0;
    buf.insert(0, "(");
    if (hadSomething) {
      buf.append(" in ");
    }
    buf.append(protoElement.getContainingFile().getName());
    buf.append(")");
    return buf.toString();
  }

  private ProtoElement getParentDefBodyOwner(ProtoElement el) {
    return PsiTreeUtil.getParentOfType(el, ProtoDefinitionBodyOwner.class,
                                       ProtoEnumDefinition.class);
  }

  public String getPresentableText() {
    String name = protoElement.getName();
    return name == null ? "<unknown>" : name;
  }
}
