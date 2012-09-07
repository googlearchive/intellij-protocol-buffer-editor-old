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

import com.intellij.lang.ASTNode;
import com.intellij.util.Icons;

import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * ProtoElement that encapsulates the user defined type in field declaration. 
 */
public class ProtoUserDefinedProperty extends ProtoProperty {

  public ProtoUserDefinedProperty(ASTNode astNode) {
    super(astNode);
  }

  @Nullable public ProtoSimplePropertyType getTypeElement() {
    return ProtoPsiTools.findDirectChildOfType(this, ProtoSimplePropertyType.class);
  }

  @Nullable public ProtoDefaultValue getDefaultValue() {
    return ProtoPsiTools.findDirectChildOfType(this, ProtoDefaultValue.class);
  }

  public Icon getIcon(int flags) {
    return Icons.FIELD_ICON;
  }
}
