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

import javax.swing.*;

public class ProtoGroupDefinition extends ProtoProperty implements ProtoDefinitionBodyOwner {

  public ProtoGroupDefinition(ASTNode astNode) {
    super(astNode);
  }

  public ProtoDefinitionBody getDefinitionBody() {
    return ProtoPsiTools.findDirectChildOfType(this, ProtoDefinitionBody.class);
  }

  public Icon getIcon(int flags) {
    return ProtoeditorIcon.PROTOEDITOR_ICON;
  }
}
