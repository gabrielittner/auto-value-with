package com.gabrielittner.auto.value.with;

import com.gabrielittner.auto.value.util.Property;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashSet;
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

import static com.gabrielittner.auto.value.util.AutoValueUtil.getAutoValueClassClassName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newFinalClassConstructorCall;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static javax.lang.model.element.Modifier.FINAL;

@AutoService(AutoValueExtension.class)
public class AutoValueWithExtension extends AutoValueExtension {

    private static final String PREFIX = "with";

    private static List<WithMethod> getWithMethods(Context context) {
        ProcessingEnvironment environment = context.processingEnvironment();
        TypeElement autoValueClass = context.autoValueClass();
        ImmutableSet<ExecutableElement> methods = MoreElements.getLocalAndInheritedMethods(
                autoValueClass, environment.getElementUtils());
        Map<String, ExecutableElement> propertyNames = context.properties();
        List<WithMethod> withMethods = new ArrayList<>(methods.size());
        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();
            if (!method.getModifiers().contains(Modifier.ABSTRACT)) {
                continue;
            }
            if (!methodName.startsWith(PREFIX)) {
                continue;
            }
            List<? extends VariableElement> parameters = method.getParameters();
            if (parameters.size() == 0) {
                continue;
            }

            Property property;
            String propertyName = removePrefix(methodName);
            ExecutableElement element = propertyNames.get(propertyName);
            if (element != null) {
                property = new Property(propertyName, element);
            } else {
                throw new IllegalArgumentException(String.format("%s doesn't have property with name"
                        + " %s which is required for %s()", autoValueClass, propertyName, methodName));
            }

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

    private static String removePrefix(String name) {
        return Character.toLowerCase(name.charAt(PREFIX.length()))
                + name.substring(PREFIX.length() + 1);
    }

    private static class WithMethod {
        private final String methodName;
        private final Set<Modifier> methodModifiers;
        private final List<? extends AnnotationMirror> methodAnnotations;

        private final Property property;

        WithMethod(String methodName, Set<Modifier> methodModifiers,
                List<? extends AnnotationMirror> methodAnnotations, Property property) {
            this.methodName = methodName;
            this.methodModifiers = methodModifiers;
            this.methodAnnotations = methodAnnotations;
            this.property = property;
        }
    }

    @Override
    public boolean applicable(Context context) {
        return getWithMethods(context).size() > 0;
    }

    @Override
    public Set<String> consumeProperties(Context context) {
        List<WithMethod> withMethods = getWithMethods(context);
        Set<String> consumedProperties = new HashSet<>(withMethods.size());
        for (WithMethod method : withMethods) {
            consumedProperties.add(method.methodName);
        }
        return consumedProperties;
    }

    @Override public String generateClass(Context context, String className, String classToExtend,
            boolean isFinal) {
        TypeSpec.Builder subclass = newTypeSpecBuilder(context, className, classToExtend, isFinal)
                .addMethods(generateWithMethods(context));

        return JavaFile.builder(context.packageName(), subclass.build())
                .build()
                .toString();
    }

    private List<MethodSpec> generateWithMethods(Context context) {
        List<WithMethod> withMethods = getWithMethods(context);
        ImmutableList<Property> properties = properties(context);
        List<MethodSpec> generatedMethods = new ArrayList<>(withMethods.size());
        for (WithMethod withMethod : withMethods) {
            generatedMethods.add(generateWithMethod(withMethod, context, properties));
        }
        return generatedMethods;
    }

    private MethodSpec generateWithMethod(WithMethod withMethod, Context context,
            ImmutableList<Property> properties) {
        String[] propertyNames = new String[properties.size()];
        for (int i = 0; i < propertyNames.length; i++) {
            Property property = properties.get(i);
            if (property.humanName().equals(withMethod.property.humanName())) {
                propertyNames[i] = property.humanName();
            } else {
                propertyNames[i] = property.methodName() + "()";
            }
        }

        List<AnnotationSpec> annotationSpecs = new ArrayList<>(withMethod.methodAnnotations.size() + 1);
        annotationSpecs.add(AnnotationSpec.builder(Override.class).build());
        for (AnnotationMirror methodAnnotation : withMethod.methodAnnotations) {
            annotationSpecs.add(AnnotationSpec.get(methodAnnotation));
        }

        List<Modifier> modifiers = new ArrayList<>(2);
        modifiers.add(FINAL);
        for (Modifier modifier : withMethod.methodModifiers) {
            if (modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED) {
                modifiers.add(modifier);
                break;
            }
        }

        return MethodSpec.methodBuilder(withMethod.methodName)
                .addAnnotations(annotationSpecs)
                .addModifiers(modifiers)
                .returns(getAutoValueClassClassName(context))
                .addParameter(withMethod.property.type(), withMethod.property.humanName())
                .addCode("return ")
                .addCode(newFinalClassConstructorCall(context, propertyNames))
                .build();

    }

    private static ImmutableList<Property> properties(AutoValueExtension.Context context) {
        ImmutableList.Builder<Property> values = ImmutableList.builder();
        for (Map.Entry<String, ExecutableElement> entry : context.properties().entrySet()) {
            values.add(new Property(entry.getKey(), entry.getValue()));
        }
        return values.build();
    }
}
