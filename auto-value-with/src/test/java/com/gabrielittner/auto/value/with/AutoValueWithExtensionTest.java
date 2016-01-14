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
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "import javax.annotation.Nonnull;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(String a, String b, int c, int d, String e, String f, String g, String h) {\n"
            + "    super(a, b, c, d, e, f, g, h);\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withA(String a) {\n"
            + "    return new AutoValue_Test(a, b(), c(), d(), e(), f(), g(), h());\n"
            + "  }\n"
            + "  @Override final AutoValue_Test withC(int c) {\n"
            + "    return new AutoValue_Test(a(), b(), c, d(), e(), f(), g(), h());\n"
            + "  }\n"
            + "  @Override public final AutoValue_Test withE(String e) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e, f(), g(), h());\n"
            + "  }\n"
            + "  @Override protected final AutoValue_Test withF(String f) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f, g(), h());\n"
            + "  }\n"
            + "  @Override @Nonnull final AutoValue_Test withG(String g) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g, h());\n"
            + "  }\n"
            + "  @Override @Nonnull public final AutoValue_Test withH(String h) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e(), f(), g(), h);\n"
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