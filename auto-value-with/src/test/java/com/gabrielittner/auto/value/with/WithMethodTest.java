package com.gabrielittner.auto.value.with;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.auto.common.MoreElements.getLocalAndInheritedMethods;
import static com.google.common.truth.Truth.assertThat;

public class WithMethodTest {

    @Rule public CompilationRule compilationRule = new CompilationRule();

    private Elements elements;
    private Types types;

    @Before
    public void setUp() {
        this.elements = compilationRule.getElements();
        this.types = compilationRule.getTypes();
    }

    @Test public void testResolvingGenericType() {
        TypeElement thing = elements.getTypeElement(Thing.class.getCanonicalName());
        ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(thing, elements);
        for (ExecutableElement method : methods) {
            if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                TypeMirror returns = WithMethod.getResolvedReturnType(types, thing, method);
                assertThat(returns.toString()).isEqualTo(Thing.class.getCanonicalName());
            }
        }

    }

    abstract class Thing extends BaseThing<Thing> {}

    abstract class BaseThing<P extends BaseThing<P>> extends BasementThing<P, P> {}

    abstract class BasementThing<K extends V, V extends BasementThing<K, V>> extends FoundationThing<K> {}

    abstract class FoundationThing<T extends FoundationThing<T>> {
        abstract T name();
    }
}
