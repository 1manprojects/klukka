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
import de.OneManProjects.data.Group;
import de.OneManProjects.data.Project;
import de.OneManProjects.data.Tracked;
import de.OneManProjects.data.User;
import de.OneManProjects.data.dto.*;
import de.OneManProjects.export.Exporter;
import de.OneManProjects.mail.Mail;
import de.OneManProjects.security.Auth;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.mail.MessagingException;
import io.javalin.openapi.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Groups {

    @OpenApi(
        summary = "Get Group Details",
        operationId = "group getDetails",
        path = "/api/group/details",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Group ID",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = GroupDetails.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void getGroupDetails(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userId = Auth.getUserFromContext(ctx);
            final int groupId = ctx.bodyAsClass(Integer.class);
            final Optional<de.OneManProjects.data.Group> group = Database.getGroup(groupId, userId);
            if (group.isPresent()) {
                final List<User> users = Database.getUsersInGroup(groupId);
                final List<Project> projects = Database.getProjectsFromGroup(groupId, true);
                Responses.setResponseOrError(ctx, new GroupDetails(group.get(), users, projects));
            } else {
                ctx.status(HttpStatus.FORBIDDEN);
            }
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    private static String getExportFilename(final DataFilter df, final String groupName) {
        final Instant start = Instant.parse(df.start());
        final Instant end = Instant.parse(df.start());
        final LocalDate date1 = start.atZone(ZoneId.systemDefault()).toLocalDate();
        final LocalDate date2 = end.atZone(ZoneId.systemDefault()).toLocalDate();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy");
        return "Export-" + groupName.replace(" ","_") + "-" + formatter.format(date1) + "-" + formatter.format(date2) + ".csv";
    }

    @OpenApi(
            summary = "Get Group Tracking Export",
            operationId = "group data export",
            path = "/api/group/export",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = ExportFilter.class)},
                    description = "Export filter with GroupID set",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = GroupDetails.class)),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN")
            }
    )
    public static void exportData(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final ExportFilter filter = ctx.bodyAsClass(ExportFilter.class);
            if (filter.groupId().isPresent()) {
                final int userId = Auth.getUserFromContext(ctx);
                final Optional<Group> group = Database.getGroup(filter.groupId().get(), userId);
                if (group.isPresent()) {
                    final byte[] data = Exporter.exportGroupData(filter, group.get().getId());
                    final String fileName = getExportFilename(filter.filter(), group.get().getTitle());
                    ctx.header("export", fileName);
                    ctx.header("Content-Disposition", "attachment; filename=" + fileName);
                    ctx.contentType("text/csv");
                    ctx.result(data);
                }
            }
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
            summary = "Get Group data for Analysis",
            operationId = "group data for Analysis",
            path = "/api/group/data",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = DataFilter.class)},
                    description = "Data filter with GroupID set",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = GroupDetails.class)),
                    @OpenApiResponse(status = "403", description = "FORBIDDEN")
            }
    )
    public static void getGroupDataToAnalyse(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userId = Auth.getUserFromContext(ctx);
            final DataFilter filter = ctx.bodyAsClass(DataFilter.class);
            if (filter.groupId().isPresent()) {
                final Optional<de.OneManProjects.data.Group> group = Database.getGroup(filter.groupId().get(), userId);
                if (group.isPresent() && group.get().getOwner() == userId) {
                    final List<Project> groupProjects = Database.getGroupProjects(filter.groupId().get(), true);
                    final List<Integer> groupProjectIds = groupProjects.stream().map(Project::getId).toList();
                    final List<Tracked> tracked = Database.getGroupTrackedForRange(groupProjectIds, Instant.parse(filter.start()), Instant.parse(filter.end()));
                    Responses.setResponseOrError(ctx, new AnalysisData(new ArrayList<>(), groupProjects, tracked));
                }

            }
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Create New Group",
        operationId = "group create",
        path = "/api/group/create",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = de.OneManProjects.data.Group.class)},
            description = "Group object to create",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupUserCreateGroup(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final de.OneManProjects.data.Group newGroup = ctx.bodyAsClass(de.OneManProjects.data.Group.class);
            final boolean res = Database.addNewGroup(newGroup, userID);
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Delete Group",
        operationId = "group delete",
        path = "/api/group/delete",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Group ID to delete",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupDelete(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final int groupId = ctx.bodyAsClass(Integer.class);
            final boolean res = Database.deleteGroup(groupId, userID);
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    private static boolean canUserManageGroup(final int userID, final int groupId) throws SQLException {
        final Optional<de.OneManProjects.data.Group> group = Database.getGroup(groupId, userID);
        return group.isPresent();
    }

    @OpenApi(
        summary = "Invite User to Group",
        operationId = "group inviteUser",
        path = "/api/group/inviteUser",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = GroupToUser.class)},
            description = "GroupToUser object (groupId, mail)",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = String.class, example = "Invite sent | User does not exist | Error matching user to Group")),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupUserInvite(final Context ctx) throws SQLException, MessagingException, IOException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final GroupToUser groupToUser = ctx.bodyAsClass(GroupToUser.class);
            final Optional<de.OneManProjects.data.Group> group = Database.getGroup(groupToUser.groupId(), userID);
            if (group.isPresent()) {
                final Optional<Integer> id = Database.getUserID(groupToUser.mail());
                if (id.isPresent()) {
                    final boolean res = Database.addUserToGroup(groupToUser.groupId(), id.get());
                    final Optional<String> userMail = Database.getUserMail(id.get());
                    if (res && userMail.isPresent()) {
                        Mail.sendGroupInvite(userMail.get(), group.get().getTitle());
                        Responses.setResponseOrError(ctx, "Invite sent");
                        return;
                    }
                } else {
                    Responses.setResponseOrError(ctx, "User does not exist");
                    return;
                }
            }
            Responses.setResponseOrError(ctx, "Error matching user to Group");
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Remove User from Group",
        operationId = "group removeUser",
        path = "/api/group/removeUser",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = GroupToUser.class)},
            description = "GroupToUser object (groupId, mail)",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupUserRemove(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final GroupToUser groupToUser = ctx.bodyAsClass(GroupToUser.class);
            boolean res = false;
            if (canUserManageGroup(userID, groupToUser.groupId())) {
                final Optional<Integer> id = Database.getUserID(groupToUser.mail());
                if (id.isPresent()) {
                    res = Database.removeUserFromGroup(groupToUser.groupId(), id.get());
                }
            }
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Add Project to Group",
        operationId = "group addProject",
        path = "/api/group/addProject",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Project.class)},
            description = "Project object to add to group",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupAddProject(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final Project project = ctx.bodyAsClass(Project.class);
            boolean res = false;
            if (canUserManageGroup(userID, project.getRef())) {
                res = Database.addGroupProject(project);
            }
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Update Group",
        operationId = "group update",
        path = "/api/group/update",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = de.OneManProjects.data.Group.class)},
            description = "Group object to update",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupUpdate(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final de.OneManProjects.data.Group group = ctx.bodyAsClass(de.OneManProjects.data.Group.class);
            boolean res = false;
            if (canUserManageGroup(userID, group.getId())) {
                res = Database.updateGroup(group, userID);
            }
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Delete Project from Group",
        operationId = "group deleteProject",
        path = "/api/group/deleteProject",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = IdTupel.class)},
            description = "IdTupel object (groupId, projectId)",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void groupDeleteProject(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userID = Auth.getUserFromContext(ctx);
            final IdTupel tuple = ctx.bodyAsClass(IdTupel.class);
            boolean res = false;
            if (canUserManageGroup(userID, tuple.id1())) {
                res = Database.deleteProject(tuple.id2(), tuple.id1());
            }
            Responses.setResponseOrError(ctx, res);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Get Managed Groups",
        operationId = "group getManagedGroups",
        path = "/api/group/managedGroups",
        methods = HttpMethod.GET,
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = de.OneManProjects.data.Group.class)),
            @OpenApiResponse(status = "403", description = "FORBIDDEN")
        }
    )
    public static void getManagedGroups(final Context ctx) throws SQLException {
        if (Auth.isUserGroup(ctx)) {
            final int userId = Auth.getUserFromContext(ctx);
            final List<de.OneManProjects.data.Group> groups = Database.getManagedGroups(userId);
            Responses.setResponseOrError(ctx, groups);
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
        }
    }

    @OpenApi(
        summary = "Leave Group",
        operationId = "group leave",
        path = "/api/group/leave",
        methods = HttpMethod.POST,
        requestBody = @OpenApiRequestBody(
            content = {@OpenApiContent(from = Integer.class)},
            description = "Group ID to leave",
            required = true
        ),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class))
        }
    )
    public static void userLeaveGroup(final Context ctx) throws SQLException {
        final int groupId = ctx.bodyAsClass(Integer.class);
        final int userId = Auth.getUserFromContext(ctx);
        final boolean res = Database.leaveGroup(userId, groupId);
        Responses.setResponseOrError(ctx, res);
    }
}
