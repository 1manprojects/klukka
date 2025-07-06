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
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.OneManProjects.Main;
import de.OneManProjects.data.dto.Deps;
import de.OneManProjects.data.dto.UserApiToken;
import de.OneManProjects.data.enums.Role;
import de.OneManProjects.utils.OptionalTypeAdapter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MainTests {

    @Test
    void testRoleDif() {
        final List<Role> oldRoles = List.of(Role.ADMIN, Role.USER);
        final List<Role> newRoles = List.of(Role.ADMIN, Role.GROUP);

        final List<Role> toDel = oldRoles.stream().filter(o -> !newRoles.contains(o)).toList();
        final List<Role> toAdd = newRoles.stream().filter(n -> !oldRoles.contains(n)).toList();

        assertTrue(toDel.contains(Role.USER));
        assertTrue(toAdd.contains(Role.GROUP));
    }

    @Test
    void testOptionalJson() {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        new TypeToken<Optional<Timestamp>>() {
                        }.getType(),
                        new OptionalTypeAdapter<>(new Gson().getAdapter(Timestamp.class))
                )
                .create();

        final String json = "{\"id\":-1,\"description\":\"asdfasdf\",\"expiration\":null}";

        final UserApiToken token = gson.fromJson(json, UserApiToken.class);
        assertTrue(token.expiration().isEmpty());
    }

    @Test
    void testLoadFrontendDeps() throws IOException {
        final var original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        final List<Deps> deps = Main.loadFrontendDeps();
        assertFalse(deps.isEmpty());
        assertEquals(12, deps.size());
        assertEquals("18.3.5", deps.get(0).version());
        assertEquals("types/react-dom", deps.get(0).name());
        assertEquals("MIT", deps.get(0).license());
        Thread.currentThread().setContextClassLoader(original);
    }

    @Test
    void testLoadBackendDeps() throws IOException {
        final var original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        final List<Deps> deps = Main.loadBackendDeps();
        assertFalse(deps.isEmpty());
        assertEquals(4, deps.size());
        assertEquals("javalin", deps.get(0).name());
        assertEquals("6.6.0", deps.get(0).version());
        assertEquals("The Apache Software License, Version 2.0", deps.get(0).license());

        Thread.currentThread().setContextClassLoader(original);
    }
}
