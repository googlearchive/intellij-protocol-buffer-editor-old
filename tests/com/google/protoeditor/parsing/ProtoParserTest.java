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

import com.google.protoeditor.psi.ProtoFileOptionStatement;
import com.google.protoeditor.psi.ProtoImportStatement;
import com.google.protoeditor.psi.ProtoImportValue;
import com.google.protoeditor.psi.ProtoSyntaxStatement;
import com.google.protoeditor.psi.ProtoSyntaxValue;
import com.google.protoeditor.psi.ProtoTestCase;

import com.intellij.psi.PsiErrorElement;

import java.util.List;

/**
 * Verifies that we can parse protobuffer files.
 */
public class ProtoParserTest extends ProtoTestCase {

  public void testParseEmptyFile() throws Exception {
    writeTestProto("");

    assertNull(protoFile.getPackageStatement());
    assertEquals(0, protoFile.getTopLevelDefinitions().size());
  }

  public void testParseSyntaxStatement() throws Exception {
    writeTestProto("syntax = \"proto2\"");

    ProtoSyntaxStatement statement = protoFile.getSyntaxStatement();
    assertNotNull(statement);
    ProtoSyntaxValue value = statement.getProtoSyntaxValue();
    assertNotNull(value);
    assertEquals("\"proto2\"", value.getText());
  }

  public void testParseSyntaxStatement_without_eq() throws Exception {
    writeTestProto("syntax \"proto\"");

    ProtoSyntaxStatement statement = protoFile.getSyntaxStatement();
    assertNull(statement);

    List<PsiErrorElement> errorElement = protoFile.getErrorElements();
    assertEquals("expected '=', but got '\"proto\"'", errorElement.get(0).getErrorDescription());
  }

  public void testParseSyntaxStatement_without_value() throws Exception {
    writeTestProto("syntax = ");

    ProtoSyntaxStatement statement = protoFile.getSyntaxStatement();
    assertNull(statement);

    List<PsiErrorElement> errorElement = protoFile.getErrorElements();
    assertEquals("expected syntax value", errorElement.get(0).getErrorDescription());
  }

  public void testParseSyntaxStatement_without_semicolon() throws Exception {
    writeTestProto("syntax = \"proto2\"");

    ProtoSyntaxStatement statement = protoFile.getSyntaxStatement();
    assertNotNull(statement);

    assertError(statement, "expected ';', but got 'null'");
  }

  public void testParseImportStatement() throws Exception {
    writeTestProto("import \"a.b.c\";");

    List<ProtoImportStatement> statements = protoFile.getImportStatements();
    assertEquals(1, statements.size());
    ProtoImportStatement statement = statements.get(0);
    assertNotNull(statement);
    assertNoError(statement);

    ProtoImportValue value = statement.getProtoImportValue();
    assertNotNull(value);
    assertEquals("\"a.b.c\"", value.getText());
    assertNoError(value);
  }

  public void testParseImportStatement_without_value() throws Exception {
    writeTestProto("import");

    List<ProtoImportStatement> statements = protoFile.getImportStatements();
    assertEquals(0, statements.size());

    List<PsiErrorElement> errorElements = protoFile.getErrorElements();
    assertEquals("expected import value", errorElements.get(0).getErrorDescription());
  }

  public void testParseImportStatement_witho_unqoated_value() throws Exception {
    writeTestProto("import a.b.c;");

    List<ProtoImportStatement> statements = protoFile.getImportStatements();
    assertEquals(0, statements.size());

    List<PsiErrorElement> errorElements = protoFile.getErrorElements();
    assertEquals("expected import value", errorElements.get(0).getErrorDescription());
  }

  public void testParseImportStatement_without_semicolon() throws Exception {
    writeTestProto("import \"a.b.c\"");

    List<ProtoImportStatement> statements = protoFile.getImportStatements();
    assertEquals(1, statements.size());

    ProtoImportStatement statement = statements.get(0);
    assertNotNull(statement);
    assertError(statement, "expected ';', but got 'null'");

    ProtoImportValue value = statement.getProtoImportValue();
    assertNotNull(value);
    assertEquals("\"a.b.c\"", value.getText());
    assertNoError(value);
  }

  public void testParseFileOptionStatement_with_int_value() throws Exception {
    writeTestProto("option " + "java_api_version " + " = " + 1 + ";");

    List<ProtoFileOptionStatement> statements = protoFile.getFileOptionStatements();
    assertEquals(1, statements.size());

    ProtoFileOptionStatement statement = statements.get(0);
    assertEquals("java_api_version", statement.getName());
    assertEquals(String.valueOf(1), statement.getOptionValue().getLiteralValue().getText());
    assertNoError(statement);
    assertNoError(statement.getNameElement());
    assertNoError(statement.getOptionValue());
  }

  public void testParseFileOptionStatement_with_string_value() throws Exception {
    writeTestProto("option " + "java_package " + " = a.b.c;");

    List<ProtoFileOptionStatement> statements = protoFile.getFileOptionStatements();
    assertEquals(1, statements.size());

    ProtoFileOptionStatement statement = statements.get(0);
    assertEquals("java_package", statement.getName());
    assertEquals("a.b.c", statement.getOptionValue().getIdentifierValue());
    assertNoError(statement);
    assertNoError(statement.getNameElement());
    assertNoError(statement.getOptionValue());
  }

  public void testParseFileOptionStatement_with_boolean_value() throws Exception {
    writeTestProto("option " + "java_use_javaproto2 = true;");

    List<ProtoFileOptionStatement> statements = protoFile.getFileOptionStatements();
    assertEquals(1, statements.size());

    ProtoFileOptionStatement statement = statements.get(0);
    assertEquals("java_use_javaproto2", statement.getName());
    assertEquals("true", statement.getOptionValue().getLiteralValue().getText());
    assertNoError(statement);
    assertNoError(statement.getNameElement());
    assertNoError(statement.getOptionValue());
  }

  public void testParseFileOptionStatement_without_option_name() throws Exception {
    writeTestProto("option = 2;");

    List<ProtoFileOptionStatement> statements = protoFile.getFileOptionStatements();
    assertEquals(0, statements.size());

    List<PsiErrorElement> errorElements = protoFile.getErrorElements();
    assertEquals("expected option name", errorElements.get(0).getErrorDescription());
  }

  public void testParseFileOptionStatement_without_eq() throws Exception {
    writeTestProto("option " + "java_api_version 2;");

    List<ProtoFileOptionStatement> statements = protoFile.getFileOptionStatements();
    assertEquals(0, statements.size());

    List<PsiErrorElement> errorElements = protoFile.getErrorElements();
    assertEquals("expected '=', but got '2'", errorElements.get(0).getErrorDescription());
  }

  public void testParseFileOptionStatement_without_option_value() throws Exception {
    writeTestProto("option " + "java_api_version = ");

    List<ProtoFileOptionStatement> statements = protoFile.getFileOptionStatements();
    assertEquals(1, statements.size());

    ProtoFileOptionStatement statement = statements.get(0);
    assertEquals("java_api_version", statement.getName());
    assertNull(statement.getOptionValue());
    assertError(statement, "expected valid option value");
  }

  public void testCustomOptions() throws Exception {
    writeTestProto(
        "import \"net/proto2/proto/descriptor.proto\";", // google/protobuf/descriptor.proto in O/S
        "extend google.protobuf.MessageOptions {",
        "  optional string my_option = 51234;",
        "}",
        "message MyMessage {",
        "  option (my_option) = \"Hello world!\";",
        "}");

    assertNoParseErrors(protoFile);
  }

  // TODO: this is part of proto2 syntax that is not supported yet
  public void disabledTestComplexCustomOptions() throws Exception {
    // see net/proto2/internal/unittest_custom_options.proto
    writeTestProto(
        "message VariousComplexOptions {",
        "  option (complex_opt2).baz = 987;",
        "  option (complex_opt2).(grault) = 654;",
        "  option (complex_opt2).bar.foo = 743;",
        "  option (complex_opt2).bar.(quux) = 1999;",
        "  option (complex_opt2).bar.(proto2_unittest.corge).qux = 2008;",
        "  option (proto2_unittest.complex_opt1).(.proto2_unittest.quux) = 324;",
        "  option (.proto2_unittest.complex_opt1).foo = 42;",
        "  option (.proto2_unittest.complex_opt1).(proto2_unittest.corge).qux = 876;",
        "  option (complex_opt2).(garply).foo = 741;",
        "  option (complex_opt2).(garply).(.proto2_unittest.quux) = 1998;",
        "  option (complex_opt2).(proto2_unittest.garply).(corge).qux = 2121;",
        "  option (ComplexOptionType2.ComplexOptionType4.complex_opt4).waldo = 1971;",
        "  option (complex_opt2).fred.waldo = 321;",
        "  option (proto2_unittest.complex_opt3).qux = 9;",
        "  option (complex_opt3).complexoptiontype5.plugh = 22;",
        "  option (complexopt6).xyzzy = 24;",
        "}");

    assertNoParseErrors(protoFile);
  }

}
