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
import de.OneManProjects.data.*;
import de.OneManProjects.data.dto.*;
import de.OneManProjects.data.enums.RefType;
import de.OneManProjects.data.enums.Role;
import de.OneManProjects.export.Exporter;
import de.OneManProjects.security.Auth;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class Users {

    @OpenApi(
            summary = "Delete User Account",
            operationId = "user delete",
            path = "/api/user/delete",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":23}")),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN"),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void deleteAccount(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        Responses.setResponseOrError(ctx, Database.deleteAllUserProjects(userId) && Database.deleteUser(userId));
    }

    @OpenApi(
            summary = "Create User Token",
            operationId = "user createToken",
            path = "/api/user/createToken",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = UserApiToken.class)},
                    description = "UserApiToken Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":\"Generated-Token\"}")),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "500", description = "INTERNAL_SERVER_ERROR")
            }
    )
    public static void createUserApiToken(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final UserApiToken userApiToken = ctx.bodyAsClass(UserApiToken.class);
        final String apiToken = Auth.generateApiToken();
        if (Database.addUserApiToken(userId, apiToken, userApiToken.description(), userApiToken.expiration())) {
            Responses.setResponseOrError(ctx, apiToken);
        } else {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @OpenApi(
        summary = "Get User Tokens",
        operationId = "user getTokens",
        path = "/api/user/tokens",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserApiToken.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getUserTokens(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<UserApiToken> tokens = Database.getUserApiTokens(userId);
        Responses.setResponseOrError(ctx, tokens);
    }

    @OpenApi(
        summary = "Delete User Token",
        operationId = "user deleteToken",
        path = "/api/user/deleteToken",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Token ID to delete",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void deleteToken(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final int tokenId = ctx.bodyAsClass(Integer.class);
        final boolean res = Database.deleteUserToken(userId, tokenId);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Update User Email",
        operationId = "user updateMail",
        path = "/api/user/updateMail",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = String.class)},
            description = "New email address",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void updateUserMail(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final String newMail = ctx.bodyAsClass(String.class);
        Responses.setResponseOrError(ctx, Database.updateUserMail(userId, newMail));
    }

    @OpenApi(
        summary = "Get User Data",
        operationId = "user getData",
        path = "/api/user/data",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserData.class)),
            @OpenApiResponse(status = "204", description = "NO_CONTENT"),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getUserData(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Project> projects = Database.getProjects(userId, true);
        final Optional<User> user = Database.getUserInfo(userId);
        final List<Group> groups = Database.getUserGroups(userId);
        final List<UserApiToken> tokens = Database.getUserApiTokens(userId);
        if (user.isPresent()) {
            Responses.setResponseOrError(ctx, new UserData(user.get(), projects, groups, tokens));
        } else {
            ctx.status(HttpStatus.NO_CONTENT);
        }
    }

    private static String getExportFilename(final DataFilter df) {
        final Instant start = Instant.parse(df.start());
        final Instant end = Instant.parse(df.start());
        final LocalDate date1 = start.atZone(ZoneId.systemDefault()).toLocalDate();
        final LocalDate date2 = end.atZone(ZoneId.systemDefault()).toLocalDate();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
        return "Export-" + formatter.format(date1) + "-" + formatter.format(date2) + ".csv";
    }

    @OpenApi(
        summary = "Export User Data",
        operationId = "user exportData",
        path = "/api/user/export",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = ExportFilter.class)},
            description = "Export filter",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/csv")),
            @OpenApiResponse(status = "503", description = "SERVICE_UNAVAILABLE")
        }
    )
    public static void exportData(final Context ctx) throws SQLException {
        final ExportFilter filter = ctx.bodyAsClass(ExportFilter.class);
        final int userId = Auth.getUserFromContext(ctx);
        if (userId > -1) {
            final byte[] data = Exporter.exportUserData(filter, userId);
            ctx.header("export", getExportFilename(filter.filter()));
            ctx.header("Content-Disposition", "attachment; filename=" + getExportFilename(filter.filter()));
            ctx.contentType("text/csv");
            ctx.result(data);
        } else {
            ctx.status(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @OpenApi(
        summary = "Archive Project",
        operationId = "user archiveProject",
        path = "/api/user/archiveProject",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = ArchiveId.class)},
            description = "ArchiveId object",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void archiveProject(final Context ctx) throws SQLException {
        final ArchiveId archiveId = ctx.bodyAsClass(ArchiveId.class);
        final int userID = Auth.getUserFromContext(ctx);
        boolean res = false;
        if (Database.canUserManageProject(userID, archiveId.projectId())) {
            res = Database.setProjectArchive(archiveId.projectId(), archiveId.archive());
        }
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Update Tracking",
        operationId = "user updateTracking",
        path = "/api/user/updateTracking",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Tracked.class)},
            description = "Tracked object",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void updateTracking(final Context ctx) throws SQLException {
        final Tracked tracked = ctx.bodyAsClass(Tracked.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = Database.updateTracking(tracked, userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Get Data to Analyse",
        operationId = "user getDataToAnalyse",
        path = "/api/user/dataToAnalyse",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = DataFilter.class)},
            description = "DataFilter object",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = AnalysisData.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getDataToAnalyse(final Context ctx) throws SQLException {
        final DataFilter filter = ctx.bodyAsClass(DataFilter.class);
        final int userId = Auth.getUserFromContext(ctx);
        if (userId > -1) {
            final List<Project> userProjects = Database.getProjects(userId, true);
            final List<Project> groupProjects = Database.getUserGroupProjects(userId);
            final List<Tracked> tracked = Database.getTrackedForRange(userId, Instant.parse(filter.start()), Instant.parse(filter.end()));
            Responses.setResponseOrError(ctx, new AnalysisData(userProjects, groupProjects, tracked));
        }
    }

    @OpenApi(
        summary = "Delete Tracking Entry",
        operationId = "user deleteTracking",
        path = "/api/user/deleteTracking",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Tracking ID to delete",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void deleteTracking(final Context ctx) throws SQLException {
        final int id = ctx.bodyAsClass(Integer.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = Database.deleteTracking(id, userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Delete User Project",
        operationId = "user deleteProject",
        path = "/api/user/deleteProject",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Project ID to delete",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void deleteUserProject(final Context ctx) throws SQLException {
        final int idToDel = ctx.bodyAsClass(Integer.class);
        final int userId = Auth.getUserFromContext(ctx);
        final Optional<Project> p = Database.getProjectById(idToDel);
        boolean res = false;
        if (p.isPresent() && p.get().getRefType().equals(RefType.USER) && p.get().getRef() == userId) {
            res =Database.deleteProject(userId, idToDel);
        }
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Get User Role",
        operationId = "user getRole",
        path = "/api/user/role",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Role.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getUserRole(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Role> roles = Database.getUserRoles(userId);
        Responses.setResponseOrError(ctx, Role.getHighestRole(roles));
    }

    @OpenApi(
        summary = "Get User Projects",
        operationId = "user getProjects",
        path = "/api/user/projects",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserProjects.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getUserProjects(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Project> userProjects = Database.getProjects(userId, false);
        final List<Project> groupProjects = Database.getUserGroupProjects(userId);
        Responses.setResponseOrError(ctx, new UserProjects(userProjects, groupProjects));
    }

    @OpenApi(
        summary = "Update Project",
        operationId = "user updateProject",
        path = "/api/user/updateProject",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Project.class)},
            description = "Project object to update",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void updateProject(final Context ctx) throws SQLException {
        final Project toUpdate = ctx.bodyAsClass(Project.class);
        final int userId = Auth.getUserFromContext(ctx);
        if (Database.canUserManageProject(userId, toUpdate.getId())) {
            Responses.setResponseOrError(ctx, Database.updateProjects(toUpdate));
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Get User Archived Projects",
        operationId = "user getArchivedProjects",
        path = "/api/user/archivedProjects",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserProjects.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getUserArchivedProjects(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Project> userProjects = Database.getProjects(userId, true);
        final List<Project> groupProjects = Database.getUserGroupProjects(userId);
        Responses.setResponseOrError(ctx, new UserProjects(userProjects, groupProjects));
    }

    @OpenApi(
        summary = "Add Personal Project",
        operationId = "user addPersonalProject",
        path = "/api/user/addPersonalProject",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Project.class)},
            description = "Project object to add",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void addPersonalProject(final Context ctx) throws SQLException {
        final Project project = ctx.bodyAsClass(Project.class);
        final int userId = Auth.getUserFromContext(ctx);
        Responses.setResponseOrError(ctx, Database.addProject(project, userId));
    }

    @OpenApi(
            summary = "Start Tracking Project",
            operationId = "start",
            path = "/api/start",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Start.class)},
                    description = "Start Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":23}")),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN"),
            }
    )
    public static void startTracking(final Context ctx) throws SQLException {
        final Start start = ctx.bodyAsClass(Start.class);
        final int userID = Auth.getUserFromContext(ctx);
        final Optional<Project> p = Database.getProjectById(start.getProjectID());
        if (p.isPresent()) {
            final int res = Database.addTracking(new Tracked(
                    -1, userID, start.getProjectID(), Timestamp.from(Instant.now()), start.getTimeZone()
            ));
            Responses.setResponseOrError(ctx, res > 0);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
            ctx.json(false);
        }
    }

    @OpenApi(
        summary = "Get Active Tracking",
        operationId = "user getActiveTracking",
        path = "/api/user/activeTracking",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Tracked.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getActive(final Context ctx) throws SQLException {
        final int userID = Auth.getUserFromContext(ctx);
        final Optional<Tracked> res = Database.getActiveTracking(userID);
        Responses.setResponseOrError(ctx, res, true);
    }

    @OpenApi(
        summary = "Get Tracked Minutes This Month",
        operationId = "user getMonth",
        path = "/api/user/month",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Double.class)),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void getMonth(final Context ctx) throws SQLException {
        final int userID = Auth.getUserFromContext(ctx);
        final double res = Database.getTrackedMinutesThisMonth(userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Stop Tracking",
        operationId = "user stopTracking",
        path = "/api/user/stopTracking",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Tracking ID to stop",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void stopTracking(final Context ctx) throws SQLException {// throws SQLException {
        final int userID = Auth.getUserFromContext(ctx);
        final Integer id = ctx.bodyAsClass(Integer.class);
        final boolean res = Database.stopTracking(id, userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
        summary = "Update Password",
        operationId = "user updatePassword",
        path = "/api/user/updatePassword",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = String.class)},
            description = "New password",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
            @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
        }
    )
    public static void updatePassword(final Context ctx) throws SQLException {
        final String newPass = ctx.bodyAsClass(String.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = Database.updatePassword(userID, Auth.hashPassword(newPass));
        Responses.setResponseOrError(ctx, res);
    }
}
