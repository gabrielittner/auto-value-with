package com.gabrielittner.auto.value.with;

import com.gabrielittner.auto.value.util.Property;
import com.google.auto.service.AutoService;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import static com.gabrielittner.auto.value.util.AutoValueUtil.getAutoValueClassTypeName;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newFinalClassConstructorCall;
import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;
import static javax.lang.model.element.Modifier.FINAL;

@AutoService(AutoValueExtension.class)
public class AutoValueWithExtension extends AutoValueExtension {

    @Override
    public boolean applicable(Context context) {
        return WithMethod.filteredAbstractMethods(context).size() > 0;
    }

    @Override
    public IncrementalExtensionType incrementalType(ProcessingEnvironment processingEnvironment) {
        return IncrementalExtensionType.ISOLATING;
    }

    @Override
    public Set<ExecutableElement> consumeMethods(Context context) {
        return WithMethod.filteredAbstractMethods(context);
    }

    @Override
    public String generateClass(
            Context context, String className, String classToExtend, boolean isFinal) {
        TypeSpec subclass =
                newTypeSpecBuilder(context, className, classToExtend, isFinal)
                        .addMethods(generateWithMethods(context))
                        .build();

        return JavaFile.builder(context.packageName(), subclass).build().toString();
    }

    private List<MethodSpec> generateWithMethods(Context context) {
        List<WithMethod> withMethods = WithMethod.getWithMethods(context);
        ImmutableList<Property> properties = Property.buildProperties(context);
        List<MethodSpec> generatedMethods = new ArrayList<>(withMethods.size());
        for (WithMethod withMethod : withMethods) {
            generatedMethods.add(generateWithMethod(withMethod, context, properties));
        }
        return generatedMethods;
    }

    private MethodSpec generateWithMethod(
            WithMethod withMethod, Context context, ImmutableList<Property> properties) {
        String[] propertyNames = new String[properties.size()];
        for (int i = 0; i < propertyNames.length; i++) {
            Property property = properties.get(i);
            if (withMethod.propertyNames.contains(property.humanName())) {
                propertyNames[i] = property.humanName();
            } else {
                propertyNames[i] = property.methodName() + "()";
            }
        }

        List<AnnotationSpec> annotations = new ArrayList<>(withMethod.methodAnnotations.size() + 1);
        for (AnnotationMirror methodAnnotation : withMethod.methodAnnotations) {
            annotations.add(AnnotationSpec.get(methodAnnotation));
        }
        AnnotationSpec override = AnnotationSpec.builder(Override.class).build();
        if (!annotations.contains(override)) {
            annotations.add(0, override);
        }

        List<Modifier> modifiers = new ArrayList<>(2);
        modifiers.add(FINAL);
        for (Modifier modifier : withMethod.methodModifiers) {
            if (modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED) {
                modifiers.add(modifier);
                break;
            }
        }

        List<ParameterSpec> parameters = new ArrayList<>(withMethod.properties.size());
        for (Property property : withMethod.properties) {
            parameters.add(ParameterSpec.builder(property.type(), property.humanName()).build());
        }

        return MethodSpec.methodBuilder(withMethod.methodName)
                .addAnnotations(annotations)
                .addModifiers(modifiers)
                .returns(getAutoValueClassTypeName(context))
                .addParameters(parameters)
                .addCode("return ")
                .addCode(newFinalClassConstructorCall(context, propertyNames))
                .build();
    }
}
