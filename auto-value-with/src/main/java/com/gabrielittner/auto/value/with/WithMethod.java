package com.gabrielittner.auto.value.with;

import com.gabrielittner.auto.value.util.Property;
import com.google.auto.value.extension.AutoValueExtension.Context;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import static com.gabrielittner.auto.value.util.ElementUtil.getResolvedReturnType;

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
}
