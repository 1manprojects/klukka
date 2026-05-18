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

import de.OneManProjects.data.*;
import de.OneManProjects.data.dto.*;
import de.OneManProjects.data.enums.RefType;
import de.OneManProjects.data.enums.Role;
import de.OneManProjects.database.Groups;
import de.OneManProjects.database.Projects;
import de.OneManProjects.database.Tokens;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Users {

    @OpenApi(
            summary = "Delete User Account",
            tags = {"User"},
            operationId = "user delete",
            path = "/api/user/delete",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN"),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void deleteAccount(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        Responses.setResponseOrError(ctx, Projects.deleteAllUserProjects(userId) && de.OneManProjects.database.Users.deleteUser(userId));
    }

    @OpenApi(
            summary = "Create User Token",
            tags = {"User"},
            operationId = "user createToken",
            path = "/api/user/createToken",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = UserApiToken.class)},
                    description = "UserApiToken Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = StringResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "500", description = "INTERNAL_SERVER_ERROR")
            }
    )
    public static void createUserApiToken(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final UserApiToken userApiToken = ctx.bodyAsClass(UserApiToken.class);
        for (int i = 0; i < 5; i++) {
            try {
                final String apiToken = Auth.generateApiToken();
                if (Tokens.addUserApiToken(userId, apiToken, userApiToken.description(), userApiToken.expiration())) {
                    Responses.setResponseOrError(ctx, apiToken);
                    return;
                }
            } catch (final SQLException e) {
                if (isUniqueConstraintViolation(e)) {
                    continue;
                }
                throw e;
            }
        }
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static boolean isUniqueConstraintViolation(final SQLException e) {
        return e.getSQLState().startsWith("23");
    }

    @OpenApi(
            summary = "Get User Tokens",
            tags = {"User"},
            operationId = "user getTokens",
            path = "/api/user/listTokens",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserApiTokenResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getUserTokens(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<UserApiToken> tokens = Tokens.getUserApiTokens(userId);
        Responses.setResponseOrError(ctx, tokens);
    }

    @OpenApi(
            summary = "Delete User Token",
            tags = {"User"},
            operationId = "user deleteToken",
            path = "/api/user/deleteToken",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Integer.class)},
                    description = "Token ID to delete",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void deleteToken(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final int tokenId = ctx.bodyAsClass(Integer.class);
        final boolean res = Tokens.deleteUserToken(userId, tokenId);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Update User Email",
            tags = {"User"},
            operationId = "user updateMail",
            path = "/api/user/changeMail",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = String.class)},
                    description = "New email address",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void updateUserMail(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final String newMail = ctx.bodyAsClass(String.class);
        Responses.setResponseOrError(ctx, de.OneManProjects.database.Users.updateUserMail(userId, newMail));
    }

    @OpenApi(
            summary = "Get User Data",
            tags = {"User"},
            operationId = "user getData",
            path = "/api/user/data",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserDataResponse.class)),
                    @OpenApiResponse(status = "204", description = "NO_CONTENT"),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getUserData(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Project> projects = Projects.getProjects(userId, true);
        final Optional<User> user = de.OneManProjects.database.Users.getUserInfo(userId);
        final List<Group> groups = Groups.getUserGroups(userId);
        final List<UserApiToken> tokens = Tokens.getUserApiTokens(userId);
        if (user.isPresent()) {
            Responses.setResponseOrError(ctx, new UserData(user.get(), projects, groups, tokens));
        } else {
            ctx.status(HttpStatus.NO_CONTENT);
        }
    }

    public static String getExportFilename(final DataFilter df) {
        final Instant start = Instant.parse(df.start());
        final Instant end = Instant.parse(df.start());
        final LocalDate date1 = start.atZone(ZoneId.systemDefault()).toLocalDate();
        final LocalDate date2 = end.atZone(ZoneId.systemDefault()).toLocalDate();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
        return "Export-" + formatter.format(date1) + "-" + formatter.format(date2) + ".csv";
    }

    @OpenApi(
            summary = "Export User Data",
            tags = {"User"},
            operationId = "user exportData",
            path = "/api/export",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
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
    public static void exportData(final Context ctx) throws SQLException, IllegalAccessException {
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
            tags = {"User"},
            operationId = "user archiveProject",
            path = "/api/archive",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = ArchiveId.class)},
                    description = "ArchiveId object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void archiveProject(final Context ctx) throws SQLException, IllegalAccessException {
        final ArchiveId archiveId = ctx.bodyAsClass(ArchiveId.class);
        final int userID = Auth.getUserFromContext(ctx);
        boolean res = false;
        if (Projects.canUserManageProject(userID, archiveId.projectId())) {
            res = Projects.setProjectArchive(archiveId.projectId(), archiveId.archive());
        }
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Update Tracking",
            tags = {"User"},
            operationId = "user updateTracking",
            path = "/api/update",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Tracked.class)},
                    description = "Tracked object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void updateTracking(final Context ctx) throws SQLException, IllegalAccessException {
        final Tracked tracked = ctx.bodyAsClass(Tracked.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = Projects.updateTracking(tracked, userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Update Comment",
            tags = {"User"},
            operationId = "user updateComment",
            path = "/api/comment",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = CommentUpdate.class)},
                    description = "Comment Update object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void updateComment(final Context ctx) throws SQLException, IllegalAccessException {
        final CommentUpdate tracked = ctx.bodyAsClass(CommentUpdate.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = Projects.updateComment(tracked.id(), userID, tracked.comment());
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Get Data to Analyse",
            tags = {"User"},
            operationId = "user getDataToAnalyse",
            path = "/api/data",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = DataFilter.class)},
                    description = "DataFilter object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = AnalysisDataResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getDataToAnalyse(final Context ctx) throws SQLException, IllegalAccessException {
        final DataFilter filter = ctx.bodyAsClass(DataFilter.class);
        final int userId = Auth.getUserFromContext(ctx);
        if (userId > -1) {
            final List<Project> userProjects = Projects.getProjects(userId, true);
            final List<Project> groupProjects = Projects.getUserGroupProjects(userId);
            final List<Tracked> tracked = Projects.getTrackedForRange(userId, Instant.parse(filter.start()), Instant.parse(filter.end()));
            Responses.setResponseOrError(ctx, new AnalysisData(userProjects, groupProjects, tracked));
        }
    }

    @OpenApi(
            summary = "Delete Tracking Entry",
            tags = {"User"},
            operationId = "user deleteTracking",
            path = "/api/delete",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Integer.class)},
                    description = "Tracking ID to delete",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void deleteTracking(final Context ctx) throws SQLException, IllegalAccessException {
        final int id = ctx.bodyAsClass(Integer.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = Projects.deleteTracking(id, userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Delete User Project",
            tags = {"User"},
            operationId = "user deleteProject",
            path = "/api/deleteProject",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Integer.class)},
                    description = "Project ID to delete",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void deleteUserProject(final Context ctx) throws SQLException, IllegalAccessException {
        final int idToDel = ctx.bodyAsClass(Integer.class);
        final int userId = Auth.getUserFromContext(ctx);
        final Optional<Project> p = Projects.getProjectById(idToDel);
        boolean res = false;
        if (p.isPresent() && p.get().getRefType().equals(RefType.USER) && p.get().getRef() == userId) {
            res = Projects.deleteProject(userId, idToDel);
        }
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Get User Role",
            tags = {"User"},
            operationId = "user getRole",
            path = "/api/role",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = RoleResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getUserRole(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Role> roles = de.OneManProjects.database.Users.getUserRoles(userId);
        Responses.setResponseOrError(ctx, Role.getHighestRole(roles));
    }

    @OpenApi(
            summary = "Get User Projects",
            tags = {"User"},
            operationId = "user getProjects",
            path = "/api/projects",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserProjectsResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getUserProjects(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Project> userProjects = Projects.getProjects(userId, false);
        final List<Project> groupProjects = Projects.getUserGroupProjects(userId);
        Responses.setResponseOrError(ctx, new UserProjects(userProjects, groupProjects));
    }

    @OpenApi(
            summary = "Update Project",
            tags = {"User"},
            operationId = "user updateProject",
            path = "/api/edit",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Project.class)},
                    description = "Project object to update",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN")
            }
    )
    public static void updateProject(final Context ctx) throws SQLException, IllegalAccessException {
        final Project toUpdate = ctx.bodyAsClass(Project.class);
        final int userId = Auth.getUserFromContext(ctx);
        if (Projects.canUserManageProject(userId, toUpdate.getId())) {
            Responses.setResponseOrError(ctx, Projects.updateProjects(toUpdate));
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
            summary = "Get User Archived Projects",
            tags = {"User"},
            operationId = "user getArchivedProjects",
            path = "/api/archived",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = UserProjectsResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getUserArchivedProjects(final Context ctx) throws SQLException, IllegalAccessException {
        final int userId = Auth.getUserFromContext(ctx);
        final List<Project> userProjects = Projects.getArchived(userId);
        Responses.setResponseOrError(ctx, new UserProjects(userProjects, new ArrayList<>()));
    }

    @OpenApi(
            summary = "Add Personal Project",
            tags = {"User"},
            operationId = "user addPersonalProject",
            path = "/api/add",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Project.class)},
                    description = "Project object to add",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void addPersonalProject(final Context ctx) throws SQLException, IllegalAccessException {
        final Project project = ctx.bodyAsClass(Project.class);
        final int userId = Auth.getUserFromContext(ctx);
        Responses.setResponseOrError(ctx, Projects.addProject(project, userId));
    }

    @OpenApi(
            summary = "Start Tracking Project",
            tags = {"User"},
            operationId = "start",
            path = "/api/start",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Start.class)},
                    description = "Start Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = IntegerResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN"),
            }
    )
    public static void startTracking(final Context ctx) throws SQLException, IllegalAccessException {
        final Start start = ctx.bodyAsClass(Start.class);
        final int userID = Auth.getUserFromContext(ctx);
        final Optional<Project> p = Projects.getProjectById(start.projectID());
        if (p.isPresent()) {
            final Optional<Tracked> current = Projects.getActiveTracking(userID);
            if (current.isEmpty()) {
                final int res = Projects.addTracking(new Tracked(
                        -1, userID, start.projectID(), Timestamp.from(Instant.now()), start.timeZone()
                ));
                Responses.setResponseOrError(ctx, res > 0);
            } else {
                Responses.setResponseOrError(ctx, current.get().getId());
            }
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
            ctx.json(false);
        }
    }

    @OpenApi(
            summary = "Get Active Tracking",
            tags = {"User"},
            operationId = "user getActiveTracking",
            path = "/api/active",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = TrackedResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getActive(final Context ctx) throws SQLException, IllegalAccessException {
        final int userID = Auth.getUserFromContext(ctx);
        final Optional<Tracked> res = Projects.getActiveTracking(userID);
        Responses.setResponseOrError(ctx, res, true);
    }

    @OpenApi(
            summary = "Get Tracked Minutes This Month",
            tags = {"User"},
            operationId = "user getMonth",
            path = "/api/month",
            methods = HttpMethod.GET,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = DoubleResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void getMonth(final Context ctx) throws SQLException, IllegalAccessException {
        final int userID = Auth.getUserFromContext(ctx);
        final double res = Projects.getTrackedMinutesThisMonth(userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Stop Tracking",
            tags = {"User"},
            operationId = "user stopTracking",
            path = "/api/stop",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Integer.class)},
                    description = "Tracking ID to stop",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void stopTracking(final Context ctx) throws SQLException, IllegalAccessException {// throws SQLException {
        final int userID = Auth.getUserFromContext(ctx);
        final Integer id = ctx.bodyAsClass(Integer.class);
        final boolean res = Projects.stopTracking(id, userID);
        Responses.setResponseOrError(ctx, res);
    }

    @OpenApi(
            summary = "Update Password",
            tags = {"User"},
            operationId = "user updatePassword",
            path = "/api/user/updatePassword",
            methods = HttpMethod.POST,
            security = {
                    @OpenApiSecurity(name = "jwtCookie"),
                    @OpenApiSecurity(name = "Authorization")
            },
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = String.class)},
                    description = "New password",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = BooleanResponse.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED")
            }
    )
    public static void updatePassword(final Context ctx) throws SQLException, IllegalAccessException {
        final String newPass = ctx.bodyAsClass(String.class);
        final int userID = Auth.getUserFromContext(ctx);
        final boolean res = de.OneManProjects.database.Users.updatePassword(userID, Auth.hashPassword(newPass));
        Responses.setResponseOrError(ctx, res);
    }
}