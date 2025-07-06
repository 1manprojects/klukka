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
import de.OneManProjects.security.Auth;
import de.OneManProjects.security.TokenType;
import de.OneManProjects.data.dto.Login;
import de.OneManProjects.data.enums.Role;
import de.OneManProjects.security.UserToken;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AuthTests {

    @Test
    void testHashPasswordAndVerify() {
        final String password = "mySecret123!";
        final String hash = Auth.hashPassword(password);
        assertNotNull(hash);
        assertTrue(at.favre.lib.crypto.bcrypt.BCrypt.verifyer().verify(password.toCharArray(), hash).verified);
    }

    @Test
    void testLoginSuccess() throws SQLException {
        final Login login = new Login("user@mail.com", "password");
        final String hash = Auth.hashPassword("password");

        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getUserHash("user@mail.com"))
                  .thenReturn(Optional.of(hash));
            assertTrue(Auth.login(login));
        }
    }

    @Test
    void testLoginFail() throws SQLException {
        final Login login = new Login("user@mail.com", "wrongpassword");
        final String hash = Auth.hashPassword("password");

        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getUserHash("user@mail.com"))
                  .thenReturn(Optional.of(hash));
            assertFalse(Auth.login(login));
        }
    }

    @Test
    void testGenJWTAndValidate() {
        final int userId = 42;
        final String jwt = Auth.genJWT(userId);
        assertNotNull(jwt);
        // Validate by decoding
        final var decoded = com.auth0.jwt.JWT.decode(jwt);
        assertEquals(userId, decoded.getClaim("user").asInt());
    }

    @Test
    void testValidateTokenWithCookie() throws SQLException {
        final Context ctx = mock(Context.class);
        when(ctx.cookieMap()).thenReturn(java.util.Map.of("jwt", "dummy"));
        when(ctx.cookie("jwt")).thenReturn(Auth.genJWT(1));

        assertTrue(Auth.validateToken(ctx));
    }

    @Test
    void testValidateTokenWithApiToken() throws SQLException {
        final Context ctx = mock(Context.class);
        when(ctx.cookieMap()).thenReturn(java.util.Map.of());
        when(ctx.header("Authorization")).thenReturn("Bearer testToken");

        final UserToken token = new UserToken("testToken", 0, Optional.empty(), TokenType.API_TOKEN, "testing");
        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getToken("testToken"))
                  .thenReturn(Optional.of(token));
            assertTrue(Auth.validateToken(ctx));
        }
    }

    @Test
    void testGetUserFromContextWithCookie() {
        final Context ctx = mock(Context.class);
        final String jwt = Auth.genJWT(123);
        when(ctx.cookieMap()).thenReturn(java.util.Map.of("jwt", "dummy"));
        when(ctx.cookie("jwt")).thenReturn(jwt);

        final int userId = Auth.getUserFromContext(ctx);
        assertEquals(123, userId);
    }

    @Test
    void testGetUserFromContextWithApiToken() throws SQLException {
        final Context ctx = mock(Context.class);
        when(ctx.cookieMap()).thenReturn(java.util.Map.of());
        when(ctx.header("Authorization")).thenReturn("Bearer testToken");

        final UserToken token = new UserToken("", 99, Optional.empty(), TokenType.API_TOKEN, "testing");
        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getToken("testToken"))
                  .thenReturn(Optional.of(token));
            final int userId = Auth.getUserFromContext(ctx);
            assertEquals(99, userId);
        }
    }

    @Test
    void testIsUserAdmin() throws SQLException {
        final Context ctx = mock(Context.class);
        final String jwt = Auth.genJWT(1);
        when(ctx.cookieMap()).thenReturn(java.util.Map.of("jwt", "dummy"));
        when(ctx.cookie("jwt")).thenReturn(jwt);

        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getUserRoles(1))
                  .thenReturn(List.of(Role.ADMIN, Role.USER));
            assertTrue(Auth.isUserAdmin(ctx));
        }
    }

    @Test
    void testIsUserGroup() throws SQLException {
        final Context ctx = mock(Context.class);
        final String jwt = Auth.genJWT(2);
        when(ctx.cookieMap()).thenReturn(java.util.Map.of("jwt", "dummy"));
        when(ctx.cookie("jwt")).thenReturn(jwt);

        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getUserRoles(2))
                  .thenReturn(List.of(Role.GROUP));
            assertTrue(Auth.isUserGroup(ctx));
        }
    }

    @Test
    void testGenerateApiToken() {
        try (final MockedStatic<de.OneManProjects.Database> dbMock = mockStatic(de.OneManProjects.Database.class)) {
            dbMock.when(() -> de.OneManProjects.Database.getToken(anyString()))
                  .thenReturn(Optional.empty());
            final String token = Auth.generateApiToken();
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }
    }

    @Test
    void testCreateRandomPassword() {
        final String password = Auth.createRandomPassword();
        assertNotNull(password);
        assertEquals(12, password.length());
        // Should contain at least one lowercase, one uppercase, one number, one symbol
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
        assertTrue(password.chars().anyMatch(c -> "^$*.[]{}()?-\"!@#%&/\\,><':;|_~`".indexOf(c) >= 0));
        for (int i= 0; i < 100; i++) {
            assertNotEquals(password, Auth.createRandomPassword());
        }
    }
}
