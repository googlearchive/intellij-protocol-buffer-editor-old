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

package com.google.protoeditor;

import com.intellij.openapi.fileTypes.LanguageFileType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProtoFileType extends LanguageFileType {

  private static final ProtoFileType INSTANCE = new ProtoFileType();

  public static ProtoFileType instance() {
    return INSTANCE;
  }

  private ProtoFileType() {
    super(new ProtocolBufferLanguage());
  }

  @NotNull
  public String getName() {
    return "Protocol Buffer";
  }

  @NotNull
  public String getDescription() {
    return "Protocol Buffer";
  }

  @NotNull
  public String getDefaultExtension() {
    return "proto";
  }

  @Nullable
  public Icon getIcon() {
    return new ImageIcon(getClass().getResource("proto-file.png"));
  }
}
