package com.gabrielittner.auto.value.with;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.util.*;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;

@AutoService(AutoValueExtension.class)
public class AutoValueWithExtension extends AutoValueExtension {

    private static final String PREFIX = "with";

    private static List<WithMethod> getWithMethods(Context context) {
        ProcessingEnvironment environment = context.processingEnvironment();
        TypeElement autoValueClass = context.autoValueClass();
        ImmutableSet<ExecutableElement> methods = MoreElements.getLocalAndInheritedMethods(autoValueClass,
                environment.getElementUtils());
        Set<String> propertyNames = context.properties().keySet();
        List<WithMethod> withMethods = new ArrayList<WithMethod>(methods.size());
        for (ExecutableElement method : methods) {
            String methodName = method.getSimpleName().toString();
            if (!methodName.startsWith(PREFIX)) {
                continue;
            }
            List<? extends VariableElement> parameters = method.getParameters();
            int parameterCount = parameters.size();
            if (parameterCount == 0) {
                continue;
            }
            if (parameterCount > 1) {
                throw new IllegalArgumentException(String.format("%s() in %s has %d parameters, expected 1",
                    methodName, autoValueClass, parameterCount));
            }

            int propertyNameStart = PREFIX.length();
            String methodPropertyName = Character.toLowerCase(methodName.charAt(propertyNameStart))
                    + methodName.substring(propertyNameStart + 1);
            if (!propertyNames.contains(methodPropertyName)) {
                throw new IllegalArgumentException(String.format("%s doesn't have property with name %s which"
                        + " is required for %s()", autoValueClass, methodPropertyName, methodName));
            }
            VariableElement parameter = parameters.get(0);
            String parameterName = parameter.getSimpleName().toString();
            if (!methodPropertyName.equals(parameterName)) {
                throw new IllegalArgumentException(String.format("%s() in %s has \"%s\" as parameter, expected \"%s\"",
                        methodName, autoValueClass, parameterName, methodPropertyName));
            }

            TypeMirror returnType = method.getReturnType();
            if (!environment.getTypeUtils().isAssignable(autoValueClass.asType(), returnType)) {
                throw new IllegalArgumentException(String.format("%s() in %s returns %s, expected %s",
                        methodName, autoValueClass, returnType, autoValueClass));
            }
            withMethods.add(new WithMethod(methodName, method.getModifiers(), method.getAnnotationMirrors(),
                    parameterName, parameter.asType()));
        }
        return withMethods;
    }

    private static class WithMethod {
        private final String methodName;
        private final Set<Modifier> methodModifiers;
        private final List<? extends AnnotationMirror> methodAnnotations;
        private final String propertyName;
        private final TypeMirror propertyType;

        WithMethod(String methodName, Set<Modifier> methodModifiers, List<? extends AnnotationMirror> methodAnnotations,
                   String propertyName, TypeMirror propertyType) {
            this.methodName = methodName;
            this.methodModifiers = methodModifiers;
            this.methodAnnotations = methodAnnotations;
            this.propertyName = propertyName;
            this.propertyType = propertyType;
        }
    }

    @Override
    public boolean applicable(Context context) {
        return getWithMethods(context).size() > 0;
    }

    @Override
    public Set<String> consumeProperties(Context context) {
        List<WithMethod> withMethods = getWithMethods(context);
        Set<String> consumedProperties = new HashSet<String>(withMethods.size());
        for (WithMethod method : withMethods) {
            consumedProperties.add(method.methodName);
        }
        return consumedProperties;
    }

    @Override public String generateClass(Context context, String className, String classToExtend,
                                          boolean isFinal) {
        String packageName = context.packageName();
        Map<String, ExecutableElement> properties = context.properties();

        TypeSpec.Builder subclass = TypeSpec.classBuilder(className)
                .addModifiers(isFinal ? FINAL : ABSTRACT)
                .superclass(ClassName.get(packageName, classToExtend))
                .addMethod(generateConstructor(properties))
                .addMethods(generateWithMethods(context, className, properties));

        return JavaFile.builder(packageName, subclass.build())
                .build()
                .toString();
    }

    private MethodSpec generateConstructor(Map<String, ExecutableElement> properties) {
        List<ParameterSpec> params = Lists.newArrayList();
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            TypeName typeName = TypeName.get(entry.getValue().getReturnType());
            params.add(ParameterSpec.builder(typeName, entry.getKey()).build());
        }

        StringBuilder superFormat = new StringBuilder("super(");
        for (int i = properties.size(); i > 0; i--) {
            superFormat.append("$N");
            if (i > 1) superFormat.append(", ");
        }
        superFormat.append(")");

        return MethodSpec.constructorBuilder()
                .addParameters(params)
                .addStatement(superFormat.toString(), properties.keySet().toArray())
                .build();
    }

    private List<MethodSpec> generateWithMethods(Context context, String className,
                                                 Map<String, ExecutableElement> properties) {
        List<WithMethod> withMethods = getWithMethods(context);
        String packageName = context.packageName();
        List<MethodSpec> generatedMethods = new ArrayList<MethodSpec>(withMethods.size());
        Set<String> keySet = properties.keySet();
        String[] propertyNames = new String[keySet.size()];
        propertyNames = keySet.toArray(propertyNames);
        for (WithMethod withMethod : withMethods) {
            generatedMethods.add(generateWithMethod(withMethod, packageName, className, propertyNames));
        }
        return generatedMethods;
    }

    private MethodSpec generateWithMethod(WithMethod withMethod, String packageName, String className,
                String[] propertyNames) {
        String finalAutoValueClass = className.replaceAll("\\$", "");
        StringBuilder format = new StringBuilder("return new ");
        format.append(finalAutoValueClass);
        format.append("(");
        for (int i = 0; i < propertyNames.length; i++) {
            if (i > 0) format.append(", ");
            format.append("$L");
            if (!propertyNames[i].equals(withMethod.propertyName)) format.append("()");
        }
        format.append(")");

        List<AnnotationSpec> annotationSpecs = new ArrayList<AnnotationSpec>(withMethod.methodAnnotations.size() + 1);
        annotationSpecs.add(AnnotationSpec.builder(Override.class).build());
        for (AnnotationMirror methodAnnotation : withMethod.methodAnnotations) {
            annotationSpecs.add(AnnotationSpec.get(methodAnnotation));
        }
        List<Modifier> modifiers = new ArrayList<Modifier>(2);
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
                .returns(ClassName.get(packageName, finalAutoValueClass))
                .addParameter(TypeName.get(withMethod.propertyType), withMethod.propertyName)
                .addStatement(format.toString(), (Object[]) propertyNames)
                .build();

    }
}
