// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.protoeditor.parsing;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.List;
import java.util.Set;

/**
 * Extension point for providing acceptable values of proto options.
 */
public interface ProtoOptionProvider {

  ExtensionPointName<ProtoOptionProvider> EP_NAME
      = ExtensionPointName.create("com.google.protoeditor.protoOptionProvider");

  public List<String> getOptionsFor(String tokenType);

  public Set<String> getAllOptions();

  /**
   * This returns true if an option in a proto file can have multiple values. In that case, the
   * values will be separated by ','.
   *
   * @param tokenType
   * @return true if can have multiple values, false otherwise.
   */
  public boolean hasMultipleOptions(String tokenType);
}
