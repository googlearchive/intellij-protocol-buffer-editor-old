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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.protoeditor.psi.ProtoElement;
import com.google.protoeditor.psi.ProtoFile;

import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;

import java.util.Collection;
import java.util.List;

/**
 * Provides utility methods for looking up sumbols in proto index.
 */
public class ProtoSymbolIndexLookup {

  /**
   * Returns proto symbol names of the given symbol type in the given project.
   *
   * @param symbolType ProtoSymbolType, type of the desired symbols
   * @param project Intellij Project
   * @return Array of proto symbol names that match the criteria.
   */
  public String[] getProtoNames(ProtoSymbolIndex.ProtoSymbolType symbolType, Project project) {
    List<String> topLevelNames = Lists.newArrayList();
    FileBasedIndex index = FileBasedIndex.getInstance();
    Collection<ProtoSymbolIndex.ProtoIndexKey> allKeys = index.getAllKeys(
        ProtoSymbolIndex.NAME, project);
    for (ProtoSymbolIndex.ProtoIndexKey key : allKeys) {
      if (key.getType() == symbolType) {
         topLevelNames.add(key.getSymbolName());
      }
    }
    return topLevelNames.toArray(new String[] {});
  }

  /**
   * Returns navigation elements for the given proto symbol.
   *
   * @param name Name of the desired symbol.
   * @param project Intellij Project
   * @param includeNonProjectItems Whether to have global scope of project scope.
   * @param symbolType ProtoSymbolType, type of the desired symbols
   * @return Array of proto symbol elements that match the criteria.
   */
  public NavigationItem[] getProtoItemsByName(String name, Project project,
      boolean includeNonProjectItems, final ProtoSymbolIndex.ProtoSymbolType symbolType) {
    return getProtoItemsByName(name, project, includeNonProjectItems, symbolType, null);
  }

  @VisibleForTesting
  NavigationItem[] getProtoItemsByName(String name, Project project,
      boolean includeNonProjectItems, final ProtoSymbolIndex.ProtoSymbolType symbolType,
      final VirtualFile vFile) {
    final List<NavigationItem> topLevelElements = Lists.newArrayList();
    final GlobalSearchScope scope = includeNonProjectItems ? GlobalSearchScope.allScope(project)
        : GlobalSearchScope.projectScope(project);
    final PsiManager psiManager = PsiManager.getInstance(project);

    FileBasedIndex.ValueProcessor<ProtoSymbolIndex.ProtoIndexValue> indexEntryValueProcessor =
        new FileBasedIndex.ValueProcessor<ProtoSymbolIndex.ProtoIndexValue>() {
          @Override
          public boolean process(VirtualFile file, ProtoSymbolIndex.ProtoIndexValue value) {
            if (!scope.contains(file) && vFile != file) {
              return true;
            }
            final PsiFile psiFile = psiManager.findFile(file);
            if (!(psiFile instanceof ProtoFile)) {
              return true;
            }

            PsiElement element = psiFile;
            for (Integer path : value.getPathFromRoot()) {
              element = element.getChildren()[path];
            }
            topLevelElements.add((ProtoElement) element);
            return true;
          }
        };

    FileBasedIndex index = FileBasedIndex.getInstance();
    Collection<ProtoSymbolIndex.ProtoIndexKey> allKeys = index.getAllKeys(
        ProtoSymbolIndex.NAME, project);
    for (ProtoSymbolIndex.ProtoIndexKey key : allKeys) {
      if (key.getType() == symbolType && key.getSymbolName().equals(name)) {
        FileBasedIndex.getInstance().processValues(ProtoSymbolIndex.NAME, key, vFile,
            indexEntryValueProcessor, scope);
      }
    }
    return topLevelElements.toArray(new NavigationItem[] {});
  }

}
