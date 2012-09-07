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

public class ProtoPsiToolsTest extends ProtoTestCase {

  public void testIsProto2Syntax() throws Exception {
    assertFalse(ProtoPsiTools.isProto2Syntax(null));

    writeTestProto("syntax = \"proto2\"");
    assertTrue(ProtoPsiTools.isProto2Syntax(protoFile));
  }

  public void testIsProto2Syntax2() throws Exception {
    writeTestProto("syntax = \"proto\"");
    assertFalse(ProtoPsiTools.isProto2Syntax(protoFile));
  }

  public void testIsProto2Syntax3() throws Exception {
    writeTestProto("just some not parseable mess");
    assertFalse(ProtoPsiTools.isProto2Syntax(protoFile));
  }
}
