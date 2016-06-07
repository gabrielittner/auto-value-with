package com.gabrielittner.auto.value.with;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.CompilationRule;
import javax.lang.model.element.ExecutableElement;
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

    private void resolvedGenericTypeTest(Class cls, String methodName) {
        String className = cls.getCanonicalName();
        TypeElement thing = elements.getTypeElement(className);
        ImmutableSet<ExecutableElement> methods = getLocalAndInheritedMethods(thing, elements);
        for (ExecutableElement method : methods) {
            if (method.getSimpleName().toString().equals(methodName)) {
                TypeMirror returns = WithMethod.getResolvedReturnType(types, thing, method);
                assertThat(returns.toString()).isEqualTo(className);
            }
        }
    }



    abstract class Foo extends BaseFoo<Foo> {}

    abstract class BaseFoo<T extends BaseFoo<T>> {
        abstract T name1();
    }

    @Test
    public void testResolvingGenericTypeSimple() {
        resolvedGenericTypeTest(Foo.class, "name1");
    }



    abstract class Thing extends BaseThing<Thing> {}

    abstract class BaseThing<P extends BaseThing<P>> extends BasementThing<P, P> {}

    abstract class BasementThing<K extends V, V extends BasementThing<K, V>>
            extends FoundationThing<K> {}

    abstract class FoundationThing<T extends FoundationThing<T>> {
        abstract T name2();
    }

    @Test
    public void testResolvingGenericTypeComplex() {
        resolvedGenericTypeTest(Thing.class, "name2");
    }



    abstract class FooIf implements BaseFooIf<FooIf> {}

    interface BaseFooIf<T extends BaseFooIf<T>> {
        T name3();
    }

    @Test
    public void testResolvingGenericTypeInterfaceSimple() {
        resolvedGenericTypeTest(FooIf.class, "name3");
    }



    abstract class ThingIf implements BaseThingIf<ThingIf> {}

    interface BaseThingIf<P extends BaseThingIf<P>> extends BasementThingIf<P, P> {}

    interface BasementThingIf<K extends V, V extends BasementThingIf<K, V>>
            extends FoundationThingIf<K> {}

    interface FoundationThingIf<T extends FoundationThingIf<T>> {
        T name4();
    }

    @Test
    public void testResolvingGenericTypeInterfaceComplex() {
        resolvedGenericTypeTest(ThingIf.class, "name3");
    }



    abstract class ThingCombo extends BaseThing<ThingCombo>
            implements BaseFooIf<ThingCombo>, BaseThingIf<ThingCombo> {}

    @Test
    public void testResolvingGenericTypeCombined() {
        resolvedGenericTypeTest(ThingCombo.class, "name2");
        resolvedGenericTypeTest(ThingCombo.class, "name3");
        resolvedGenericTypeTest(ThingCombo.class, "name4");
    }
}
