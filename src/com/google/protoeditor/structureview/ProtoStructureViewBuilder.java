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

import com.google.protoeditor.psi.ProtoElement;
import com.google.protoeditor.psi.ProtoEnumConstant;
import com.google.protoeditor.psi.ProtoEnumProperty;
import com.google.protoeditor.psi.ProtoGroupDefinition;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoMessageProperty;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoServiceDefinition;
import com.google.protoeditor.psi.ProtoSimpleProperty;
import com.google.protoeditor.psi.ProtoSyntaxStatement;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Grouper;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Icons;
import org.jetbrains.annotations.NotNull;

public class ProtoStructureViewBuilder extends TreeBasedStructureViewBuilder {

  private final PsiFile psiFile;

  public ProtoStructureViewBuilder(PsiFile psiFile) {
    this.psiFile = psiFile;
  }

  @Override
  public StructureViewModel createStructureViewModel() {
    return new ProtoStructureViewModel();
  }

  private class ProtoStructureViewModel extends TextEditorBasedStructureViewModel {

    public ProtoStructureViewModel() {
      super(ProtoStructureViewBuilder.this.psiFile);
    }

    @Override
    protected PsiFile getPsiFile() {
      return psiFile;
    }

    @NotNull
    @Override
    protected Class[] getSuitableClasses() {
      return new Class[]{
          ProtoMessageDefinition.class,
          ProtoGroupDefinition.class,
          ProtoProperty.class,
          ProtoServiceDefinition.class,
          ProtoRpcDefinition.class
      };
    }

    @NotNull
    @Override
    public StructureViewTreeElement getRoot() {
      return new ProtoStructureViewTreeElement((ProtoElement) psiFile);
    }

    @NotNull
    @Override
    public Grouper[] getGroupers() {
      return new Grouper[0];
    }

    @NotNull
    @Override
    public Sorter[] getSorters() {
      return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @NotNull
    @Override
    public Filter[] getFilters() {
      return new Filter[]{new Filter() {
        @Override
        public boolean isVisible(TreeElement treeNode) {
          if (treeNode instanceof ProtoStructureViewTreeElement) {
            ProtoStructureViewTreeElement protoEl
                = (ProtoStructureViewTreeElement) treeNode;
            PsiElement element = protoEl.getElement();
            if (element instanceof ProtoSimpleProperty
                || element instanceof ProtoEnumProperty
                || element instanceof ProtoMessageProperty
                || element instanceof ProtoEnumConstant
                || element instanceof ProtoSyntaxStatement) {
              return false;
            }
          }
          return true;
        }

        @Override
        public boolean isReverted() {
          return true;
        }

        @NotNull
        @Override
        public ActionPresentation getPresentation() {
          return new ActionPresentation() {
            @Override
            public String getText() {
              return "Show Properties";
            }

            @Override
            public String getDescription() {
              return "Show message properties.";
            }

            @Override
            public javax.swing.Icon getIcon() {
              return Icons.FIELD_ICON;
            }
          };
        }

        @NotNull
        @Override
        public String getName() {
          return "SHOW_PROPERTIES";
        }
      }};
    }
  }
}
