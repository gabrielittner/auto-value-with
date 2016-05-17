package com.gabrielittner.auto.value.with;

import com.gabrielittner.auto.value.util.Property;
import com.google.auto.value.extension.AutoValueExtension.Context;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

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
            TypeMirror returnType = method.getReturnType();
            if (!environment.getTypeUtils().isAssignable(autoValueClass.asType(), returnType)) {
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
}
