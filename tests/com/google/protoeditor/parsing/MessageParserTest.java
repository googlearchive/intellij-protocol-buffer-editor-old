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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protoeditor.psi.ProtoEnumDefinition;
import com.google.protoeditor.psi.ProtoExtensionsStatement;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoTestCase;

/**
 * Verifies that we can parse messages in protobuffer files.
 */
public class MessageParserTest extends ProtoTestCase {

  public void testParseOneSimpleMessagePropertyWithOptionalParsedKeyword() throws Exception {
    writeTestProto(
        "package foo;",
        "parsed message Foo {",
        "  required int64 Bar = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    assertNoError(foo);
    ProtoProperty prop = getOnlyProperty(foo);
    assertSimpleProperty(prop, "Bar", "int64");
    assertNoError(prop);
  }

  public void testParseParsedMessageWithError() throws Exception {
    writeTestProto(
        "package foo;",
        "parsed messag Foo {",
        "  int64 Bar = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, null);
    assertError(foo, "expected '" + ProtoKeywords.MESSAGE + "' or '"
                    + ProtoKeywords.CLASS + "' or ' keyword");
    assertNotNull(foo.getDefinitionBody());
  }

  public void testParseOneSimpleMessageProperty() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  required int64 Bar = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    assertNoError(foo);
    ProtoProperty prop = getOnlyProperty(foo);
    assertSimpleProperty(prop, "Bar", "int64");
    assertNoError(prop);
  }

  public void testParseOneSimpleMessagePropertyWithError() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  int64 Bar = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    assertError(foo.getDefinitionBody(), "Message body should contain a <modifier"
            + " fieldname>|<enum>|<message>|<extends>|<extensions>|<group>|<option>|\":\".");
  }

  public void testParseMessageWithEnums() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "  }\n",
        "  required Response answer = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");

    ProtoProperty prop = getOnlyProperty(foo);
    assertEnumProperty(prop, "answer", "Response");
    assertNoError(prop);
  }

  public void testParseMessageWithEnumsWithErrors() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "  }\n",
        "  required Response = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");

    assertError(foo.getDefinitionBody(), "expected property name");
  }

  public void testParseMessageWithEnumsAndDefaults() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "  }\n",
        "  optional Response answer = 1 [default = YES];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");

    ProtoProperty prop = getOnlyProperty(foo);
    assertEnumProperty(prop, "answer", "Response", "YES");
    assertNoError(prop);
  }

  public void testParseMessageWithEnumsAndDefaultsWithErrors() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "  }\n",
        "  optional Response answer = 1 [default = MAYBE];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertError(prop, "invalid default value");
  }

  public void testParseMessageWithIndependentEnumAndDefaults() throws Exception {
    writeTestProto(
        "package foo;",
        "enum Response {",
        "  YES = 0;",
        "  NO = 1;",
        "}",
        "message Foo {",
        "  optional Response answer = 1 [default = YES];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);
  }

  public void testParseMessageWithIndependentEnumAndDefaultsWithErrors() throws Exception {
    writeTestProto(
        "package foo;",
        "enum Response {",
        "  YES = 0;",
        "  NO = 1;",
        "}",
        "message Foo {",
        "  optional Response answer = 1 [default = MAYBE];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertError(prop, "invalid default value");
  }

  public void testParseMessageWithImportedEnumAndDefaults() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  optional Response answer = 1 [default = MAYBE];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);
  }

  public void testParseMessageWithRequiredFieldsAndDefaults() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "  }\n",
        "  required Response answer = 1 [default = NO];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");

    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);
  }

  public void testParseMessageWithRepeatedFieldsAndDefaults() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "  }\n",
        "  repeated Response answer = 1 [default = NO];",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertError(prop, "repeated fields can't have defaults.");
  }

  public void testParseEnum_WithDuplicateName() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    BAR = 0;",
        "    BAR = 1;",
        "  }\n",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    assertNotNull(foo);

    assertCheckEnum(foo, true, "multiple constants in Response have name BAR");
  }

  public void testParseEnum_WithDuplicateValue() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    APPLE = 1;",
        "    PEAR = 1;",
        "  }\n",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    assertNotNull(foo);

    assertCheckEnum(foo, false, "multiple constants in Response have value 1");
  }

  public void testParseEnums_WithDuplicateEnumName() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  enum Response {",
        "    PEAR = 1;",
        "  }\n",
        "  enum Response {",
        "    PEAR = 1;",
        "  }\n",
        "}"
    );

    assertPackageName(protoFile, "foo");
    ProtoMessageDefinition foo = getOnlyMessage(protoFile, "Foo");
    assertNotNull(foo);

    assertCheckEnum(foo, true, "multiple definitions of enum Response");
  }


  public void testParseEnumsWithinNestedMessage() throws Exception {
    writeTestProto(
        "message SearchResponse {",
        "  message Result {",
        "    enum Response {",
        "      PEAR = 1;",
        "    }",
        "    optional Response Bar = 1 [default = PEAR];",
        "  }",
        "  repeated Result result = 1;",
        "}");

    assertNotNull(getMessageByName(protoFile, "SearchResponse"));
    ProtoMessageDefinition def = getMessageByName(protoFile, "SearchResponse");
    assertNotNull(def);
    ProtoProperty prop = getOnlyProperty(def);
    assertNull(prop.getError());

    ProtoMessageDefinition result =
        Iterables.get(def.getDefinitionBody().getMessageDefinitions(), 0);
    assertNotNull(result);
    assertEquals("Result", result.getName());

    assertEquals(1, result.getDefinitionBody().getEnumerations().size());
    ProtoEnumDefinition enumDef = Iterables.get(result.getDefinitionBody().getEnumerations(), 0);
    assertNotNull(enumDef);
    assertEquals("Response", enumDef.getName());

    assertNoParseErrors(protoFile);
  }

  public void testParseMessageReferencingToOtherMessage() throws Exception {
    writeTestProto(
        "package foo;",
        "message Foo {",
        "  required string Bar = 1;",
        "}",
        "message Acme {",
        "  required message<Foo> foo = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    assertNotNull(getMessageByName(protoFile, "Foo"));
    ProtoMessageDefinition def = getMessageByName(protoFile, "Acme");
    assertNotNull(def);
    ProtoProperty prop = getOnlyProperty(def);
    assertNull(prop.getError());
  }

  public void testParseMessageReferencingToOtherMessageProto2() throws Exception {
    writeTestProto(
        "syntax = \"proto2\";",
        "package foo;",
        "message Foo {",
        "  required string Bar = 1;",
        "}",
        "message Acme {",
        "  required Foo foo = 1;",
        "}"
    );

    assertNoParseErrors(protoFile);
    assertPackageName(protoFile, "foo");
    assertNotNull(getMessageByName(protoFile, "Foo"));
    ProtoMessageDefinition def = getMessageByName(protoFile, "Acme");
    assertNotNull(def);
    ProtoProperty prop = getOnlyProperty(def);
    assertNull(prop.getError());
  }

  public void testParseNestedMessageProto2() throws Exception {
    // new feature in proto2
    writeTestProto(
        "syntax = \"proto2\";",
        "package foo;",
        "message SearchResponse {",
        "  message Result {",
        "    required string Bar = 1;",
        "  }",
        "  repeated Result result = 1;",
        "  message WrappedResult {",
        "    optional Result Foo = 1;",
        "  }",
        "}",
        "message SomeOtherMessage {",
        "  optional SearchResponse.Result result = 1;",
        "}"
    );

    assertPackageName(protoFile, "foo");
    assertNotNull(getMessageByName(protoFile, "SomeOtherMessage"));
    assertNotNull(getMessageByName(protoFile, "SearchResponse"));
    ProtoMessageDefinition def = getMessageByName(protoFile, "SearchResponse");
    assertNotNull(def);
    ProtoProperty prop = getOnlyProperty(def);
    assertNull(prop.getError());

    // check 'message Result'
    ProtoMessageDefinition result =
        Iterables.get(def.getDefinitionBody().getMessageDefinitions(), 0);
    assertNotNull(result);
    assertEquals("Result", result.getName());

    // check 'message WrappedResult'
    result = Iterables.get(def.getDefinitionBody().getMessageDefinitions(), 1);
    assertNotNull(result);
    assertEquals("WrappedResult", result.getName());
    prop = getOnlyProperty(result); // and its property is resolved
    assertNull(prop.getError());

    def = getMessageByName(protoFile, "SomeOtherMessage");
    assertNotNull(def);
    prop = getOnlyProperty(def);
    assertNull(prop.getError());
    assertNoParseErrors(protoFile);
  }

  // TODO: this is part of proto2 syntax that is not supported yet
  public void disabledTestCustomOptions() throws Exception {
    // example that uses every kind of option from
    // https://developers.google.com/protocol-buffers/docs/proto
    writeTestProto("import \"google/protobuf/descriptor.proto\";",
        "",
        "extend google.protobuf.FileOptions {",
        "  optional string my_file_option = 50000;",
        "}",
        "extend google.protobuf.MessageOptions {",
        "  optional int32 my_message_option = 50001;",
        "}",
        "extend google.protobuf.FieldOptions {",
        "  optional float my_field_option = 50002;",
        "}",
        "extend google.protobuf.EnumOptions {",
        "  optional bool my_enum_option = 50003;",
        "}",
        "extend google.protobuf.EnumValueOptions {",
        "  optional uint32 my_enum_value_option = 50004;",
        "}",
        "extend google.protobuf.ServiceOptions {",
        "  optional MyEnum my_service_option = 50005;",
        "}",
        "extend google.protobuf.MethodOptions {",
        "  optional MyMessage my_method_option = 50006;",
        "}",
        "",
        "option (my_file_option) = \"Hello world!\";",
        "",
        "message MyMessage {",
        "  option (my_message_option) = 1234;",
        "",
        "  optional int32 foo = 1 [(my_field_option) = 4.5];",
        "  optional string bar = 2;",
        "}",
        "",
        "enum MyEnum {",
        "  option (my_enum_option) = true;",
        "",
        "  FOO = 1 [(my_enum_value_option) = 321];",
        "  BAR = 2;",
        "}",
        "",
        "service MyService {",
        "  option (my_service_option) = FOO;",
        "",
        "  method MyMethod() {",
        "    // Note:  my_method_option has type MyMessage.  We can set each field",
        "    //   within it using a separate \"option\" line.",
        "    option (my_method_option).foo = 567;",
        "    option (my_method_option).bar = \"Some string\";",
        "  }",
        "}");
    assertNoParseErrors(protoFile);
  }

  public void testExtensionsDeclarationProto2() throws Exception {
    writeTestProto("message Foo {",
        "extensions 100 to 199;",
        "}");
    ProtoMessageDefinition message = getOnlyMessage(protoFile, "Foo");
    ProtoExtensionsStatement extensionsStatement = getOnlyExtensionsStatement(message);
    assertNotNull(extensionsStatement);
    assertNoError(extensionsStatement);
  }

  public void testExtensionsDeclarationWithMaxProto2() throws Exception {
    writeTestProto("message Foo {",
        "extensions 100 to max;",
        "}");
    ProtoMessageDefinition message = getOnlyMessage(protoFile, "Foo");
    ProtoExtensionsStatement extensionsStatement = getOnlyExtensionsStatement(message);
    assertNotNull(extensionsStatement);
    assertNoError(extensionsStatement);
  }

  public void testBadExtensionsDeclarationProto2NoTo() throws Exception {
    writeTestProto("message Foo {",
        "extensions 100 max;",
        "}");
    ProtoMessageDefinition message = getOnlyMessage(protoFile, "Foo");
    assertError(message.getDefinitionBody(), "expected 'TO'");
  }

  public void testBadExtensionsDeclarationProto2NoMax() throws Exception {
    writeTestProto("message Foo {",
        "extensions 100 to;",
        "}");
    ProtoMessageDefinition message = getOnlyMessage(protoFile, "Foo");
    assertError(message.getDefinitionBody(), "expected integer, upper bound for extensions");
  }

  public void testBadExtensionsDeclarationProto2MaxLower() throws Exception {
    writeTestProto("message Foo {",
        "extensions max to 100;",
        "}");
    ProtoMessageDefinition message = getOnlyMessage(protoFile, "Foo");
    assertError(message.getDefinitionBody(), "expected integer, lower bound for extensions");
  }

  public void testExtensionsDeclarationProto2WithoutSemicolon() throws Exception {
    writeTestProto("message Foo {",
        "extensions 100 to 199",
        "}");
    ProtoMessageDefinition message = getOnlyMessage(protoFile, "Foo");
    ProtoExtensionsStatement extensionsStatement = getOnlyExtensionsStatement(message);
    assertNotNull(extensionsStatement);
    assertError(extensionsStatement, "expected ';', but got '}'");
  }

  public void testSimpleExtendProto2() throws Exception {
    writeTestProto("message MyExt {",
        "optional int32 bar = 1;",
        "optional string baz = 2;",
        "optional float qux = 3;",
        "}",
        "extend Foo {",
        "  optional MyExt my_ext = 123;",
        "}");
     assertNoParseErrors(protoFile);
  }
  
  public void testNestedExtendsProto2() throws Exception {
    writeTestProto("message Baz {",
        "extend Foo {",
        "optional int32 bar = 126;",
        "}",
        "}");
    assertNoParseErrors(protoFile);
  }

  public void testDefaultFieldOption() throws Exception {
    writeTestProto(
        "message Foo {",
        "  optional int64 Bar = 1 [default=\"bar\"];",
        "}",
        "message Foo2 {",
        "  required int64 Bar = 1 [default=\"bar\"];",
        "}",
        "message Foo3 {",
        "  optional int64 Bar = 1 [default=];",
        "}");

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);

    ProtoMessageDefinition foo2 = getMessageByName(protoFile, "Foo2");
    ProtoProperty prop2 = getOnlyProperty(foo2);
    assertNoError(prop2);

    ProtoMessageDefinition foo3 = getMessageByName(protoFile, "Foo3");
    ProtoProperty prop3 = getOnlyProperty(foo3);
    assertError(prop3, "expected default value");
  }

  public void testDeprecatedFieldOption() throws Exception {
    writeTestProto(
        "message Foo {",
        "  optional int64 Bar = 1 [deprecated=true];",
        "}",
        "message Foo2 {",
        "  required int64 Bar = 1 [deprecated];",
        "}",
        "message Foo3 {",
        "  optional int64 Bar = 1 [deprecated=1];",
        "}");

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);

    ProtoMessageDefinition foo2 = getMessageByName(protoFile, "Foo2");
    ProtoProperty prop2 = getOnlyProperty(foo2);
    assertError(prop2, "expected '=', but got ']'");

    ProtoMessageDefinition foo3 = getMessageByName(protoFile, "Foo3");
    ProtoProperty prop3 = getOnlyProperty(foo3);
    assertError(prop3, "Expected one of [true, false]");
  }

  public void testLazyFieldOption() throws Exception {
    writeTestProto(
      "message Foo {",
      "  optional int64 Bar = 1 [lazy=true];",
      "}");
    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);
  }

  public void testPackedFieldOption() throws Exception {
    writeTestProto(
        "message Foo {",
        "  optional int64 Bar = 1 [packed=true];",
        "}",
        "message Foo2 {",
        "  optional int64 Bar = 1 [packed=1];",
        "}");

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);

    ProtoMessageDefinition foo2 = getMessageByName(protoFile, "Foo2");
    ProtoProperty prop2 = getOnlyProperty(foo2);
    assertError(prop2, "Expected one of [true, false]");
  }

  public void testCustomFieldOption() throws Exception {
     writeTestProto(
         "message Foo {",
         "  optional int64 Bar = 1 [(cacheable) = true];",
         "}",
         "message Foo1 {",
         "  optional int64 Bar = 1 [(integer_property.name) = 1];",
         "}",
         "message Foo2 {",
         "  optional int64 Bar = 1 [(string.name) = \"name\"];",
         "}",
         "message Foo3 {",
         "  optional int64 Bar = 1 [(cacheable];",
         "}",
         "message Foo4 {",
         "  optional int64 Bar = 1 [(cacheable)];",
         "}",
         "message Foo5 {",
         "  optional int64 Bar = 1 [(cacheable) = abc];",
         "}");

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertNoError(prop);

    ProtoMessageDefinition foo1 = getMessageByName(protoFile, "Foo1");
    ProtoProperty prop1 = getOnlyProperty(foo1);
    assertNoError(prop1);

    ProtoMessageDefinition foo2 = getMessageByName(protoFile, "Foo2");
    ProtoProperty prop2 = getOnlyProperty(foo2);
    assertNoError(prop2);

    ProtoMessageDefinition foo3 = getMessageByName(protoFile, "Foo3");
    ProtoProperty prop3 = getOnlyProperty(foo3);
    assertError(prop3, "expected ')', but got ']'");

    ProtoMessageDefinition foo4 = getMessageByName(protoFile, "Foo4");
    ProtoProperty prop4 = getOnlyProperty(foo4);
    assertError(prop4, "expected '=', but got ']'");

    ProtoMessageDefinition foo5 = getMessageByName(protoFile, "Foo5");
    ProtoProperty prop5 = getOnlyProperty(foo5);
    assertError(prop5, "expected custom option value");
  }

  public void testCTypeFieldOption() throws Exception {
    writeTestProto(
        "message Foo {",
        "  optional string foo1 = 1 [ctype=CORD];",
        "  optional string foo2 = 2 [ctype=Cord];",
        "  optional string foo3 = 3 [ctype=STRING];",
        "  optional string foo4 = 4 [ctype=STRING_PIECE];",
        "}",
        "message Foo2 {",
        "  optional int64 Bar = 5 [ctype=1];",
        "}");

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    for (ProtoProperty prop : foo.getDefinitionBody().getProperties()) {
      assertNoError(prop);
    }

    ProtoMessageDefinition foo2 = getMessageByName(protoFile, "Foo2");
    ProtoProperty prop2 = getOnlyProperty(foo2);
    assertError(prop2, "Expected one of [STRING, CORD, Cord, STRING_PIECE, proto2]");
  }

  public void testWeakFieldOption() throws Exception {
    writeTestProto(
        "message Foo {",
        "  optional otherMessage Bar = 1 [weak=true];",
        "  optional message<OtherMessage> foo2 = 2 [weak=true];",
        "  optional message<OtherMessage> foo2 = 3 [weak=false];",
        "}",
        "message Foo2 {",
        "  optional otherMessage Bar = 1 [weak=1];",
        "}");

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    for (ProtoProperty prop : foo.getDefinitionBody().getProperties()) {
      assertNoError(prop);
    }

    ProtoMessageDefinition foo2 = getMessageByName(protoFile, "Foo2");
    ProtoProperty prop2 = getOnlyProperty(foo2);
    assertError(prop2, "Expected one of [true, false]");
  }

  public void testMessageKeywordWithoutBody() throws Exception {
    writeTestProto(
        "message ");

    ImmutableList<ProtoMessageDefinition> messageDefinitionList
        = ImmutableList.copyOf(protoFile.getMessageDefinitions());
    assertEquals(1, protoFile.getMessageDefinitions().size());
    assertError(messageDefinitionList.get(0), "expected message name");
  }
}
