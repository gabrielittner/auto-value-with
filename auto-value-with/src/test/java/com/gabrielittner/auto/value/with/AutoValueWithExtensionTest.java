package com.gabrielittner.auto.value.with;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.util.Arrays;
import java.util.Collections;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public final class AutoValueWithExtensionTest {

    @Test
    public void simple() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "import javax.annotation.Nonnull;\n"
                + "@AutoValue public abstract class Test {\n"
                // normal
                + "  public abstract String a();\n"
                + "  public abstract String b();\n"
                + "  abstract Test withA(String a);\n"
                // primitive
                + "  public abstract int c();\n"
                + "  public abstract int d();\n"
                + "  abstract Test withC(int c);\n"
                // public
                + "  public abstract String e();\n"
                + "  public abstract Test withE(String e);\n"
                // protected
                + "  public abstract String f();\n"
                + "  protected abstract Test withF(String f);\n"
                // with annotation
                + "  public abstract String g();\n"
                + "  @Nonnull abstract Test withG(String g);\n"
                // public with annotation
                + "  public abstract String h();\n"
                + "  @Nonnull public abstract Test withH(String h);\n"
                // property name starting with "with"
                + "  public abstract String withI();\n"
                + "  abstract Test withWithI(String withI);\n"
                // multiple properties
                + "  abstract Test withAC(String a, int c);\n"
                + "  abstract Test withBACD(String b, String a, int c, int d);\n"
                + "}\n");

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
                + "package test;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "import javax.annotation.Nonnull;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a, String b, int c, int d, String e, String f, String g, String h, String withI) {\n"
                + "    super(a, b, c, d, e, f, g, h, withI);\n"
                + "  }\n"
                + "  @Override final Test withA(String a) {\n"
                + "    return new AutoValue_Test(a, b(), c(), d(), e(), f(), g(), h(), withI());\n"
                + "  }\n"
                + "  @Override final Test withC(int c) {\n"
                + "    return new AutoValue_Test(a(), b(), c, d(), e(), f(), g(), h(), withI());\n"
                + "  }\n"
                + "  @Override public final Test withE(String e) {\n"
                + "    return new AutoValue_Test(a(), b(), c(), d(), e, f(), g(), h(), withI());\n"
                + "  }\n"
                + "  @Override protected final Test withF(String f) {\n"
                + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f, g(), h(), withI());\n"
                + "  }\n"
                + "  @Override @Nonnull final Test withG(String g) {\n"
                + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g, h(), withI());\n"
                + "  }\n"
                + "  @Override @Nonnull public final Test withH(String h) {\n"
                + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g(), h, withI());\n"
                + "  }\n"
                + "  @Override final Test withWithI(String withI) {\n"
                + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g(), h(), withI);\n"
                + "  }\n"
                + "  @Override final Test withAC(String a, int c) {\n"
                + "    return new AutoValue_Test(a, b(), c, d(), e(), f(), g(), h(), withI());\n"
                + "  }\n"
                + "  @Override final Test withBACD(String b, String a, int c, int d) {\n"
                + "    return new AutoValue_Test(a, b, c, d, e(), f(), g(), h(), withI());\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void returnsSuperType() {
        JavaFileObject source1 = JavaFileObjects.forSourceString("test.AbstractTest", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "public abstract class AbstractTest {\n"
                + "  public abstract String a();\n"
                + "  abstract AbstractTest withA(String a);\n"
                + "  public abstract String b();\n"
                + "  abstract AbstractTest withB(String B);\n"
                + "}\n");
        JavaFileObject source2 = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test extends AbstractTest {\n"
                + "  @Override abstract Test withB(String b);\n"
                + "}\n");

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
                + "package test;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override final Test withA(String a) {\n"
                + "    return new AutoValue_Test(a, b());\n"
                + "  }\n"
                + "  @Override final Test withB(String b) {\n"
                + "    return new AutoValue_Test(a(), b);\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Arrays.asList(source1, source2))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void returnsGenericSuperType() {
        JavaFileObject source1 = JavaFileObjects.forSourceString("test.AbstractTest", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "public abstract class AbstractTest<T extends AbstractTest<T>> {\n"
                + "  public abstract String a();\n"
                + "  abstract T withA(String a);\n"
                + "}\n");
        JavaFileObject source2 = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "  @AutoValue public abstract class Test extends AbstractTest<Test> {\n"
                + "}\n");

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
                + "package test;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a) {\n"
                + "    super(a);\n"
                + "  }\n"
                + "  @Override final Test withA(String a) {\n"
                + "    return new AutoValue_Test(a);\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Arrays.asList(source1, source2))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void wrongParameterName() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract String a();\n"
                + "  abstract Test withA(String b);\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Property \"b\" not found");
    }

    @Test
    public void wrongParameterType() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract String a();\n"
                + "  abstract Test withA(int a);\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Expected type java.lang.String for a");
    }

    @Test
    public void wrongReturnType() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract String a();\n"
                + "  abstract String withA(String a);\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .failsToCompile()
                .withErrorContaining("Expected test.Test as return type");
    }

    @Test
    public void dontImplementNonAbstractWithMethod() {
        //TODO AutoValue 1.3: we're getting only abstract methods from AV
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract String a();\n"
                + "  public abstract String b();\n"
                + "  abstract Test withA(String a);\n"
                + "  Test withB(String b) {\n"
                + "    return null;\n"
                + "  }\n"
                + "}\n");

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
                + "package test;\n"
                + "import java.lang.Override;\n"
                + "import java.lang.String;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(String a, String b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override final Test withA(String a) {\n"
                + "    return new AutoValue_Test(a, b());\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }

    @Test
    public void prefixedMethods() {
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import com.google.auto.value.AutoValue;\n"
                + "@AutoValue public abstract class Test {\n"
                + "  public abstract int getA();\n"
                + "  public abstract boolean isB();\n"
                + "  abstract Test withA(int a);\n"
                + "  abstract Test withB(boolean b);\n"
                + "}\n");

        JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
                + "package test;\n"
                + "import java.lang.Override;\n"
                + "final class AutoValue_Test extends $AutoValue_Test {\n"
                + "  AutoValue_Test(int a, boolean b) {\n"
                + "    super(a, b);\n"
                + "  }\n"
                + "  @Override final Test withA(int a) {\n"
                + "    return new AutoValue_Test(a, isB());\n"
                + "  }\n"
                + "  @Override final Test withB(boolean b) {\n"
                + "    return new AutoValue_Test(getA(), b);\n"
                + "  }\n"
                + "}\n");

        assertAbout(javaSources())
                .that(Collections.singletonList(source))
                .processedWith(new AutoValueProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedSource);
    }
}
