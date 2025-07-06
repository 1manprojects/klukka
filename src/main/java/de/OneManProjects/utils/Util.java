package de.OneManProjects.utils;

/*-
 * #%L
 * Klukka
 * %%
 * Copyright (C) 2025 Nikolai Reed reed@1manprojects.de
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import de.OneManProjects.Main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

public class Util {
    public static <T> Optional<T> getEnvVar(final String envVarName, final Function<String, T> parser, final boolean required) {
        final Optional<T> res = Optional.ofNullable(System.getenv(envVarName)).map(parser);
        if (required) {
            if (res.isEmpty()) {
                throw new IllegalStateException("ENV VAR " + envVarName + " is not defined but is required");
            }
        }
        return res;
    }

    public static String getVersionInfo() {
        final Properties props = new Properties();
        try (final InputStream input = Main.class.getResourceAsStream("/version.properties")) {
            if (input != null) {
                props.load(input);
                return props.getProperty("version");
            }
        } catch (final IOException e) {
            return "N/A";
        }
        return "N/A";
    }
}
