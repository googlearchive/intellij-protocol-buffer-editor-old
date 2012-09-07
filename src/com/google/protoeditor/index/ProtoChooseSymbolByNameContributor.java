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

package com.google.protoeditor.index;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;

/**
 * Provides "Go to symbol" functionality for proto files.
 */
public class ProtoChooseSymbolByNameContributor implements ChooseByNameContributor {

  @Override
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    return new ProtoSymbolIndexLookup().getProtoNames(
        ProtoSymbolIndex.ProtoSymbolType.NAMED_ELEMENT, project);
  }

  @Override
  public NavigationItem[] getItemsByName(String name, String pattern, Project project,
      boolean includeNonProjectItems) {
    return new ProtoSymbolIndexLookup().getProtoItemsByName(name, project, includeNonProjectItems,
        ProtoSymbolIndex.ProtoSymbolType.NAMED_ELEMENT);
  }
}
