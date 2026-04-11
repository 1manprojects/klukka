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

import de.OneManProjects.data.dto.Login;
import de.OneManProjects.data.dto.PasswordReset;
import de.OneManProjects.data.dto.Response;
import de.OneManProjects.database.Tokens;
import de.OneManProjects.mail.Mail;
import de.OneManProjects.security.Auth;
import de.OneManProjects.security.UserToken;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class Logins {
    @OpenApi(
            summary = "Refresh token from RefreshToken",
            tags = {"Authentication"},
            operationId = "refresh",
            path = "/api/refresh",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
            }
    )
    public static void refresh(final Context ctx) throws SQLException {
        final String refreshToken = ctx.cookie("refresh");
        final Optional<Integer> userID = Auth.validateRefreshToken(refreshToken);
        if (userID.isPresent()) {
            Tokens.deleteToken(refreshToken, userID.get());
            Auth.setCookies(ctx, userID.get());
            final Response response = new Response(true);
            ctx.json(response);
            ctx.status(HttpStatus.OK);
        } else {
            ctx.status(HttpStatus.UNAUTHORIZED);
        }
    }

    @OpenApi(
            summary = "Logout",
            tags = {"Authentication"},
            operationId = "logout",
            path = "/api/logout",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
            }
    )
    public static void logout(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        ctx.removeCookie("jwt", "/api");
        ctx.removeCookie("refresh", "/api");
        Tokens.deleteAllRefreshTokensForUser(userId);
        ctx.status(200).result("Logged out");
    }

    @OpenApi(
            summary = "Login",
            tags = {"Authentication"},
            operationId = "login",
            path = "/api/login",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Login.class)},
                    description = "Login Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "406", description = "NOT_ACCEPTABLE")
            }
    )
    public static void login(final Context ctx) throws SQLException {
        final Login login = ctx.bodyAsClass(Login.class);
        if (Auth.login(login)) {
            final Optional<Integer> userID = de.OneManProjects.database.Users.getUserID(login.mail());
            if (userID.isPresent()) {
                Auth.setCookies(ctx, userID.get());
                final Response response = new Response(true);
                ctx.status(HttpStatus.OK);
                ctx.json(response);
            } else {
                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            ctx.status(HttpStatus.UNAUTHORIZED);
        }
    }

    @OpenApi(
            summary = "Request Password reset",
            tags = {"Authentication"},
            operationId = "login/reset",
            path = "/api/login/reset",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = String.class, example = "user@mail.com")},
                    description = "Email String",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "204", description = "NO_CONTENT")
            }
    )
    public static void sendResetPasswordLink(final Context ctx) throws SQLException, MessagingException, IOException {
        final String mail = ctx.bodyAsClass(String.class);
        final Optional<Integer> userId = de.OneManProjects.database.Users.getUserID(mail);
        if (userId.isPresent()) {
            final String token = UUID.randomUUID().toString();
            final boolean result = Tokens.resetPasswordToken(token, userId.get());
            Mail.sendPasswordReset(mail, token);
            Responses.setResponseOrError(ctx, result);
        }
    }

    @OpenApi(
            summary = "Validate Token for Password reset",
            tags = {"Authentication"},
            operationId = "login/check",
            path = "/api/login/check",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = String.class, example = "PASSWORD_RESET_TOKEN")},
                    description = "Password Reset Token String",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = Response.class, example = "{\"payload\":false}")),
                    @OpenApiResponse(status = "204", description = "NO_CONTENT")
            }
    )
    public static void validToken(final Context ctx) throws SQLException {
        final String token = ctx.bodyAsClass(String.class);
        final Optional<UserToken> userToken = Tokens.getToken(token);
        if (userToken.isPresent()) {
            Responses.setResponseOrError(ctx, true);
        } else {
            Responses.setResponseOrError(ctx, false);
        }
    }

    @OpenApi(
            summary = "Reset User Password",
            tags = {"Authentication"},
            operationId = "login/token",
            path = "/api/login/token",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = PasswordReset.class)},
                    description = "Password Reset Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "404", description = "NOT_FOUND"),
                    @OpenApiResponse(status = "406", description = "NOT_ACCEPTABLE")
            }
    )
    public static void resetPasswordByToken(final Context ctx) throws SQLException {
        final PasswordReset reset = ctx.bodyAsClass(PasswordReset.class);
        final Optional<UserToken> userToken = Tokens.getToken(reset.token());
        if (userToken.isPresent() && userToken.get().expiration().isPresent() && userToken.get().expiration().get().after(Timestamp.from(Instant.now()))) {
            final Optional<String> userMail = de.OneManProjects.database.Users.getUserMail(userToken.get().user());
            if (userMail.isPresent()) {
                final boolean res = de.OneManProjects.database.Users.updatePassword(userToken.get().user(), Auth.hashPassword(reset.newPassword()));
                Responses.setResponseOrError(ctx, res);
            } else {
                ctx.status(HttpStatus.NOT_FOUND);
            }
        } else {
            ctx.status(HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @OpenApi(
            summary = "Validate if cookie is still valid",
            tags = {"Authentication"},
            operationId = "validate",
            path = "/api/validate",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
            }
    )
    public static void validate(final Context ctx) {
        Responses.setResponseOrError(ctx, true);
    }
}