package com.gabrielittner.auto.value.with;

import com.gabrielittner.auto.value.util.Property;
import com.google.auto.value.extension.AutoValueExtension.Context;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
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

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;

class WithMethod {

    private static final String PREFIX = "with";

    final String methodName;
    final Set<Modifier> methodModifiers;
    final List<? extends AnnotationMirror> methodAnnotations;

    final Property property;

    private WithMethod(String methodName, Set<Modifier> methodModifiers,
            List<? extends AnnotationMirror> methodAnnotations, Property property) {
        this.methodName = methodName;
        this.methodModifiers = methodModifiers;
        this.methodAnnotations = methodAnnotations;
        this.property = property;
    }

    static List<WithMethod> getWithMethods(Context context) {
        ImmutableSet<ExecutableElement> methods = filterMethods(context);
        ProcessingEnvironment environment = context.processingEnvironment();
        Types typeUtils = environment.getTypeUtils();
        TypeElement autoValueClass = context.autoValueClass();

        Map<String, ExecutableElement> properties = context.properties();
        List<WithMethod> withMethods = new ArrayList<>(methods.size());
        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();

            Property property;
            String propertyName = removePrefix(methodName);
            ExecutableElement element = properties.get(propertyName);
            if (element != null) {
                property = new Property(propertyName, element);
            } else {
                throw new IllegalArgumentException(String.format("%s doesn't have property with name"
                        + " %s which is required for %s()", autoValueClass, propertyName, methodName));
            }

            List<? extends VariableElement> parameters = method.getParameters();
            if (parameters.size() != 1
                    || !TypeName.get(parameters.get(0).asType()).equals(property.type())) {
                throw new IllegalArgumentException(String.format("Expected single argument of type"
                        + " %s for %s()", property.type(), methodName));
            }
            TypeMirror returnType = getResolvedReturnType(typeUtils, autoValueClass, method);
            if (!typeUtils.isAssignable(autoValueClass.asType(), returnType)) {
                throw new IllegalArgumentException(String.format("%s() in %s returns %s, expected %s",
                        methodName, autoValueClass, returnType, autoValueClass));
            }
            withMethods.add(new WithMethod(methodName, method.getModifiers(),
                    method.getAnnotationMirrors(), property));
        }
        return withMethods;
    }

    static ImmutableSet<ExecutableElement> filterMethods(Context context) {
        ImmutableSet<ExecutableElement> methods = getAbstractMethods(context);
        List<ExecutableElement> withMethods = new ArrayList<>(methods.size());
        for (ExecutableElement method : methods) {
            if (method.getModifiers().contains(Modifier.ABSTRACT)
                    && method.getSimpleName().toString().startsWith(PREFIX)
                    && method.getParameters().size() > 0) {
                withMethods.add(method);
            }
        }
        return ImmutableSet.copyOf(withMethods);
    }

    private static ImmutableSet<ExecutableElement> getAbstractMethods(Context context) {
        //TODO AutoValue 1.3: replace with context.getAbstractMethods()
        ProcessingEnvironment environment = context.processingEnvironment();
        TypeElement autoValueClass = context.autoValueClass();
        return getLocalAndInheritedMethods(autoValueClass, environment.getElementUtils());
    }

    private static String removePrefix(String name) {
        return Character.toLowerCase(name.charAt(PREFIX.length()))
                + name.substring(PREFIX.length() + 1);
    }


    static TypeMirror getResolvedReturnType(
            Types typeUtils, TypeElement type, ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() == TypeKind.TYPEVAR) {
            List<TypeElement> hierarchy = getHierarchyUntilClassWithElement(typeUtils, type, method);
            return resolveGenericType(hierarchy, returnType);
        }
        return returnType;
    }

    private static List<TypeElement> getHierarchyUntilClassWithElement(
            Types typeUtils, TypeElement start, Element target) {
        List<TypeElement> classHierarchy = new ArrayList<>();
        classHierarchy.add(start);
        TypeElement element = start;
        while (!element.getEnclosedElements().contains(target)) {
            element = (TypeElement) typeUtils.asElement(element.getSuperclass());
            classHierarchy.add(element);
        }
        Collections.reverse(classHierarchy);
        return classHierarchy;
    }

    private static TypeMirror resolveGenericType(List<TypeElement> hierarchy, TypeMirror type) {
        int position = indexOfParameter(hierarchy.get(0).getTypeParameters(), type.toString());
        for (int i = 1; i < hierarchy.size(); i++) {
            TypeElement current = hierarchy.get(i);
            type = ((DeclaredType) current.getSuperclass()).getTypeArguments().get(position);

            if (type.getKind() != TypeKind.TYPEVAR) {
                return type;
            }

            position = indexOfParameter(current.getTypeParameters(), type.toString());
        }
        throw new IllegalArgumentException("Couldn't resolve type " + type);
    }

    private static int indexOfParameter(List<? extends TypeParameterElement> params, String param) {
        for (int i = 0; i < params.size(); i++) {
            if (params.get(i).getSimpleName().toString().equals(param)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Param " + param + "not not found in list");
    }
}
