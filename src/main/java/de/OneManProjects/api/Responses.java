package de.OneManProjects.api;

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

import de.OneManProjects.data.dto.Response;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Optional;

public class Responses {
    public static <T> void setResponseOrError(final Context ctx, final Optional<T> data, final boolean allowEmpty) {
        if (!allowEmpty && data.isEmpty()) {
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            final Response response = new Response(data.orElse(null));
            ctx.json(response);
        }
    }

    public static <T> void setResponseOrError(final Context ctx, final T data) {
        if (data == null) {
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            final Response response = new Response(data);
            ctx.json(response);
        }
    }

    public static void setBadRequest(final Context ctx, final String message) {
        ctx.status(HttpStatus.BAD_REQUEST);
        final Response response = new Response(message);
        ctx.json(response);
    }
}
