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

package com.google.protoeditor.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.google.protoeditor.lex.ProtoTextAttributes;
import com.google.protoeditor.lex.ProtoTokenTypes;
import com.google.protoeditor.psi.ProtoAbstractIntegerLiteral;
import com.google.protoeditor.psi.ProtoChangeTools;
import com.google.protoeditor.psi.ProtoCustomOptionName;
import com.google.protoeditor.psi.ProtoDefaultValue;
import com.google.protoeditor.psi.ProtoDefinitionBody;
import com.google.protoeditor.psi.ProtoDefinitionBodyOwner;
import com.google.protoeditor.psi.ProtoEnumConstant;
import com.google.protoeditor.psi.ProtoEnumDefinition;
import com.google.protoeditor.psi.ProtoEnumValue;
import com.google.protoeditor.psi.ProtoExtendDefinition;
import com.google.protoeditor.psi.ProtoFile;
import com.google.protoeditor.psi.ProtoFileOptionStatement;
import com.google.protoeditor.psi.ProtoGroupDefinition;
import com.google.protoeditor.psi.ProtoHexLiteral;
import com.google.protoeditor.psi.ProtoIntegerLiteral;
import com.google.protoeditor.psi.ProtoLiteral;
import com.google.protoeditor.psi.ProtoMessageDefinition;
import com.google.protoeditor.psi.ProtoMessageTypeReference;
import com.google.protoeditor.psi.ProtoNameElement;
import com.google.protoeditor.psi.ProtoOption;
import com.google.protoeditor.psi.ProtoOptionsHolder;
import com.google.protoeditor.psi.ProtoPackageNameReference;
import com.google.protoeditor.psi.ProtoPackageStatement;
import com.google.protoeditor.psi.ProtoPrimitive;
import com.google.protoeditor.psi.ProtoPrimitiveType;
import com.google.protoeditor.psi.ProtoProperty;
import com.google.protoeditor.psi.ProtoPropertyId;
import com.google.protoeditor.psi.ProtoPsiTools;
import com.google.protoeditor.psi.ProtoRpcBody;
import com.google.protoeditor.psi.ProtoRpcDefinition;
import com.google.protoeditor.psi.ProtoServiceBody;
import com.google.protoeditor.psi.ProtoServiceDefinition;
import com.google.protoeditor.psi.ProtoSimpleProperty;
import com.google.protoeditor.psi.ProtoSimplePropertyType;
import com.google.protoeditor.psi.ProtoSyntaxStatement;
import com.google.protoeditor.psi.ProtoToplevelDefinition;
import com.google.protoeditor.psi.ProtoType;
import com.google.protoeditor.psi.ProtoUserDefinedPropertyType;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * This does the validations for the various types in proto file, and updates the annotation holder
 * accordingly.
 */
// TODO: Clean up this class when implementing syntax highlighting for new features.
// Fix old comments, and implement old TODOs.
public class ProtoValidator implements ProtoValidatorExtn {

  private static final List<ProtoPrimitive> INTEGER_PRIMITIVES = Arrays.asList(
      ProtoPrimitive.FIXED32,
      ProtoPrimitive.FIXED64,
      ProtoPrimitive.INT32,
      ProtoPrimitive.INT64,
      ProtoPrimitive.UINT32,
      ProtoPrimitive.UINT64,
      ProtoPrimitive.SFIXED32,
      ProtoPrimitive.SFIXED64,
      ProtoPrimitive.SINT32,
      ProtoPrimitive.SINT64);
  private static final List<ProtoPrimitive> INT32_PRIMITIVES = Arrays.asList(
      ProtoPrimitive.FIXED32,
      ProtoPrimitive.SFIXED32,
      ProtoPrimitive.INT32,
      ProtoPrimitive.UINT32);
  private static final List<ProtoPrimitive> INT64_PRIMITIVES = Arrays.asList(
      ProtoPrimitive.FIXED64,
      ProtoPrimitive.SFIXED64,
      ProtoPrimitive.INT64,
      ProtoPrimitive.UINT64);
  private static final List<ProtoPrimitive> FLOAT_PRIMITIVES = Arrays
      .asList(ProtoPrimitive.DOUBLE, ProtoPrimitive.FLOAT);
  private static final Pattern PATTERN_PACKAGE_NAME = Pattern.compile(
      "([a-zA-Z_][a-zA-Z_0-9]*)+(\\.([a-zA-Z_][a-zA-Z_0-9]*)+)*");

  //TODO: show gutter marker for classes generated from proto classes
  //TODO: add completion for keywords
  //TODO: resolve message names from other proto-files and from classpath
  //TODO: move members refactoring (to move elements between files)
  //TODO: show "reassign ID" as intention
  //TODO: convert some warnings to inspections
  //TODO: add intention for assigning id when missing id or =id for property and enum constant
  //TODO: accept "," in constant list, provide intention to convert to ";"
  //TODO: add refactoring: convert group to message

  public Map<Class, ProtoElementAnnotator> getAnnotators() {
    return ImmutableMap.<Class, ProtoElementAnnotator>builder()
        .put(ProtoSyntaxStatement.class, new SyntaxStatementAnnotator())
        .put(ProtoFileOptionStatement.class, new FileOptionStatementAnnotator())
        .build();
  }

  public void checkRpcDefinition(ProtoRpcDefinition rpcDefinition,
      AnnotationHolder annotationHolder) {
    ProtoRpcBody rpcBody = rpcDefinition.getRpcBody();
    if (rpcBody != null) {
      checkOptionOverwritten(rpcBody, annotationHolder);
    }
  }

  public void checkServiceDefinition(ProtoServiceDefinition serviceDefinition,
      AnnotationHolder annotationHolder) {
    checkRpcNames(serviceDefinition, annotationHolder);

    ProtoServiceBody serviceBody = serviceDefinition.getServiceBody();
    if (serviceBody != null) {
      checkOptionOverwritten(serviceBody, annotationHolder);
    }
  }

  @VisibleForTesting
  public void checkLeafs(LeafPsiElement leaf, AnnotationHolder annotationHolder) {
    if (leaf.getNode().getElementType() == ProtoTokenTypes.IDENTIFIER) {
      if (PsiTreeUtil.getParentOfType(leaf, ProtoPackageNameReference.class) == null &&
          !(leaf.getParent() instanceof ProtoSimplePropertyType) &&
          !(leaf.getParent() instanceof ProtoUserDefinedPropertyType) &&
          !(leaf.getParent() instanceof ProtoMessageTypeReference) &&
          !(leaf.getParent() instanceof ProtoExtendDefinition) &&
          !(leaf.getParent() instanceof ProtoCustomOptionName)) {
        String name = leaf.getText();
        if (name.contains(".")) {
          annotationHolder.createErrorAnnotation(leaf.getNode(),
                                                 "illegal name '" + name + "'");
        }
      }
    }
  }

  public void checkDuplicatePackageStatements(ProtoFile protoFile,
      AnnotationHolder annotationHolder) {
    List<ProtoPackageStatement> children = protoFile.getPackageStatements();
    if (children.size() > 1) {
      for (final ProtoPackageStatement element : children.subList(1, children.size())) {
        Annotation anno = annotationHolder
            .createErrorAnnotation(element, "multiple package statements");
        anno.registerFix(new RemovePackageStatementAction(element));
      }
    }
  }

  public void checkHexLiteral(ProtoHexLiteral hexLiteral,
      AnnotationHolder annotationHolder) {
    if (PsiTreeUtil.getParentOfType(hexLiteral,
                                    ProtoDefaultValue.class) == null
        && PsiTreeUtil.getParentOfType(hexLiteral,
                                       ProtoEnumConstant.class) == null) {
      Annotation anno = annotationHolder.createErrorAnnotation(
          hexLiteral, "hex literals may only be used for "
                      + "default values and enum constants");
      anno.registerFix(new ConvertToDecimalLiteralAction(hexLiteral));
    }
  }

  public void checkPackageStatement(ProtoPackageStatement packageStatement,
      AnnotationHolder annotationHolder) {
    ProtoPackageNameReference ref = packageStatement.getPackageNameReference();
    String name = ref.getReferencedPackageName();
    if (name != null) {
      if (!PATTERN_PACKAGE_NAME.matcher(name).matches()) {
        annotationHolder.createErrorAnnotation(
            ref, "illegal package name '" + name + "'");
      }
    }
  }

  @VisibleForTesting
  public void checkParentNameClash(ProtoProperty property,
      AnnotationHolder annotationHolder) {
    ProtoDefinitionBodyOwner parent = PsiTreeUtil
        .getParentOfType(property, ProtoDefinitionBodyOwner.class);
    if (parent == null || ProtoPsiTools.isProto2Syntax(property)) {
      return;
    }
    String name = property.getName();
    String parentName = parent.getName();
    if (name == null || parentName == null) {
      return;
    }
    if (name.equalsIgnoreCase(parentName)) {
      annotationHolder.createErrorAnnotation(
          property.getNameElement(), "property name cannot be "
                                     + "the same as parent's name");
    }
  }

  public void checkPropertyId(ProtoProperty property,
      AnnotationHolder annotationHolder) {
    ProtoPropertyId idel = property.getProtoIdElement();
    if (idel != null) {
      ProtoAbstractIntegerLiteral idLiteral = idel.getIdLiteral();
      if (idLiteral instanceof ProtoIntegerLiteral) {

        if (idLiteral.hasValidValue()) {
          long val = idLiteral.getIntValue();
          if (val > Integer.MAX_VALUE) {
            annotationHolder.createErrorAnnotation(
                idLiteral, "property ID must be less "
                           + "than " + Integer.MAX_VALUE);
          } else if (val <= 0) {
            annotationHolder.createErrorAnnotation(
                idLiteral, "property ID must be "
                           + "positive");
          } else if (val <= 15) {
            Annotation anno = annotationHolder.createInfoAnnotation(
                idLiteral, null);
            anno.setTextAttributes(ProtoTextAttributes.ATTR_CORE_ID);
          }
        }
      }
    }
  }

  public void checkIntegerLiteralValue(ProtoAbstractIntegerLiteral literal,
      AnnotationHolder annotationHolder) {
    if (!literal.hasValidValue()) {
      annotationHolder.createErrorAnnotation(literal,
                                             "integer value must be between " + Long.MIN_VALUE
                                             + " and " + Long.MAX_VALUE);
    }
  }

  public void checkGroupNameCase(ProtoGroupDefinition groupDefinition,
                                  AnnotationHolder annotationHolder) {
    final String name = groupDefinition.getName();
    if (name != null && name.length() >= 1) {
      char firstChar = name.charAt(0);
      if (!Character.isUpperCase(firstChar)) {
        final ProtoNameElement nameEl = groupDefinition.getNameElement();
        Annotation anno = annotationHolder.createErrorAnnotation(
            nameEl, "Group name must start with capital letter");
        final char upperFirstChar = Character.toUpperCase(firstChar);
        if (upperFirstChar != firstChar) {
          anno.registerFix(new CapitalizeGroupNameAction(nameEl));
        }
      }
    }
  }

  public void checkPropertyIds(ProtoMessageDefinition messageDefinition,
      AnnotationHolder annotationHolder) {
    SortedMap<Long, Set<ProtoProperty>> usedValues = new TreeMap<Long, Set<ProtoProperty>>();
    ProtoDefinitionBody body = messageDefinition.getDefinitionBody();
    addUsedValues(usedValues, body);
    for (Map.Entry<Long, Set<ProtoProperty>> entry : usedValues
        .entrySet()) {
      Set<ProtoProperty> props = entry.getValue();
      if (props.size() > 1) {
        for (ProtoProperty prop : props) {
          ProtoPropertyId protoIdElement = prop.getProtoIdElement();
          if (protoIdElement != null) {
            ProtoAbstractIntegerLiteral idEl = protoIdElement.getIdLiteral();
            Annotation anno = annotationHolder.createErrorAnnotation(idEl,
                                                                     "multiple properties have ID "
                                                                     + entry.getKey());
            anno.registerFix(new ReassignIdAction(prop, false));
          }
        }
      }
    }
  }

  private void addUsedValues(SortedMap<Long, Set<ProtoProperty>> usedValues,
                             ProtoDefinitionBody body) {
    for (ProtoProperty property : body.getProperties()) {
      ProtoPropertyId protoIdElement = property.getProtoIdElement();
      if (protoIdElement != null) {
        ProtoAbstractIntegerLiteral idLiteral = protoIdElement.getIdLiteral();
        if (idLiteral != null && idLiteral.hasValidValue()) {
          long idVal = idLiteral.getIntValue();
          Set<ProtoProperty> props = usedValues.get(idVal);
          if (props == null) {
            props = new HashSet<ProtoProperty>(5);
            usedValues.put(idVal, props);
          }
          props.add(property);
        }
      }
      if (property instanceof ProtoGroupDefinition) {
        ProtoGroupDefinition groupDefinition = (ProtoGroupDefinition) property;

        addUsedValues(usedValues, groupDefinition.getDefinitionBody());
      }
    }
  }

  public void checkUnusedMessages(ProtoFile file, AnnotationHolder annotationHolder) {
    List<ProtoServiceDefinition> serviceDefinitions = file.getServiceDefinitions();
    final Set<String> used = new HashSet<String>();
    boolean hasServices = false;
    for (ProtoServiceDefinition protoServiceDefinition : serviceDefinitions) {
      hasServices = true;
      protoServiceDefinition.accept(new UsedTypeNameCollector(used));
    }
    if (hasServices) {
      List<ProtoMessageDefinition> msgs = file.getMessageDefinitions();
      Set<String> tried = new HashSet<String>();
      boolean tryAgain = true;
      while (tryAgain) {
        tryAgain = false;
        for (ProtoMessageDefinition messageDefinition : msgs) {
          String name = messageDefinition.getName();
          if (!tried.contains(name) && used.contains(name)) {
            tried.add(name);
            tryAgain = true;
            messageDefinition.accept(new UsedTypeNameCollector(used));
          }
        }
      }
      for (ProtoMessageDefinition msg : msgs) {
        String name = msg.getName();
        if (name == null) {
          continue;
        }
        if (!used.contains(name)) {
          Annotation anno = annotationHolder.createWarningAnnotation(
              msg.getNameElement(), "message is never used by service");
          anno.setHighlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL);
        }
      }
    }
  }

  private void checkOptionOverwritten(ProtoOptionsHolder holder,
                                      AnnotationHolder annotationHolder) {
    Map<String, List<ProtoOption>> usedNames = new HashMap<String, List<ProtoOption>>();
    for (ProtoOption protoOption : holder.getOptions()) {
      String name = protoOption.getName();
      List<ProtoOption> options = usedNames.get(name);
      if (options == null) {
        options = new ArrayList<ProtoOption>();
        usedNames.put(name, options);
      }
      options.add(protoOption);
    }
    for (Map.Entry<String, List<ProtoOption>> entry : usedNames
        .entrySet()) {
      List<ProtoOption> options = entry.getValue();
      if (options.size() > 1) {
        for (Iterator<ProtoOption> it = options.iterator();
             it.hasNext();) {
          final ProtoOption protoOption = it.next();
          if (!it.hasNext()) {
            // the last one works
            break;
          }
          Annotation warning = annotationHolder.createWarningAnnotation(
              protoOption, "option setting is overwritten by "
                           + "later setting");
          warning.setHighlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL);
          warning.registerFix(new RemoveOptionAction(protoOption));
        }
      }
    }
  }

  private void checkRpcNames(ProtoServiceDefinition def,
                             AnnotationHolder annotationHolder) {
    Map<String, Set<ProtoRpcDefinition>> rpcNames
        = new HashMap<String, Set<ProtoRpcDefinition>>();
    for (ProtoRpcDefinition rpcDefinition : def
        .getRpcDefinitions()) {
      String name = rpcDefinition.getName();
      Set<ProtoRpcDefinition> defs = rpcNames.get(name);
      if (defs == null) {
        defs = new HashSet<ProtoRpcDefinition>();
        rpcNames.put(name, defs);
      }
      defs.add(rpcDefinition);
    }
    for (Map.Entry<String, Set<ProtoRpcDefinition>> entry : rpcNames
        .entrySet()) {
      Set<ProtoRpcDefinition> defs = entry.getValue();
      if (defs.size() > 1) {
        for (ProtoRpcDefinition rpcDef : defs) {
          annotationHolder.createErrorAnnotation(
              rpcDef.getNameElement(), "multiple "
                                       + "definitions of " + entry.getKey());
        }
      }
    }
  }

  public void checkMessageTypeReference(final ProtoMessageTypeReference ref,
      AnnotationHolder annotationHolder) {
    final String referencedName = ref.getReferencedName();
    if (ref.resolve() == null) {
      Annotation anno = annotationHolder.createErrorAnnotation(ref,
                                                               "unknown message type "
                                                               + referencedName);
      anno.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
      anno.registerFix(new CreateMessageAction(referencedName, ref));
    }
  }

  public void checkDuplicateNames(ProtoFile protoFile, AnnotationHolder annotationHolder) {
    Map<String, Set<ProtoToplevelDefinition>> map
        = new HashMap<String, Set<ProtoToplevelDefinition>>();
    for (ProtoToplevelDefinition messageDefinition : protoFile
        .getTopLevelDefinitions()) {
      String name = messageDefinition.getName();
      if (name == null) {
        continue;
      }

      Set<ProtoToplevelDefinition> defs = map.get(name);
      if (defs == null) {
        defs = new HashSet<ProtoToplevelDefinition>();
        map.put(name, defs);
      }
      defs.add(messageDefinition);
    }

    for (Map.Entry<String, Set<ProtoToplevelDefinition>> entry : map
        .entrySet()) {
      Set<ProtoToplevelDefinition> msgs = entry.getValue();
      if (msgs.size() > 1) {
        for (ProtoToplevelDefinition definition : msgs) {
          PsiElement nameEl = definition.getNameElement();
          annotationHolder.createErrorAnnotation(nameEl,
                                                 "multiple definitions of '"
                                                 + entry.getKey() + "'");
        }
      }
    }
  }

  public void checkEnums(
      ProtoDefinitionBody protoMessageDefinition, AnnotationHolder annotationHolder) {
    SetMultimap<String, ProtoEnumDefinition> enumNames = HashMultimap.create();

    for (ProtoEnumDefinition enm : protoMessageDefinition.getEnumerations()) {
      SetMultimap<String, ProtoEnumConstant> enumConstantNames =
          HashMultimap.create();

      String enumName = enm.getName();
      enumNames.put(enumName, enm);

      final SortedMap<Long, Set<ProtoEnumConstant>> enumNumbers =
          getUsedEnumConstantValues(enm, enumConstantNames);
      for (Map.Entry<Long, Set<ProtoEnumConstant>> entry : enumNumbers
          .entrySet()) {
        Set<ProtoEnumConstant> consts = entry.getValue();
        if (consts.size() > 1) {
          for (ProtoEnumConstant constant : consts) {
            ProtoEnumValue enumValue = constant.getEnumValue();
            if (enumValue == null) {
              continue;
            }

            final ProtoAbstractIntegerLiteral valueLiteral = enumValue.getValueLiteral();
            if (valueLiteral == null) {
              continue;
            }
            Annotation anno = annotationHolder.createWarningAnnotation(valueLiteral,
                                                                       "multiple constants in "
                                                                       + enm.getName()
                                                                       + " have value " + entry
                                                                           .getKey());
            anno.registerFix(new IntentionAction() {
              @Override
              public String getText() {
                return "Reassign enum constant value";
              }

              @Override
              public String getFamilyName() {
                return "ReassignEnumValue";
              }

              @Override
              public boolean isAvailable(Project project, Editor editor, PsiFile file) {
                return true;
              }

              @Override
              public void invoke(Project project, Editor editor, PsiFile file) {
                valueLiteral.setIntValue(enumNumbers.lastKey() + 1);
              }

              @Override
              public boolean startInWriteAction() {
                return true;
              }
            });
          }
        }
      }
      for (Map.Entry<String, Collection<ProtoEnumConstant>> entry : enumConstantNames
          .asMap().entrySet()) {
        Collection<ProtoEnumConstant> consts = entry.getValue();
        if (consts.size() > 1) {
          for (ProtoEnumConstant constant : consts) {
            annotationHolder.createErrorAnnotation(
                constant.getNameElement(),
                "multiple constants in " + enm.getName()
                + " have name " + entry.getKey());
          }
        }
      }
    }
    for (Map.Entry<String, Collection<ProtoEnumDefinition>> entry : enumNames
        .asMap().entrySet()) {
      Collection<ProtoEnumDefinition> enums = entry.getValue();
      if (enums.size() > 1) {
        for (ProtoEnumDefinition enm : enums) {
          annotationHolder.createErrorAnnotation(
              enm.getNameElement(), "multiple definitions "
                                    + "of enum " + entry.getKey());
        }
      }
    }
  }

  private SortedMap<Long, Set<ProtoEnumConstant>> getUsedEnumConstantValues(
      ProtoEnumDefinition enm,
      SetMultimap<String, ProtoEnumConstant> enumConstantNames) {
    SortedMap<Long, Set<ProtoEnumConstant>> enumNumbers
        = new TreeMap<Long, Set<ProtoEnumConstant>>();
    for (ProtoEnumConstant constant : enm.getConstants()) {
      ProtoNameElement nameElement = constant.getNameElement();
      if (nameElement != null) {
        String name = nameElement.getName();
        enumConstantNames.put(name, constant);
      }

      ProtoEnumValue enumValue = constant.getEnumValue();
      if (enumValue != null && enumValue.hasValidValue()) {
        long value = enumValue.getValue();
        Set<ProtoEnumConstant> constsForId = enumNumbers.get(value);
        if (constsForId == null) {
          constsForId = new HashSet<ProtoEnumConstant>();
          enumNumbers.put(value, constsForId);
        }
        constsForId.add(constant);
      }
    }
    return enumNumbers;
  }

  public void checkProperties(
      ProtoDefinitionBody body,
      AnnotationHolder annotationHolder) {
    Map<String, Set<ProtoProperty>> usedNames = new HashMap<String, Set<ProtoProperty>>();
    for (ProtoProperty property : body.getProperties()) {
      ProtoNameElement nameElement = property.getNameElement();
      if (nameElement != null) {
        String name = nameElement.getName();
        Set<ProtoProperty> namedProps = usedNames.get(name);
        if (namedProps == null) {
          namedProps = new HashSet<ProtoProperty>();
          usedNames.put(name, namedProps);
        }
        namedProps.add(property);
      }
    }
    Set<String> warned = new HashSet<String>();
    for (Map.Entry<String, Set<ProtoProperty>> entry : usedNames
        .entrySet()) {
      Set<ProtoProperty> props = entry.getValue();
      String name = entry.getKey();
      String lowerName = name.toLowerCase();
      if (props.size() > 1) {
        addErrorsForNames(props, annotationHolder, "multiple definitions of property '"
                                                   + name + "'");
      } else if (!warned.contains(lowerName)) {
        warned.add(lowerName);
        for (Map.Entry<String, Set<ProtoProperty>> otherEntry : usedNames.entrySet()) {
          String otherName = otherEntry.getKey();
          if (!name.equals(otherName)
              && name.equalsIgnoreCase(otherName)) {
            String errorMsg = "property names differ only in case";
            addErrorsForNames(props, annotationHolder, errorMsg);
            addErrorsForNames(otherEntry.getValue(), annotationHolder, errorMsg);
          }
        }
      }
    }
  }

  @VisibleForTesting
  void checkDefaultValueType(final ProtoSimpleProperty property,
                                     AnnotationHolder annotationHolder) {
    ProtoSimplePropertyType typeElement = property.getTypeElement();
    if (typeElement == null) {
      return;
    }
    final ProtoType propType = typeElement.getType();
    if (propType == null) {
      return;
    }
    ProtoDefaultValue protoDefaultValue = property.getDefaultValue();
    if (protoDefaultValue == null) {
      return;
    }
    ProtoLiteral valueElement = protoDefaultValue.getValueElement();
    if (valueElement == null) {
      return;
    }

    final Collection<? extends ProtoType> valTypes
        = valueElement.getPossibleTypes();
    if (!isAssignableFromAny(propType, valTypes)) {
      String valTypeStr;
      if (valTypes.size() == 1) {
        valTypeStr = valTypes.iterator().next().getDisplayName();

      } else {
        valTypeStr = getTypeString(valTypes);
      }
      Annotation anno = annotationHolder.createErrorAnnotation(protoDefaultValue,
                                                               "cannot assign " + valTypeStr
                                                               + " to " + propType
                                                                   .getDisplayName());
      for (ProtoType type : valTypes) {
        anno.registerFix(new ChangePropertyTypeAction(type, property));
      }
    }
  }

  protected boolean isAssignableFromAny(ProtoType propType,
      Collection<? extends ProtoType> valTypes) {
    boolean good = false;
    for (ProtoType valType : valTypes) {
      if (propType.isAssignableFrom(valType)) {
        good = true;
        break;
      }
    }
    return good;
  }

  private String getTypeString(Collection<? extends ProtoType> valTypes) {
    List<ProtoPrimitive> primitives = new ArrayList<ProtoPrimitive>();
    boolean good = true;
    for (ProtoType type : valTypes) {
      if (type instanceof ProtoPrimitiveType) {
        ProtoPrimitiveType protoPrimitiveType = (ProtoPrimitiveType) type;
        primitives.add(protoPrimitiveType.getPrimitiveType());
      } else {
        good = false;
        break;
      }
    }
    if (good) {
      List<ProtoPrimitive> primClone = new ArrayList<ProtoPrimitive>(primitives);
      boolean containsFloat = containsAny(primClone, FLOAT_PRIMITIVES);
      boolean containsInt = containsAny(primClone, INT32_PRIMITIVES);
      boolean containsLong = containsAny(primClone, INT64_PRIMITIVES);
      primClone.removeAll(FLOAT_PRIMITIVES);
      primClone.removeAll(INTEGER_PRIMITIVES);
      if (primClone.isEmpty()) {
        if (containsFloat && containsInt && containsLong) {
          return "number";
        } else if (containsInt && containsLong) {
          return "integer";
        } else if (containsInt) {
          return "32-bit integer";
        } else if (containsLong) {
          return "64-bit integer";
        } else if (containsFloat) {
          return "float";
        }
      }
    }
    StringBuffer sb = new StringBuffer(valTypes.size() * 10);
    boolean first = true;
    for (ProtoType type : valTypes) {
      if (first) {
        first = false;
      } else {
        sb.append("|");
      }
      sb.append(type.getDisplayName());
    }
    return sb.toString();
  }

  private void addErrorsForNames(Set<ProtoProperty> props,
                                 AnnotationHolder annotationHolder, String errorMsg) {
    for (ProtoProperty prop : props) {
      annotationHolder.createErrorAnnotation(prop.getNameElement(), errorMsg);
    }
  }

  private static <E> boolean containsAny(Collection<? extends E> collection,
                                         Collection<? extends E> elements) {
    for (E e : elements) {
      if (collection.contains(e)) {
        return true;
      }
    }
    return false;
  }

  private static class UsedTypeNameCollector extends PsiRecursiveElementVisitor {

    private final Set<String> used;

    public UsedTypeNameCollector(Set<String> used) {
      this.used = used;
    }

    @Override
    public void visitElement(PsiElement psiElement) {
      super.visitElement(psiElement);

      if (psiElement instanceof ProtoMessageTypeReference) {
        ProtoMessageTypeReference protoMessageTypeReference
            = (ProtoMessageTypeReference) psiElement;
        used.add(protoMessageTypeReference.getReferencedName());
      }
    }
  }

  private static class RemoveOptionAction implements IntentionAction {

    private final ProtoOption protoOption;

    public RemoveOptionAction(ProtoOption protoOption) {
      this.protoOption = protoOption;
    }

    @Override
    public String getText() {
      return "Remove option setting";
    }

    @Override
    public String getFamilyName() {
      return "RemoveOption";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file)
        throws IncorrectOperationException {
      ASTNode optionNode = protoOption.getNode();
      optionNode.getTreeParent().removeChild(optionNode);
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class CreateMessageAction implements IntentionAction {

    private final String referencedName;
    private final ProtoMessageTypeReference ref;

    public CreateMessageAction(String referencedName, ProtoMessageTypeReference ref) {
      this.referencedName = referencedName;
      this.ref = ref;
    }

    @Override
    public String getText() {
      return "Create message '" + referencedName + "'";
    }

    @Override
    public String getFamilyName() {
      return "CreateMessage";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file)
        throws IncorrectOperationException {
      ProtoToplevelDefinition parentMsg = PsiTreeUtil
          .getParentOfType(ref, ProtoToplevelDefinition.class);
      if (parentMsg == null) {
        return;
      }

      ASTNode parentNode = parentMsg.getNode().getTreeParent();
      PsiElement nextSibling = parentMsg.getNextSibling();
      ASTNode msgNode = ProtoChangeTools.createMessageFromText(project,
                                                               "parsed message " + referencedName
                                                               + " {\n}");
      if (nextSibling == null) {
        parentNode.addChild(msgNode);
      } else {
        parentNode.addChild(msgNode, nextSibling.getNode());
      }
      PsiElement psi = msgNode.getPsi();
      CodeStyleManager.getInstance(psi.getManager()).reformat(psi);
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class ReassignIdAction implements IntentionAction {

    private boolean tryLessThan16;
    private final ProtoProperty prop;

    public ReassignIdAction(ProtoProperty prop, boolean tryLessThan16) {
      this.prop = prop;
      this.tryLessThan16 = tryLessThan16;
    }

    @Override
    public String getText() {
      return "Reassign ID";
    }

    @Override
    public String getFamilyName() {
      return "ReassignId";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file)
        throws IncorrectOperationException {
      ProtoMessageDefinition body = PsiTreeUtil
          .getParentOfType(prop, ProtoMessageDefinition.class);
      SortedSet<Long> ids = getUsedIds(body);
      long literal = -1;
      if (tryLessThan16) {
        for (long i = 15; i >= 1; i--) {
          if (!ids.contains(i)) {
            literal = i;
            break;
          }
        }
      }
      if (literal == -1) {
        literal = ids.last() + 1;
      }
      prop.getProtoIdElement().getIdLiteral().setIntValue(literal);
    }

    private SortedSet<Long> getUsedIds(ProtoMessageDefinition body) {
      SortedSet<Long> ids = new TreeSet<Long>();
      addUsedIds(ids, body.getDefinitionBody());
      return ids;
    }

    private void addUsedIds(SortedSet<Long> ids, ProtoDefinitionBody body) {
      for (ProtoProperty property : body.getProperties()) {
        if (property != prop) {
          ProtoPropertyId idElement = property.getProtoIdElement();
          ProtoAbstractIntegerLiteral idLiteral = idElement.getIdLiteral();
          ids.add(idLiteral.getIntValue());
        }
        if (property instanceof ProtoGroupDefinition) {
          ProtoGroupDefinition groupDefinition = (ProtoGroupDefinition) property;
          addUsedIds(ids, groupDefinition.getDefinitionBody());
        }
      }
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class ChangePropertyTypeAction implements IntentionAction {

    private final ProtoType valType;
    private final ProtoSimpleProperty property;

    public ChangePropertyTypeAction(ProtoType valType, ProtoSimpleProperty property) {
      this.valType = valType;
      this.property = property;
    }

    @Override
    public String getText() {
      return "Change property type to '" + valType.getDisplayName() + "'";
    }

    @Override
    public String getFamilyName() {
      return "ChangePropertyType";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file)
        throws IncorrectOperationException {
      ASTNode newNode = ProtoChangeTools
          .createIdentifierFromText(project, valType.getIdentifierText());
      ASTNode node = property.getTypeElement().getNode();
      ASTNode nodeParent = node.getTreeParent();
      nodeParent.replaceChild(node,
                              newNode);
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class CapitalizeGroupNameAction implements IntentionAction {

    private final ProtoNameElement nameEl;

    public CapitalizeGroupNameAction(ProtoNameElement nameEl) {
      this.nameEl = nameEl;
    }

    @Override
    public String getText() {
      return "Capitalize group name";
    }

    @Override
    public String getFamilyName() {
      return "CapitalizeGroupName";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file)
        throws IncorrectOperationException {
      String name = nameEl.getName();
      char upperFirst = Character.toUpperCase(name.charAt(0));
      nameEl.setName(upperFirst + name.substring(1));
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class ConvertToDecimalLiteralAction implements IntentionAction {

    private final ProtoHexLiteral hexLiteral;

    public ConvertToDecimalLiteralAction(ProtoHexLiteral hexLiteral) {
      this.hexLiteral = hexLiteral;
    }

    @Override
    public String getText() {
      return "Convert to decimal literal";
    }

    @Override
    public String getFamilyName() {
      return "ConvertToDecimal";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file) {
      long val = hexLiteral.getIntValue();
      ASTNode newNode = ProtoChangeTools
          .createLiteralFromText(project, Long.toString(val));
      ASTNode oldNode = hexLiteral.getNode();
      oldNode.getTreeParent().replaceChild(oldNode, newNode);
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class RemovePackageStatementAction implements IntentionAction {

    private final ProtoPackageStatement element;

    public RemovePackageStatementAction(ProtoPackageStatement element) {
      this.element = element;
    }

    @Override
    public String getText() {
      return "Remove duplicate package statement";
    }

    @Override
    public String getFamilyName() {
      return "RemovePackageStatement";
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file)
        throws IncorrectOperationException {
      ASTNode node = element.getNode();
      node.getTreeParent().removeChild(node);
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }
}
