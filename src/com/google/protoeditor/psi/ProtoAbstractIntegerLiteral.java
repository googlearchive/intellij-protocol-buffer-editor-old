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

import com.google.protoeditor.lex.ProtoTokenTypes;

import com.intellij.lang.ASTNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ProtoAbstractIntegerLiteral extends AbstractProtoElement
    implements ProtoLiteral {

  public ProtoAbstractIntegerLiteral(ASTNode astNode) {
    super(astNode);
  }

  public abstract boolean hasValidValue();

  public abstract long getIntValue() throws IllegalStateException;

  public Collection<? extends ProtoType> getPossibleTypes() {
    if (!hasValidValue()) {
      return Collections.EMPTY_LIST;
    }
    List<ProtoType> types = new ArrayList<ProtoType>();
    long val = getIntValue();
    types.add(new ProtoPrimitiveType(ProtoPrimitive.INT64));
    types.add(new ProtoPrimitiveType(ProtoPrimitive.FIXED64));
    types.add(new ProtoPrimitiveType(ProtoPrimitive.SINT64));
    types.add(new ProtoPrimitiveType(ProtoPrimitive.SFIXED64));
    types.add(new ProtoPrimitiveType(ProtoPrimitive.DOUBLE));
    types.add(new ProtoPrimitiveType(ProtoPrimitive.FLOAT));
    if (val >= 0) {
      types.add(new ProtoPrimitiveType(ProtoPrimitive.UINT64));
    }
    if ((long) (int) val == val) {
      types.add(new ProtoPrimitiveType(ProtoPrimitive.INT32));
      types.add(new ProtoPrimitiveType(ProtoPrimitive.SINT32));
      types.add(new ProtoPrimitiveType(ProtoPrimitive.FIXED32));
      types.add(new ProtoPrimitiveType(ProtoPrimitive.SFIXED32));
      if (val >= 0) {
        types.add(new ProtoPrimitiveType((ProtoPrimitive.UINT32)));
      }
    }
    return types;
  }

  public void setIntValue(long val) {
    ASTNode litNode = getNode().findChildByType(ProtoTokenTypes.INTEGER_LITERAL);
    litNode.getTreeParent().replaceChild(litNode,
                                         ProtoChangeTools.createLiteralFromText(getProject(),
                                                                                createStringValue(
                                                                                    val)));
  }

  protected abstract String createStringValue(long val);
}
