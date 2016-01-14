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
import java.util.Collections;
import javax.tools.JavaFileObject;
import org.junit.Test;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public final class AutoValueWithExtensionTest {


  @Test public void simple() {
    JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
            + "package test;\n"
            + "import com.google.auto.value.AutoValue;\n"
            + "@AutoValue public abstract class Test {\n"
            + "public abstract String a();\n"
            + "public abstract String b();\n"
            + "public abstract int c();\n"
            + "public abstract int d();\n"
            + "public abstract Integer e();\n"
            + "public abstract Integer f();\n"
            + "public abstract Test withA(String a);"
            + "public abstract Test withC(int c);"
            + "public abstract Test withE(Integer e);"
            + "}\n"
    );

    JavaFileObject expectedSource = JavaFileObjects.forSourceString("test/AutoValue_Test", ""
            + "package test;\n"
            + "import java.lang.Integer;\n"
            + "import java.lang.Override;\n"
            + "import java.lang.String;\n"
            + "final class AutoValue_Test extends $AutoValue_Test {\n"
            + "  AutoValue_Test(String a, String b, int c, int d, Integer e, Integer f) {\n"
            + "    super(a, b, c, d, e, f);\n"
            + "  }\n"
            + "  @Override public Test withA(String a) {\n"
            + "    return new AutoValue_Test(a, b(), c(), d(), e(), f());\n"
            + "  }\n"
            + "  @Override public Test withC(int c) {\n"
            + "    return new AutoValue_Test(a(), b(), c, d(), e(), f());\n"
            + "  }\n"
            + "  @Override public Test withE(Integer e) {\n"
            + "    return new AutoValue_Test(a(), b(), c(), d(), e, f());\n"
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