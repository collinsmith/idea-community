/*
 * Copyright 2000-2007 JetBrains s.r.o.
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
package com.intellij.psi.util;

import com.intellij.psi.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class PsiFormatUtil extends PsiFormatUtilBase {

  public static String formatVariable(PsiVariable variable, int options, PsiSubstitutor substitutor){
    StringBuilder buffer = new StringBuilder();
    formatVariable(variable, options, substitutor,buffer);
    return buffer.toString();
  }
  private static void formatVariable(PsiVariable variable, int options, PsiSubstitutor substitutor,StringBuilder buffer){
    if ((options & SHOW_MODIFIERS) != 0 && (options & MODIFIERS_AFTER) == 0){
      formatModifiers(variable, options,buffer);
    }
    if ((options & SHOW_TYPE) != 0 && (options & TYPE_AFTER) == 0){
      appendSpaceIfNeeded(buffer);
      buffer.append(formatType(variable.getType(), options, substitutor));
    }
    if (variable instanceof PsiField && (options & SHOW_CONTAINING_CLASS) != 0){
      PsiClass aClass = ((PsiField)variable).getContainingClass();
      if (aClass != null){
        String className = aClass.getName();
        if (className != null) {
          appendSpaceIfNeeded(buffer);
          if ((options & SHOW_FQ_NAME) != 0){
            String qName = aClass.getQualifiedName();
            if (qName != null){
              buffer.append(qName);
            }
            else{
              buffer.append(className);
            }
          }
          else{
            buffer.append(className);
          }
          buffer.append('.');
        }
      }
      if ((options & SHOW_NAME) != 0){
        buffer.append(variable.getName());
      }
    }
    else{
      if ((options & SHOW_NAME) != 0){
        String name = variable.getName();
        if (name != null){
          appendSpaceIfNeeded(buffer);
          buffer.append(name);
        }
      }
    }
    if ((options & SHOW_TYPE) != 0 && (options & TYPE_AFTER) != 0){
      if ((options & SHOW_NAME) != 0 && variable.getName() != null){
        buffer.append(':');
      }
      buffer.append(formatType(variable.getType(), options, substitutor));
    }
    if ((options & SHOW_MODIFIERS) != 0 && (options & MODIFIERS_AFTER) != 0){
      formatModifiers(variable, options,buffer);
    }
    if ((options & SHOW_INITIALIZER) != 0){
      PsiExpression initializer = variable.getInitializer();
      if (initializer != null){
        buffer.append(" = ");
        String text = initializer.getText();
        int index1 = text.lastIndexOf('\n');
        if (index1 < 0) index1 = text.length();
        int index2 = text.lastIndexOf('\r');
        if (index2 < 0) index2 = text.length();
        int index = Math.min(index1, index2);
        buffer.append(text.substring(0, index));
        if (index < text.length()) {
          buffer.append(" ...");
        }
      }
    }
  }

  public static String formatMethod(PsiMethod method, PsiSubstitutor substitutor, int options, int parameterOptions){
    return formatMethod(method, substitutor, options, parameterOptions, MAX_PARAMS_TO_SHOW);
  }

  public static String formatMethod(PsiMethod method, PsiSubstitutor substitutor, int options, int parameterOptions, int maxParametersToShow){
    StringBuilder buffer = new StringBuilder();
    formatMethod(method, substitutor, options, parameterOptions, maxParametersToShow,buffer);
    return buffer.toString();
  }
  private static void formatMethod(PsiMethod method, PsiSubstitutor substitutor, int options, int parameterOptions, int maxParametersToShow, StringBuilder buffer){
    if ((options & SHOW_MODIFIERS) != 0 && (options & MODIFIERS_AFTER) == 0){
      formatModifiers(method, options,buffer);
    }
    if ((options & SHOW_TYPE) != 0 && (options & TYPE_AFTER) == 0){
      PsiType type = method.getReturnType();
      if (type != null){
        appendSpaceIfNeeded(buffer);
        buffer.append(formatType(type, options, substitutor));
      }
    }
    if ((options & SHOW_CONTAINING_CLASS) != 0){
      PsiClass aClass = method.getContainingClass();
      if (aClass != null){
        appendSpaceIfNeeded(buffer);
        String name = aClass.getName();
        if (name != null) {
          if ((options & SHOW_FQ_NAME) != 0){
            String qName = aClass.getQualifiedName();
            if (qName != null){
              buffer.append(qName);
            }
            else{
              buffer.append(name);
            }
          }
          else{
            buffer.append(name);
          }
          buffer.append('.');
        }
      }
      if ((options & SHOW_NAME) != 0){
        buffer.append(method.getName());
      }
    }
    else{
      if ((options & SHOW_NAME) != 0){
        appendSpaceIfNeeded(buffer);
        buffer.append(method.getName());
      }
    }
    if ((options & SHOW_PARAMETERS) != 0){
      buffer.append('(');
      PsiParameter[] parms = method.getParameterList().getParameters();
      for(int i = 0; i < Math.min(parms.length, maxParametersToShow); i++) {
        PsiParameter parm = parms[i];
        if (i > 0){
          buffer.append(", ");
        }
        buffer.append(formatVariable(parm, parameterOptions, substitutor));
      }
      if(parms.length > maxParametersToShow) {
        buffer.append (", ...");
      }
      buffer.append(')');
    }
    if ((options & SHOW_TYPE) != 0 && (options & TYPE_AFTER) != 0){
      PsiType type = method.getReturnType();
      if (type != null){
        if (buffer.length() > 0){
          buffer.append(':');
        }
        buffer.append(formatType(type, options, substitutor));
      }
    }
    if ((options & SHOW_MODIFIERS) != 0 && (options & MODIFIERS_AFTER) != 0){
      formatModifiers(method, options,buffer);
    }
    if ((options & SHOW_THROWS) != 0){
      String throwsText = formatReferenceList(method.getThrowsList(), options);
      if (throwsText.length() > 0){
        appendSpaceIfNeeded(buffer);
        //noinspection HardCodedStringLiteral
        buffer.append("throws ");
        buffer.append(throwsText);
      }
    }
  }

  @NotNull public static String formatClass(@NotNull PsiClass aClass, int options){
    StringBuilder buffer = new StringBuilder();
    if ((options & SHOW_MODIFIERS) != 0 && (options & MODIFIERS_AFTER) == 0){
      formatModifiers(aClass, options,buffer);
    }
    if ((options & SHOW_NAME) != 0){
      if (aClass instanceof PsiAnonymousClass && (options & SHOW_ANONYMOUS_CLASS_VERBOSE) != 0) {
        final PsiClassType baseClassReference = ((PsiAnonymousClass) aClass).getBaseClassType();
        PsiClass baseClass = baseClassReference.resolve();
        String name = baseClass == null ? baseClassReference.getPresentableText() : formatClass(baseClass, options);
        buffer.append(PsiBundle.message("anonymous.class.derived.display", name));
      }
      else {
        String name = aClass.getName();
        if (name != null) {
          appendSpaceIfNeeded(buffer);
          if ((options & SHOW_FQ_NAME) != 0) {
            String qName = aClass.getQualifiedName();
            if (qName != null) {
              buffer.append(qName);
            }
            else {
              buffer.append(aClass.getName());
            }
          }
          else {
            buffer.append(aClass.getName());
          }
        }
      }
    }
    if ((options & SHOW_MODIFIERS) != 0 && (options & MODIFIERS_AFTER) != 0){
      formatModifiers(aClass, options,buffer);
    }
    if ((options & SHOW_EXTENDS_IMPLEMENTS) != 0){
      String extendsText = formatReferenceList(aClass.getExtendsList(), options);
      if (extendsText.length() > 0){
        appendSpaceIfNeeded(buffer);
        //noinspection HardCodedStringLiteral
        buffer.append("extends ");
        buffer.append(extendsText);
      }
      String implementsText = formatReferenceList(aClass.getImplementsList(), options);
      if (implementsText.length() > 0){
        appendSpaceIfNeeded(buffer);
        //noinspection HardCodedStringLiteral
        buffer.append("implements ");
        buffer.append(implementsText);
      }
    }
    return buffer.toString();
  }

  public static String formatModifiers(PsiElement element, int options) throws IllegalArgumentException{
    StringBuilder buffer = new StringBuilder();
    formatModifiers(element, options,buffer);
    return buffer.toString();
  }
  private static void formatModifiers(PsiElement element, int options, StringBuilder buffer) throws IllegalArgumentException{
    PsiModifierList list;
    boolean isInterface = false;
    if (element instanceof PsiVariable){
      list = ((PsiVariable)element).getModifierList();
    }
    else if (element instanceof PsiMethod){
      list = ((PsiMethod)element).getModifierList();
    }
    else if (element instanceof PsiClass){
      isInterface = ((PsiClass)element).isInterface();
      list = ((PsiClass)element).getModifierList();
      if (list == null) return;
    }
    else if (element instanceof PsiClassInitializer){
      list = ((PsiClassInitializer)element).getModifierList();
      if (list == null) return;
    }
    else{
      throw new IllegalArgumentException();
    }
    if (list == null) return;
    if ((options & SHOW_REDUNDANT_MODIFIERS) == 0
        ? list.hasExplicitModifier(PsiModifier.PUBLIC)
        : list.hasModifierProperty(PsiModifier.PUBLIC)) {
      appendModifier(buffer, PsiModifier.PUBLIC);
    }

    if (list.hasModifierProperty(PsiModifier.PROTECTED)){
      appendModifier(buffer, PsiModifier.PROTECTED);
    }
    if (list.hasModifierProperty(PsiModifier.PRIVATE)){
      appendModifier(buffer, PsiModifier.PRIVATE);
    }

    if ((options & SHOW_REDUNDANT_MODIFIERS) == 0
        ? list.hasExplicitModifier(PsiModifier.PACKAGE_LOCAL)
        : list.hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) {
      if (element instanceof PsiClass && element.getParent() instanceof PsiDeclarationStatement) {// local class
        appendModifier(buffer, PsiBundle.message("local.class.preposition"));
      }
      else {
        appendModifier(buffer, PsiBundle.visibilityPresentation(PsiModifier.PACKAGE_LOCAL));
      }
    }

    if ((options & SHOW_REDUNDANT_MODIFIERS) == 0
        ? list.hasExplicitModifier(PsiModifier.STATIC)
        : list.hasModifierProperty(PsiModifier.STATIC)) appendModifier(buffer, PsiModifier.STATIC);

    if (!isInterface && //cls modifier list
        ((options & SHOW_REDUNDANT_MODIFIERS) == 0
         ? list.hasExplicitModifier(PsiModifier.ABSTRACT)
         : list.hasModifierProperty(PsiModifier.ABSTRACT))) appendModifier(buffer, PsiModifier.ABSTRACT);

    if ((options & SHOW_REDUNDANT_MODIFIERS) == 0
        ? list.hasExplicitModifier(PsiModifier.FINAL)
        : list.hasModifierProperty(PsiModifier.FINAL)) appendModifier(buffer, PsiModifier.FINAL);

    if (list.hasModifierProperty(PsiModifier.NATIVE) && (options & JAVADOC_MODIFIERS_ONLY) == 0){
      appendModifier(buffer, PsiModifier.NATIVE);
    }
    if (list.hasModifierProperty(PsiModifier.SYNCHRONIZED) && (options & JAVADOC_MODIFIERS_ONLY) == 0){
      appendModifier(buffer, PsiModifier.SYNCHRONIZED);
    }
    if (list.hasModifierProperty(PsiModifier.STRICTFP) && (options & JAVADOC_MODIFIERS_ONLY) == 0){
      appendModifier(buffer, PsiModifier.STRICTFP);
    }
    if (list.hasModifierProperty(PsiModifier.TRANSIENT) &&
        element instanceof PsiVariable // javac 5 puts transient attr for methods
       ){
      appendModifier(buffer, PsiModifier.TRANSIENT);
    }
    if (list.hasModifierProperty(PsiModifier.VOLATILE)){
      appendModifier(buffer, PsiModifier.VOLATILE);
    }
  }

  private static void appendModifier(final StringBuilder buffer, final String modifier) {
    appendSpaceIfNeeded(buffer);
    buffer.append(modifier);
  }

  public static String formatReferenceList(PsiReferenceList list, int options){
    StringBuilder buffer = new StringBuilder();
    PsiJavaCodeReferenceElement[] refs = list.getReferenceElements();
    for(int i = 0; i < refs.length; i++) {
      PsiJavaCodeReferenceElement ref = refs[i];
      if (i > 0){
        buffer.append(", ");
      }
      buffer.append(formatReference(ref, options));
    }
    return buffer.toString();
  }

  public static String formatType(PsiType type, int options, PsiSubstitutor substitutor){
    type = substitutor.substitute(type);
    if ((options & SHOW_RAW_TYPE) != 0) {
      type = TypeConversionUtil.erasure(type);
    }
    return (options & SHOW_FQ_CLASS_NAMES) == 0 ? type.getPresentableText() : type.getInternalCanonicalText();
  }

  public static String formatReference(PsiJavaCodeReferenceElement ref, int options){
    return (options & SHOW_FQ_CLASS_NAMES) == 0 ? ref.getText() : ref.getCanonicalText();
  }

  @Nullable
  public static String getExternalName(PsiModifierListOwner owner) {
    return getExternalName(owner, true);
  }

  @Nullable
  public static String getExternalName(PsiModifierListOwner owner, final boolean showParamName) {
    final StringBuilder builder = new StringBuilder();
    if (owner instanceof PsiClass) {
      ClassUtil.formatClassName((PsiClass)owner, builder);
      return builder.toString();
    }
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(owner, PsiClass.class, false);
    assert psiClass != null;
    ClassUtil.formatClassName(psiClass, builder);
    if (owner instanceof PsiMethod) {
      builder.append(" ");
      formatMethod((PsiMethod)owner, PsiSubstitutor.EMPTY,
                   SHOW_NAME | SHOW_FQ_NAME | SHOW_TYPE | SHOW_PARAMETERS | SHOW_FQ_CLASS_NAMES,
                   showParamName ? SHOW_NAME | SHOW_TYPE | SHOW_FQ_CLASS_NAMES : SHOW_TYPE | SHOW_FQ_CLASS_NAMES, MAX_PARAMS_TO_SHOW, builder);
    }
    else if (owner instanceof PsiField) {
      builder.append(" ").append(((PsiField)owner).getName());
    }
    else if (owner instanceof PsiParameter) {
      final PsiElement declarationScope = ((PsiParameter)owner).getDeclarationScope();
      if (declarationScope instanceof PsiMethod) {
        final PsiMethod psiMethod = (PsiMethod)declarationScope;

        builder.append(" ");
        formatMethod(psiMethod, PsiSubstitutor.EMPTY,
                     SHOW_NAME | SHOW_FQ_NAME | SHOW_TYPE | SHOW_PARAMETERS | SHOW_FQ_CLASS_NAMES,
                     showParamName ? SHOW_NAME | SHOW_TYPE | SHOW_FQ_CLASS_NAMES : SHOW_TYPE | SHOW_FQ_CLASS_NAMES, MAX_PARAMS_TO_SHOW, builder);
        builder.append(" ");

        if (showParamName) {
          formatVariable((PsiVariable)owner, SHOW_NAME, PsiSubstitutor.EMPTY, builder);
        }
        else {
          builder.append(psiMethod.getParameterList().getParameterIndex((PsiParameter)owner));
        }
      }
      else {
        return null;
      }
    }
    else {
      return null;
    }
    return builder.toString();
  }

  public static String getPackageDisplayName(@NotNull final PsiClass psiClass) {
    @NonNls String packageName = psiClass.getQualifiedName();
    packageName = packageName == null || packageName.lastIndexOf('.') <= 0 ? "" : packageName.substring(0, packageName.lastIndexOf('.'));
    if (packageName.length() == 0) {
      packageName = "default package";
    }
    return packageName;
  }
}