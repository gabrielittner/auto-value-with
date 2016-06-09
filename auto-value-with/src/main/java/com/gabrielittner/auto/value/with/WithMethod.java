package com.gabrielittner.auto.value.with;

import com.gabrielittner.auto.value.util.Property;
import com.google.auto.value.extension.AutoValueExtension.Context;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

class WithMethod {

    private static final String PREFIX = "with";

    final String methodName;
    final Set<Modifier> methodModifiers;
    final List<? extends AnnotationMirror> methodAnnotations;

    final List<Property> properties;
    final List<String> propertyNames;

    private WithMethod(ExecutableElement method, List<Property> properties,
            List<String> methodPropertyNames) {
        this.methodName = method.getSimpleName().toString();
        this.methodModifiers = method.getModifiers();
        this.methodAnnotations = method.getAnnotationMirrors();
        this.properties = properties;
        this.propertyNames = methodPropertyNames;
    }

    static List<WithMethod> getWithMethods(Context context) {
        Messager messager = context.processingEnvironment().getMessager();
        Types typeUtils = context.processingEnvironment().getTypeUtils();

        TypeElement autoValueClass = context.autoValueClass();
        Map<String, ExecutableElement> properties = context.properties();

        ImmutableSet<ExecutableElement> methods = filteredAbstractMethods(context);
        List<WithMethod> withMethods = new ArrayList<>(methods.size());
        for (ExecutableElement method : methods) {
            TypeMirror returnType = getResolvedReturnType(typeUtils, autoValueClass, method);
            if (!typeUtils.isAssignable(autoValueClass.asType(), returnType)) {
                String message = String.format("Expected %s as return type", autoValueClass);
                messager.printMessage(Kind.ERROR, message, method);
                continue;
            }

            List<? extends VariableElement> parameters = method.getParameters();
            List<Property> methodProperties = new ArrayList<>(parameters.size());
            List<String> methodPropertyNames = new ArrayList<>(parameters.size());
            for (VariableElement parameter : parameters) {
                String propertyName = parameter.getSimpleName().toString();
                ExecutableElement propertyMethod = properties.get(propertyName);
                if (propertyMethod == null) {
                    String message = String.format("Property \"%s\" not found", propertyName);
                    messager.printMessage(Kind.ERROR, message, parameter);
                    continue;
                }
                Property property = new Property(propertyName, propertyMethod);
                if (!TypeName.get(parameter.asType()).equals(property.type())) {
                    String message =
                            String.format("Expected type %s for %s", property.type(), propertyName);
                    messager.printMessage(Kind.ERROR, message, parameter);
                    continue;
                }
                methodProperties.add(property);
                methodPropertyNames.add(propertyName);
            }

            withMethods.add(new WithMethod(method, methodProperties, methodPropertyNames));
        }
        return withMethods;
    }

    static ImmutableSet<ExecutableElement> filteredAbstractMethods(Context context) {
        Set<ExecutableElement> abstractMethods = context.abstractMethods();
        ImmutableSet.Builder<ExecutableElement> withMethods = ImmutableSet.builder();
        for (ExecutableElement method : abstractMethods) {
            if (method.getSimpleName().toString().startsWith(PREFIX)
                    && method.getParameters().size() > 0) {
                withMethods.add(method);
            }
        }
        return withMethods.build();
    }

    static TypeMirror getResolvedReturnType(
            Types typeUtils, TypeElement type, ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() == TypeKind.TYPEVAR) {
            List<HierarchyElement> hierarchy =
                    getHierarchyUntilClassWithElement(typeUtils, type, method);
            return resolveGenericType(hierarchy, returnType);
        }
        return returnType;
    }

    private static List<HierarchyElement> getHierarchyUntilClassWithElement(
            Types typeUtils, TypeElement start, Element target) {

        for (TypeMirror superType : typeUtils.directSupertypes(start.asType())) {
            TypeElement superTypeElement = (TypeElement) typeUtils.asElement(superType);
            if (superTypeElement.getEnclosedElements().contains(target)) {
                HierarchyElement base = new HierarchyElement(superTypeElement, null);
                HierarchyElement current = new HierarchyElement(start, superType);
                return new ArrayList<>(Arrays.asList(base, current));
            }
        }

        for (TypeMirror superType : typeUtils.directSupertypes(start.asType())) {
            TypeElement superTypeElement = (TypeElement) typeUtils.asElement(superType);
            List<HierarchyElement> result =
                    getHierarchyUntilClassWithElement(typeUtils, superTypeElement, target);
            if (result != null) {
                result.add(new HierarchyElement(start, superType));
                return result;
            }
        }
        return null;
    }

    private static TypeMirror resolveGenericType(
            List<HierarchyElement> hierarchy, TypeMirror type) {

        int position = indexOfParameter(hierarchy.get(0).element, type.toString());
        for (int i = 1; i < hierarchy.size(); i++) {
            HierarchyElement hierarchyElement = hierarchy.get(i);

            type = ((DeclaredType) hierarchyElement.superType).getTypeArguments().get(position);
            if (type.getKind() != TypeKind.TYPEVAR) {
                return type;
            }

            position = indexOfParameter(hierarchyElement.element, type.toString());
        }
        throw new IllegalArgumentException("Couldn't resolve type " + type);
    }

    private static class HierarchyElement {
        private final TypeElement element;
        private final TypeMirror superType;

        private HierarchyElement(TypeElement element, TypeMirror superType) {
            this.element = element;
            this.superType = superType;
        }
    }

    private static int indexOfParameter(TypeElement element, String param) {
        List<? extends TypeParameterElement> params = element.getTypeParameters();
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getSimpleName().toString().equals(param)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Param " + param + "not not found in list");
    }
}
