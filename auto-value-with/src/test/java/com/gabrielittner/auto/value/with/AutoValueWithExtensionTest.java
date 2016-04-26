/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gabrielittner.auto.value.with;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import javax.tools.JavaFileObject;
import java.util.Arrays;
import java.util.Collections;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public final class AutoValueWithExtensionTest {


  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "import javax.annotation.Nonnull;\n"
            + "@AutoValue public abstract class Test {\n"
            // normal
            + "public abstract String a();\n"
            + "public abstract String b();\n"
            + "abstract Test withA(String a);\n"
            // primitive
            + "public abstract int c();\n"
            + "public abstract int d();\n"
            + "abstract Test withC(int c);\n"
            // public
            + "public abstract String e();\n"
            + "public abstract Test withE(String e);\n"
            // protected
            + "public abstract String f();\n"
            + "protected abstract Test withF(String f);\n"
            // with annotation
            + "public abstract String g();\n"
            + "@Nonnull abstract Test withG(String g);\n"
            // public with annotation
            + "public abstract String h();\n"
            + "@Nonnull public abstract Test withH(String h);\n"
            // property name starting with "with"
            + "public abstract String withI();\n"
            + "abstract Test withWithI(String withI);\n"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "import javax.annotation.Nonnull;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(String a, String b, int c, int d, String e, String f, String g, String h, String withI) {\n"
            + "    super(a, b, c, d, e, f, g, h, withI);\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withA(String a) {\n"
            + "    return new AutoValue_Test(a, b(), c(), d(), e(), f(), g(), h(), withI());\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withC(int c) {\n"
            + "    return new AutoValue_Test(a(), b(), c, d(), e(), f(), g(), h(), withI());\n"
            + "  }\n"
            + "  @Override public final AutoValue_Test withE(String e) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e, f(), g(), h(), withI());\n"
            + "  }\n"
            + "  @Override protected final AutoValue_Test withF(String f) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f, g(), h(), withI());\n"
            + "  }\n"
            + "  @Override @Nonnull final AutoValue_Test withG(String g) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g, h(), withI());\n"
            + "  }\n"
            + "  @Override @Nonnull public final AutoValue_Test withH(String h) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g(), h, withI());\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withWithI(String withI) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g(), h(), withI);\n"
            + "  }\n"
            + "}\n"
    );

    assertAbout(javaSources())
        .that(Collections.singletonList(source))
        .processedWith(new AutoValueProcessor())
        .compilesWithoutError()
        .and()
        .generatesSources(expectedSource);
  }

  @Test public void returnsSuperType() {
    JavaFileObject source1 = JavaFileObjects.forSourceString("test.AbstractTest", ""
                    + "package test;\n"
                    + "import com.google.auto.value.AutoValue;\n"
                    + "public abstract class AbstractTest {\n"
                    + "public abstract String a();\n"
                    + "abstract AbstractTest withA(String a);"
                    + "}\n"
    );
    JavaFileObject source2 = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test extends AbstractTest {\n"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(String a) {\n"
            + "    super(a);\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withA(String a) {\n"
            + "    return new AutoValue_Test(a);\n"
            + "  }\n"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Arrays.asList(source1, source2))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource);
  }

  @Test public void genericClass() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test<T> {\n"
            + "public abstract T t();\n"
            + "abstract Test<T> withT(T t);"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "final class AutoValue_Test<T> extends $AutoValue_Test<T> {\n"
            + "  AutoValue_Test(T t) {\n"
            + "    super(t);\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withT(T t) {\n"
            + "    return new AutoValue_Test(t);\n"
            + "  }\n"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource);
  }

  @Test public void tooManyParameters() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "public abstract String a();\n"
            + "abstract Test withA(String a, String b);"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .failsToCompile()
            .withErrorContaining("Expected single argument of type java.lang.String for withA()");
  }

  @Test public void wrongMethodName() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "public abstract String a();\n"
            + "abstract Test withB(String b);"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .failsToCompile()
            .withErrorContaining("test.Test doesn't have property with name b which is required for withB()");
  }

  @Test public void wrongParameterType() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "public abstract String a();\n"
            + "abstract Test withA(int b);"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .failsToCompile()
            .withErrorContaining("Expected single argument of type java.lang.String for withA()");
  }

  @Test public void wrongReturnType() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "public abstract String a();\n"
            + "abstract String withA(String a);"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .failsToCompile()
            .withErrorContaining("withA() in test.Test returns java.lang.String, expected test.Test");
  }

  @Test public void dontImplementNonAbstractWithMethod() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "  public abstract String a();\n"
            + "  public abstract String b();\n"
            + "  abstract Test withA(String a);\n"
            + "  Test withB(String b) {"
            + "    return null;"
            + "  }\n"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(String a, String b) {\n"
            + "    super(a, b);\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withA(String a) {\n"
            + "    return new AutoValue_Test(a, b());\n"
            + "  }\n"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource);
  }

  @Test public void prefixedMethods() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "public abstract int getA();\n"
            + "public abstract boolean isB();\n"
            + "abstract Test withA(int a);"
            + "abstract Test withB(boolean b);"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(int a, boolean b) {\n"
            + "    super(a, b);\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withA(int a) {\n"
            + "    return new AutoValue_Test(a, isB());\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withB(boolean b) {\n"
            + "    return new AutoValue_Test(getA(), b);\n"
            + "  }\n"
            + "}\n"
    );

    assertAbout(javaSources())
            .that(Collections.singletonList(source))
            .processedWith(new AutoValueProcessor())
            .compilesWithoutError()
            .and()
            .generatesSources(expectedSource);
  }
}
