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

import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoServiceDefinition;
import com.google.protoeditor.psi.ProtoTestCase;

import java.util.List;

/**
 * Verifies that we can parse Enums in protobuffer files.
 */
public class ServiceParserTest extends ProtoTestCase {

  public void testParseOneSimpleService() throws Exception {
    writeTestProto(
        "package foo;",
        "service Foo {",
        "  rpc rpcFoo (param) returns (out) {",
        "    Bar2 = 2;",
        "  }",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertEquals(foo.getName(), "Foo");
    assertNoError(foo);
    List<ProtoRpcDefinition> rpcdefs = foo.getRpcDefinitions();
    assertEquals(rpcdefs.get(0).getName(), "rpcFoo");
    assertNoError(rpcdefs.get(0));
  }

  public void testParseServiceWithoutEndingBrace() throws Exception {
    writeTestProto(
        "package foo;",
        "service Foo {",
        "  rpc rpcFoo (param) returns (out) {",
        "    Bar2 = 2;",
        "  }",
        ""
    );
    assertPackageName(protoFile, "foo");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertEquals(foo.getName(), "Foo");
    assertError(foo.getServiceBody(), "expected '}'");
  }

  public void testParseServiceWithoutLeftBrace() throws Exception {
    writeTestProto(
        "package foo;",
        "service Foo ",
        "  rpc rpcFoo (param) returns (out) {",
        "    Bar2 = 2;",
        "  }",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertEquals(foo.getName(), "Foo");
    assertError(foo, "expected '{', but got 'rpc'");
    List<ProtoRpcDefinition> rpcdefs = foo.getRpcDefinitions();
    assertEquals(rpcdefs.get(0).getName(), "rpcFoo");
    assertNoError(rpcdefs.get(0));
  }

    public void testParseServiceWithoutName() throws Exception {
    writeTestProto(
        "package foo;",
        "service  {",
        "  rpc rpcFoo (param) returns (out) {",
        "    Bar2 = 2;",
        "  }",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertError(foo, "expected service name");
    List<ProtoRpcDefinition> rpcdefs = foo.getRpcDefinitions();
    assertEquals(rpcdefs.get(0).getName(), "rpcFoo");
    assertNoError(rpcdefs.get(0));
  }

  public void testParseServiceWithoutEqualTo() throws Exception {
    writeTestProto(
        "package foo;",
        "service Foo {",
        "  rpc rpcFoo (param) returns (out) {",
        "    Bar1 1;",
        "    Bar2 = 2;",
        "  }",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertEquals(foo.getName(), "Foo");
    assertNoError(foo);
    List<ProtoRpcDefinition> rpcdefs = foo.getRpcDefinitions();
    assertEquals(rpcdefs.get(0).getName(), "rpcFoo");
    assertNoError(rpcdefs.get(0));
  }

  public void testParseServiceWithoutSemiColon() throws Exception {
    writeTestProto(
        "package foo;",
        "service Foo {",
        "  rpc rpcFoo (param) returns (out) {",
        "    Bar1 = 1",
        "    Bar2 = 2;",
        "  }",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoServiceDefinition foo = getServiceDefinitions();
    assertEquals(foo.getName(), "Foo");
    assertNoError(foo);
    List<ProtoRpcDefinition> rpcdefs = foo.getRpcDefinitions();
    assertEquals(rpcdefs.get(0).getName(), "rpcFoo");
    assertNoError(rpcdefs.get(0));
  }
}
