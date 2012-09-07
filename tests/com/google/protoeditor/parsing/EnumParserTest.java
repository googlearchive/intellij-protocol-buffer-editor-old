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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.protoeditor.psi.AbstractProtoElement;
import com.google.protoeditor.psi.ProtoEnumBody;
import com.google.protoeditor.psi.ProtoEnumConstant;
import com.google.protoeditor.psi.ProtoEnumDefinition;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoTestCase;

import java.util.List;

import javax.annotation.Nullable;

public class EnumParserTest extends ProtoTestCase {

  private void assertEnumEquals(String name, List<String> constants,
      ProtoEnumDefinition enumDef) {
    assertEquals(name, enumDef.getName());
    assertNotNull(enumDef.getEnumBody());
    assertAnyOrder(Iterables.transform(enumDef.getConstants(),
        new Function<ProtoEnumConstant, String> () {
          @Override
          public String apply(@Nullable ProtoEnumConstant protoEnumConstant) {
            return protoEnumConstant.getName();
          }
        }), constants);
  }

  public void testParseProtoWithOnlyOneEnum() throws Exception {
    writeTestProto(
        "enum Response {",
        "  YES = 0;",
        "  NO = 1;",
        "}"
    );
    assertNoParseErrors(protoFile);
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertEnumEquals("Response", ImmutableList.of("YES", "NO"), enumDef);
  }

  public void testParseEnumDefinitionWithHexLiterals() throws Exception {
    writeTestProto(
        "enum Response {",
        "  YES = 0xA;",
        "  NO = 0xB;",
        "}"
    );
    assertNoParseErrors(protoFile);
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertEnumEquals("Response", ImmutableList.of("YES", "NO"), enumDef);
  }

  public void testParseEnumDefinitionWithCommas() throws Exception {
    writeTestProto(
        "enum Response {",
        "  YES = 0,",
        "  NO = 1,",
        "};"
    );
    assertNoParseErrors(protoFile);
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertEnumEquals("Response", ImmutableList.of("YES", "NO"), enumDef);
  }

  public void testParseBadEnumDefinitionWithMissingColon() throws Exception {
    writeTestProto(
        "enum Response {",
        "  YES = 0;",
        "  NO = 1",
        "};"
    );
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertNotNull(enumDef);
    assertNoError(enumDef.getConstants().get(0));
    assertError(enumDef.getConstants().get(1), "expected ';' or ','");
  }

  public void testParseBadEnumDefinitionWithMissingRBrace() throws Exception {
    writeTestProto(
        "enum Response {",
        "  YES = 0;",
        "  NO = 1;"
    );
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertNotNull(enumDef);
    assertError(enumDef.getEnumBody(), "expected '}'");
  }

  public void testParseEnumWithoutLeftBrace() throws Exception {
    writeTestProto(
        "package foo;",
        "enum Foo",
        "  Bar1 = 1,",
        "  Bar2 = 2,",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoEnumDefinition foo = getOnlyTopLevelEnumeration();
    assertError(foo, "expected '{', but got 'Bar1'");
    List<ProtoEnumConstant> props = getEnumConstants(foo);
    assertEquals(props.get(0).getName(), "Bar1");
    assertEquals(props.get(1).getName(), "Bar2");
    assertNoError(props.get(0));
    assertNoError(props.get(1));
  }

  public void testParseBadEnumDefinitionWithMissingName() throws Exception {
    writeTestProto(
        "enum {",
        "  YES = 0;",
        "  NO = 1;",
        "}"
    );
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertNotNull(enumDef);
    assertError(enumDef, "expected enum name");
  }

  public void testParseEnumWithoutEqualTo() throws Exception {
    writeTestProto(
        "package foo;",
        "enum Foo {",
        "  Bar1 1,",
        "  Bar2 = 2,",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoEnumDefinition foo = getOnlyTopLevelEnumeration();
    assertNoError(foo);
    List<ProtoEnumConstant> props = getEnumConstants(foo);
    assertEquals(props.get(0).getName(), "Bar1");
    assertEquals(props.get(1).getName(), "Bar2");
    assertError(props.get(0), "expected '=', but got '1'");
    assertNoError(props.get(1));
  }

  public void testParseEnumWithoutComma() throws Exception {
    writeTestProto(
        "package foo;",
        "enum Foo {",
        "  Bar1 = 1",
        "  Bar2 = 2,",
        "}"
    );
    assertPackageName(protoFile, "foo");
    ProtoEnumDefinition foo = getOnlyTopLevelEnumeration();
    List<ProtoEnumConstant> constVals = foo.getConstants();
    assertError(constVals.get(0), "expected ';' or ','");
  }

  public void testParseProtoWithOnlyEnums() throws Exception {
    writeTestProto(
        "enum DimensionFormatter {",
        "  TO_STRING = 1;",
        "  TO_AD_FORMAT = 2;",
        "  TO_COST_TYPE = 3;",
        "  TO_TARGETING_TYPE = 4;",
        "}",
        "",
        "enum MetricFormatter {",
        "  TO_INT64 = 6;",
        "  TO_DOUBLE = 7;",
        "}",
        "",
        "enum SpamAdjustmentMode {",
        "  WITHOUT_SPAM = 0;",
        "  WITH_ALL_SPAM = 1;",
        "  WITH_ONLINE_SPAM = 2;",
        "}",
        "",
        "enum SpamNormalizationMode {",
        "  POSITIVE_ADD = 0;",
        "  SPAM_ADD = 1;",
        "}"
    );
    List<ProtoEnumDefinition> enums = Lists.newArrayList(getTopLevelEnumerations());
    assertNoParseErrors(protoFile);
    assertEquals(4, enums.size());
    assertEnumEquals("DimensionFormatter",
        ImmutableList.of("TO_STRING", "TO_AD_FORMAT", "TO_COST_TYPE", "TO_TARGETING_TYPE"),
        enums.get(0));
    assertEnumEquals("MetricFormatter", ImmutableList.of("TO_INT64", "TO_DOUBLE"), enums.get(1));
    assertEnumEquals("SpamAdjustmentMode",
        ImmutableList.of("WITHOUT_SPAM", "WITH_ALL_SPAM", "WITH_ONLINE_SPAM"),
        enums.get(2));
    assertEnumEquals("SpamNormalizationMode",
        ImmutableList.of("POSITIVE_ADD", "SPAM_ADD"), enums.get(3));
  }

  public void testParseProtoWithEnumsIndependentOfMessages() throws Exception {
    writeTestProto(
        "enum Response {",
        "    YES = 0;",
        "    NO = 1;",
        "}",
        "message Foo {",
        "  required Response answer = 1;",
        "}"
    );

    assertNoParseErrors(protoFile);
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertEnumEquals("Response", ImmutableList.of("YES", "NO"), enumDef);

    ProtoMessageDefinition foo = getMessageByName(protoFile, "Foo");
    ProtoProperty prop = getOnlyProperty(foo);
    assertEnumProperty(prop, "answer", "Response");
    assertNoError(prop);
  }

  public void testParseEnumsWithFieldOptions() throws Exception {
    writeTestProto(
        "enum Response {",
        "    YES = 0 [(foo)=\"bar1\"];",
        "    NO = 1 [(foo) = \"bar2\"];",
        "}"
    );

    assertNoParseErrors(protoFile);
    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    assertEnumEquals("Response", ImmutableList.of("YES", "NO"), enumDef);
  }

  public void testParseEnumsWithInvalidFieldOption() throws Exception {
    writeTestProto(
        "enum Response {",
        "    YES = 0 [(foo)=];",
        "    NO = 1;",
        "}"
    );

    ProtoEnumDefinition enumDef = getOnlyTopLevelEnumeration();
    ProtoEnumBody body = enumDef.getEnumBody();
    assertError((AbstractProtoElement) body.getChildren()[0], "expected custom option value");
  }
}
