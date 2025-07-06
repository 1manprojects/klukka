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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {

    private final TypeAdapter<T> valueAdapter;

    public OptionalTypeAdapter(final TypeAdapter<T> valueAdapter) {
        this.valueAdapter = valueAdapter;
    }

    @Override
    public void write(final JsonWriter out, final Optional<T> value) throws IOException {
        if (value.isEmpty()) {
            out.nullValue();
        } else {
            valueAdapter.write(out, value.get());
        }
    }

    @Override
    public Optional<T> read(final JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return Optional.empty();
        }
        return Optional.ofNullable(valueAdapter.read(in));
    }

    public static class Factory implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        @Override
        public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {
            if (!Optional.class.equals(typeToken.getRawType())) {
                return null;
            }

            // Get Optional<T> inner type safely
            final Type type = typeToken.getType();
            if (!(type instanceof final ParameterizedType parameterizedType)) {
                return null; // Raw Optional<?> or unknown generic type
            }

            final Type innerType = parameterizedType.getActualTypeArguments()[0];
            final TypeAdapter<?> innerAdapter = gson.getAdapter(TypeToken.get(innerType));

            // Wrap in OptionalTypeAdapter and cast safely
            return (TypeAdapter<T>) new OptionalTypeAdapter<>(innerAdapter).nullSafe();
        }
    }
}
