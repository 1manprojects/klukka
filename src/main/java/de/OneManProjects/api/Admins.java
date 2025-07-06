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

import de.OneManProjects.Database;
import de.OneManProjects.data.User;
import de.OneManProjects.data.dto.AdminData;
import de.OneManProjects.data.dto.PrivacyInfo;
import de.OneManProjects.mail.Mail;
import de.OneManProjects.security.Auth;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

public class Admins {

    public static final Path PRIVACY_HTML = Path.of("data/privacy.html");

    @OpenApi(
        summary = "Update User Roles",
        operationId = "admin updateRoles",
        path = "/api/admin/updateRoles",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = User.class)},
            description = "User object with updated roles",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class, example = "true")),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void adminUpdateRoles(final Context ctx) throws SQLException {
        if (Auth.isUserAdmin(ctx)) {
            final User user = ctx.bodyAsClass(User.class);
            final boolean res = Database.updateUserRole(user.id(), user.roles());
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Add New User",
        operationId = "admin addNewUser",
        path = "/api/admin/addNewUser",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = User.class)},
            description = "User object to add",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class, example = "true")),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void adminAddNewUser(final Context ctx) throws SQLException, MessagingException, IOException {
        if (Auth.isUserAdmin(ctx)) {
            final User newUser = ctx.bodyAsClass(User.class);
            final String p = Auth.createRandomPassword();
            final boolean res = Database.addNewUser(newUser, p);
            if (res) {
                Mail.sendInvite(newUser.mail(), p);
            }
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Delete User",
        operationId = "admin deleteUser",
        path = "/api/admin/deleteUser",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "User ID to delete",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class, example = "true")),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void adminDeleteUser(final Context ctx) throws SQLException {
        if (Auth.isUserAdmin(ctx)) {
            final int userToDel = ctx.bodyAsClass(Integer.class);
            final boolean res1 = Database.deleteUser(userToDel);
            final boolean res2 = Database.deleteAllUserProjects(userToDel);
            Responses.setResponseOrError(ctx, res1 && res2);
        }
        else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Get Admin Data",
        operationId = "admin getAdminData",
        path = "/api/admin/data",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = AdminData.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void getAdminData(final Context ctx) throws SQLException {
        if (Auth.isUserAdmin(ctx)) {
            Responses.setResponseOrError(ctx, Optional.of(new AdminData(Database.getAllUsers(), Database.getAllGroups(), new ArrayList<>())), false);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
            summary = "POST Set Privacy HTML",
            operationId = "admin setPrivacy",
            path = "/api/admin/setPrivacy",
            methods = HttpMethod.POST,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = String.class)),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN")
            }
    )
    public static void setPrivacyHtml(final Context ctx) throws IOException, SQLException{
        if (Auth.isUserAdmin(ctx)) {
            final PrivacyInfo toUpdate = ctx.bodyAsClass(PrivacyInfo.class);
            Files.writeString(PRIVACY_HTML, toUpdate.html(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Responses.setResponseOrError(ctx, true);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }
}
