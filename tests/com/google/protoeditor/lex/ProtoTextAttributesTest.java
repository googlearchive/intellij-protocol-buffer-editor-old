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

package com.google.protoeditor.lex;

import com.intellij.codeInsight.daemon.impl.JavaHighlightInfoTypes;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.testFramework.PlatformTestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import java.awt.Color;
import java.awt.Font;

public class ProtoTextAttributesTest extends PlatformTestCase {

  public void testEditorColorSchemeChange() {

    IMocksControl ctrl = EasyMock.createNiceControl();

    EditorColorsManager manager = EditorColorsManager.getInstance();

    EditorColorsScheme scheme1 = ctrl.createMock(EditorColorsScheme.class);
    TextAttributes textAttributes1 = new TextAttributes(
        Color.BLACK, Color.WHITE, Color.BLUE, EffectType.BOLD_DOTTED_LINE, Font.PLAIN);
    EasyMock.expect(scheme1.getName()).andReturn("Scheme1").anyTimes();
    EasyMock.expect(scheme1.getAttributes(
        JavaHighlightInfoTypes.JAVA_KEYWORD.getAttributesKey()))
        .andReturn(textAttributes1).anyTimes();
    EditorColorsScheme scheme2 = ctrl.createMock(EditorColorsScheme.class);
    TextAttributes textAttributes2 = new TextAttributes(
        Color.BLACK, Color.WHITE, Color.BLUE, EffectType.BOLD_DOTTED_LINE, Font.BOLD);
    EasyMock.expect(scheme2.getName()).andReturn("Scheme2").anyTimes();
    EasyMock.expect(scheme2.getAttributes(
        JavaHighlightInfoTypes.JAVA_KEYWORD.getAttributesKey()))
        .andReturn(textAttributes2).anyTimes();

    ctrl.replay();
    manager.addColorsScheme(scheme1);
    manager.addColorsScheme(scheme2);
    manager.setGlobalScheme(scheme1);
    ProtoTextAttributes.initialize();
    assertEquals(ProtoTextAttributes.ATTR_DECLARATION.getDefaultAttributes(), textAttributes1);

    // Verify that the listener is invoked, which should set the ProtoTextAttributes color scheme
    // to scheme2 now.
    manager.setGlobalScheme(scheme2);
    assertEquals(ProtoTextAttributes.ATTR_DECLARATION.getDefaultAttributes(), textAttributes2);
    ctrl.verify();
  }
}
