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

import static com.google.protoeditor.psi.ProtoPrimitive.DOUBLE;
import static com.google.protoeditor.psi.ProtoPrimitive.FIXED32;
import static com.google.protoeditor.psi.ProtoPrimitive.FIXED64;
import static com.google.protoeditor.psi.ProtoPrimitive.FLOAT;
import static com.google.protoeditor.psi.ProtoPrimitive.INT32;
import static com.google.protoeditor.psi.ProtoPrimitive.INT64;
import static com.google.protoeditor.psi.ProtoPrimitive.SFIXED32;
import static com.google.protoeditor.psi.ProtoPrimitive.SFIXED64;
import static com.google.protoeditor.psi.ProtoPrimitive.SINT32;
import static com.google.protoeditor.psi.ProtoPrimitive.SINT64;
import static com.google.protoeditor.psi.ProtoPrimitive.UINT32;
import static com.google.protoeditor.psi.ProtoPrimitive.UINT64;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ProtoPrimitiveType extends ProtoType {

  private ProtoPrimitive type;

  public ProtoPrimitiveType(ProtoPrimitive type) {
    assert type != null;
    this.type = type;
  }

  public ProtoPrimitive getPrimitiveType() {
    return type;
  }

  @Override
  public String getDisplayName() {
    return type.getText();
  }

  private static Map<ProtoPrimitive, Collection<ProtoPrimitive>> assignableFrom
      = new HashMap<ProtoPrimitive, Collection<ProtoPrimitive>>();

  static {
    // TODO: more complete matrix of assignableFrom
    assignableFrom.put(DOUBLE, new HashSet<ProtoPrimitive>(
        Arrays.asList(FIXED32, FIXED64, FLOAT, INT32, INT64,
            SFIXED32, SFIXED64, SINT32, SINT64, UINT32, UINT64)));
    assignableFrom.put(FLOAT, new HashSet<ProtoPrimitive>(
        Arrays.asList(FIXED32, FIXED64, INT32, INT64,
            SFIXED32, SFIXED64, SINT32, SINT64, UINT32, UINT64)));
    assignableFrom.put(FIXED32, new HashSet<ProtoPrimitive>(
        Arrays.asList(INT32)));
    assignableFrom.put(INT32, new HashSet<ProtoPrimitive>(
        Arrays.asList(FIXED32)));
    assignableFrom.put(INT64, new HashSet<ProtoPrimitive>(
        Arrays.asList(FIXED32, INT32, FIXED64)));
    assignableFrom.put(FIXED64, new HashSet<ProtoPrimitive>(
        Arrays.asList(FIXED32, INT32, INT64)));
    assignableFrom.put(UINT64, new HashSet<ProtoPrimitive>(
        Arrays.asList(FIXED32, INT32)));
  }

  @Override
  public boolean isAssignableFrom(ProtoType valType) {
    if (!(valType instanceof ProtoPrimitiveType)) {
      return false;
    }

    ProtoPrimitiveType protoPrimitiveType = (ProtoPrimitiveType) valType;
    ProtoPrimitive otherType = protoPrimitiveType.getPrimitiveType();
    ProtoPrimitive ourType = getPrimitiveType();
    if (otherType.equals(ourType)) {
      return true;
    }
    Collection<ProtoPrimitive> assignableFrom = ProtoPrimitiveType.assignableFrom
        .get(ourType);
    if (assignableFrom == null) {
      return false;
    }
    return assignableFrom.contains(otherType);
  }

  @Override
  public String getIdentifierText() {
    return getDisplayName();
  }
}
