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

package com.google.protoeditor.parsing;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of the {@link ProtoOptionProvider}
 */
public class ProtoOptionProviderImpl implements ProtoOptionProvider {

  private static final List<String> VALID_BOOLEAN_TYPES = ImmutableList.of("true", "false");
  private static final List<String> VALID_CTYPE_TYPES
      = ImmutableList.of("STRING", "CORD", "Cord", "STRING_PIECE", "proto2");

  private final Map<String, List<String>> optionValues;

  public ProtoOptionProviderImpl() {
    optionValues = new HashMap<String, List<String>>();
    optionValues.put("weak", VALID_BOOLEAN_TYPES);
    optionValues.put("deprecated", VALID_BOOLEAN_TYPES);
    optionValues.put("lazy", VALID_BOOLEAN_TYPES);
    optionValues.put("ctype", VALID_CTYPE_TYPES);
    optionValues.put("packed", VALID_BOOLEAN_TYPES);
  }

  @Override
  public List<String> getOptionsFor(String tokenType) {
    List<String> values = optionValues.get(tokenType);
    if (values == null) {
      return Collections.emptyList();
    }
    return values;
  }

  public Set<String> getAllOptions() {
    return optionValues.keySet();
  }

  @Override
  public boolean hasMultipleOptions(String tokenType) {
    return false;
  }
}
