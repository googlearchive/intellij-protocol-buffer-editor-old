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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ProtoPsiTools {

  /**
   * Checks if element belongs to a proto file with proto2 syntax.
   *
   * @return {@code true} if proto syntax definition refering to proto2 is found
   */
  public static boolean isProto2Syntax(@Nullable ProtoElement element) {
    if (element == null) {
      return false;
    }
    PsiFile psiFile = element.getContainingFile();
    if (!(psiFile instanceof ProtoFile)) {
      return false;
    }
    ProtoFile protoFile = (ProtoFile) psiFile;
    try {
      return "\"proto2\"".equals(protoFile.getSyntaxStatement().getProtoSyntaxValue().getText());
    } catch (NullPointerException npe) {
      // OK - syntax statement can be missing
      return false;
    }
  }

  public static <E extends PsiElement> List<E> findDirectChildrenOfType(
      PsiElement el, Class<E> cls) {
    List<E> list = new ArrayList<E>();
    for (PsiElement element : el.getChildren()) {
      if (cls.isInstance(element)) {
        E protoProperty = cls.cast(element);
        list.add(protoProperty);
      }
    }
    return list;
  }

  public static <E extends PsiElement> E findDirectChildOfType(
      PsiElement element, Class<E> cls) {
    List<E> children = findDirectChildrenOfType(element, cls);
    return children.isEmpty() ? null : children.get(0);
  }

  public static String getContainingPackage(ProtoFile protoFile) {
    ProtoPackageStatement pkgStmt = protoFile.getPackageStatement();
    final String ourPkgName;
    if (pkgStmt != null) {
      ourPkgName = pkgStmt.getPackageNameReference()
          .getReferencedPackageName();
    } else {
      ourPkgName = null;
    }
    return ourPkgName;
  }

  public static boolean isSuperclass(PsiClass cls, String fqn) {
    PsiClass parent = cls;
    while (parent != null) {
      String qn = parent.getQualifiedName();
      if (qn != null && qn.equals(fqn)) {
        return true;
      }
      parent = parent.getSuperClass();
    }
    return false;
  }
}
