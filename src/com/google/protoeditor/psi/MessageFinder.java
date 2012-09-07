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

import com.google.protoeditor.ProtoFileType;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

import java.util.Collection;

public abstract class MessageFinder implements ContentIterator {

  private final FileDocumentManager docMgr;
  private final PsiDocumentManager psiMgr;
  private Collection<String> pkgNames;
  private Collection<String> clsNames;

  protected MessageFinder(PsiElement ref, Collection<String> pkgNames,
                          Collection<String> clsNames) {
    this.docMgr = FileDocumentManager.getInstance();
    this.psiMgr = PsiDocumentManager.getInstance(ref.getProject());
    this.pkgNames = pkgNames;
    this.clsNames = clsNames;
  }

  public boolean processFile(VirtualFile fileOrDir) {
    if (!(fileOrDir.getFileType() instanceof ProtoFileType)) {
      return true;
    }
    Document doc = docMgr.getDocument(fileOrDir);
    if (doc == null) {
      return true;
    }
    PsiFile psiFile = psiMgr.getPsiFile(doc);
    if (!(psiFile instanceof ProtoFile)) {
      return true;
    }
    ProtoFile protoFile = (ProtoFile) psiFile;
    ProtoPackageStatement pkgStmt = protoFile.getPackageStatement();
    if (pkgStmt == null) {
      return true;
    }
    ProtoPackageNameReference pkgRef = pkgStmt.getPackageNameReference();
    if (pkgRef == null) {
      return true;
    }
    String referencedPackageName = pkgRef.getReferencedPackageName();
    if (referencedPackageName != null) {
      if (pkgNames.contains(referencedPackageName)) {
        for (ProtoMessageDefinition def : protoFile
            .getMessageDefinitions()) {
          if (!handleMessage(def)) {
            return false;
          }
        }
        if (!handleFile(protoFile)) {
          return false;
        }
      } else {
        for (ProtoMessageDefinition def : protoFile
            .getMessageDefinitions()) {
          if (clsNames.contains(referencedPackageName + "." + def.getName())) {
            if (!handleMessage(def)) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  protected abstract boolean handleMessage(ProtoMessageDefinition def);

  protected abstract boolean handleFile(ProtoFile protoFile);
}
