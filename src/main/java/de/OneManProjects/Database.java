package de.OneManProjects;

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

import de.OneManProjects.data.Group;
import de.OneManProjects.data.Project;
import de.OneManProjects.data.Tracked;
import de.OneManProjects.data.User;
import de.OneManProjects.data.dto.UserApiToken;
import de.OneManProjects.data.enums.RefType;
import de.OneManProjects.data.enums.Role;
import de.OneManProjects.security.Auth;
import de.OneManProjects.security.TokenType;
import de.OneManProjects.security.UserToken;
import de.OneManProjects.utils.Util;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Database {

    private static final String PROJECT_TABLE = "projects";
    private static final String TRACKING_TABLE = "tracking";
    private static final String ROLE_TABLE = "roles";
    private static final String USERS_TABLE = "users";
    private static final String GROUP_TABLE = "groups";
    private static final String GROUP_REF_TABLE = "groupRef";
    private static final String TOKEN_TABLE = "tokens";

    private static Connection getConnection() throws SQLException {
        final int dbPort = Util.getEnvVar("DATABASE_PORT", Integer::parseInt, true).orElseThrow(() -> new RuntimeException("DATABASE_PORT is not defined"));
        final String dbHost = Util.getEnvVar("DATABASE_HOST", s -> s, true).orElseThrow(() -> new RuntimeException("DATABASE_HOST  is not defined"));
        final String dbSecret = Util.getEnvVar("DATABASE_PASSWORD", s -> s, true).orElseThrow(() -> new RuntimeException("DATABASE_PASSWORD is not defined"));
        final String dbName = Util.getEnvVar("DATABASE_NAME", s -> s, true).orElseThrow(() -> new RuntimeException("DATABASE_NAME is not defined"));
        final String dbUser = Util.getEnvVar("DATABASE_USER", s -> s, true).orElseThrow(() -> new RuntimeException("DATABASE_USER is not defined"));
        final Optional<String> dbSsl = Util.getEnvVar("DATABASE_SSL", s -> s, false);

        final String url = String.format("jdbc:postgresql://%s:%d/%s", dbHost, dbPort, dbName);
        final Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbSecret);
        props.setProperty("ssl", dbSsl.orElse("false"));
        return DriverManager.getConnection(url, props);
    }

    private static Project parseProject(final ResultSet rs) throws SQLException {
        final int id = rs.getInt("id");
        final int ref = rs.getInt("ref");
        final String refType = rs.getString("refType");
        final double tracked = getUserTrackedMinutesForProject(id, ref);
        return new Project(id,
                ref,
                refType,
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getBoolean("archived"),
                tracked);
    }

    private static Project parseGroupProjectForUser(final ResultSet rs, final int userId) throws SQLException {
        final int id = rs.getInt("id");
        final int ref = rs.getInt("ref");
        final String refType = rs.getString("refType");
        final double tracked = getUserTrackedMinutesForProject(id, userId);
        return new Project(id,
                ref,
                refType,
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getBoolean("archived"),
                tracked);
    }

    private static UserToken parseToken(final ResultSet rs) throws SQLException {
        final Timestamp expiration = rs.getTimestamp("expiration");

        return new UserToken(
                rs.getString("token"),
                rs.getInt("idUser"),
                expiration != null ? Optional.of(expiration) : Optional.empty(),
                TokenType.valueOf(rs.getInt("token_type")),
                rs.getString("description")
        );
    }

    private static Tracked parseTracked(final ResultSet rs) throws SQLException {
        return new Tracked(rs.getInt("id"),
                rs.getInt("idUser"),
                rs.getInt("project"),
                rs.getTimestamp("start_time"),
                rs.getTimestamp("end_time"),
                rs.getString("timezone"),
                rs.getBoolean("active"));
    }

    private static User parseUser(final ResultSet rs) throws SQLException {
        final int userID = rs.getInt("id");
        return new User(
                userID,
                rs.getString("email"),
                getUserRoles(userID));
    }

    private static Group parseGroup(final ResultSet rs) throws SQLException {
        return new Group(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("owner")
        );
    }

    private static boolean isSuccess(final int[] batchUpdateResults) {
        return Arrays.stream(batchUpdateResults).allMatch(updateCount -> updateCount == 1);
    }

    private static Project getAllUserTrackedMinutesForGroupProject(final ResultSet rs) throws SQLException {
        final int id = rs.getInt("id");
        final int ref = rs.getInt("ref");
        final String refType = rs.getString("refType");
        final double tracked = getAllUserTrackedMinutesForGroupProject(id);
        return new Project(id,
                ref,
                refType,
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getBoolean("archived"),
                tracked);
    }

    public static List<Role> getUserRoles(final int userId) throws SQLException {
        final List<Role> roles = new ArrayList<>();
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT DISTINCT roleType FROM " + ROLE_TABLE + " WHERE idUser = ?")) {
                statement.setInt(1, userId);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    roles.add(Role.valueOf(rs.getString("roleType")));
                }
                return roles;
            }
        }
    }

    public static boolean leaveGroup(final int userId, final int groupId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM " + GROUP_REF_TABLE + " WHERE idUser = ? AND groupId = ?")) {
                statement.setInt(1, userId);
                statement.setInt(2, groupId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean updatePassword(final int userId, final String newHash) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE " + USERS_TABLE + " SET hash = ? WHERE id = ?")) {
                statement.setString(1, newHash);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean updateUserMail(final int userId, final String newMail) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE " + USERS_TABLE + " SET email = ? WHERE id = ?")) {
                statement.setString(1, newMail);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static List<User> getAllUsers() throws SQLException {
        final List<User> users = new ArrayList<>();
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT id, email FROM " + USERS_TABLE)) {
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    users.add(parseUser(rs));
                }
                return users;
            }
        }
    }

    public static List<Group> getAllGroups() throws SQLException {
        final List<Group> groups = new ArrayList<>();
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM " + GROUP_TABLE)) {
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    groups.add(parseGroup(rs));
                }
                return groups;
            }
        }
    }

    public static Optional<Integer> getUserID(final String mail) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT id FROM " + USERS_TABLE + " WHERE email = ?")) {
                statement.setString(1, mail);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(rs.getInt("id"));
                }
                return Optional.empty();
            }
        }
    }

    public static Optional<String> getUserMail(final int id) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT email FROM " + USERS_TABLE + " WHERE id = ?")) {
                statement.setInt(1, id);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(rs.getString("email"));
                }
                return Optional.empty();
            }
        }
    }

    public static Optional<String> getUserHash(final String mail) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT hash FROM "+ USERS_TABLE + " WHERE email = ?")) {
                statement.setString(1, mail);
                final ResultSet rs = statement.executeQuery();
                if(rs.next()) {
                    return Optional.of(rs.getString("hash"));
                }
                return Optional.empty();
            }
        }
    }

    public static List<Project> getProjects(final int refId, final boolean all) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT id,ref,refType,title,description,color,archived FROM "+ PROJECT_TABLE + " WHERE reftype = '" + RefType.USER + "' AND ref = ?" +
                    (!all ? " AND archived = false" : ""))) {
                statement.setInt(1, refId);
                final ResultSet rs = statement.executeQuery();
                final List<Project> res = new ArrayList<>();
                while (rs.next()) {
                    res.add(parseProject(rs));
                }
                return res;
            }
        }
    }

    public static List<Project> getGroupProjects(final int groupId, final boolean all) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT id,ref,refType,title,description,color,archived FROM "+ PROJECT_TABLE + " WHERE reftype = '" + RefType.GROUP + "' AND ref = ?" +
                    (!all ? " AND archived = false" : ""))) {
                statement.setInt(1, groupId);
                final ResultSet rs = statement.executeQuery();
                final List<Project> res = new ArrayList<>();
                while (rs.next()) {
                    res.add(parseProject(rs));
                }
                return res;
            }
        }
    }

    public static boolean setProjectArchive(final int projectId, final boolean value) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE "+ PROJECT_TABLE + " SET archived = ? WHERE id = ?")) {
                statement.setBoolean(1, value);
                statement.setInt(2, projectId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static List<Integer> getGroupIdForUser(final int userId) throws SQLException {
        final List<Integer> res = new ArrayList<>();
        try(final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT groupId FROM " + GROUP_REF_TABLE + " WHERE idUser = ?")) {
                statement.setInt(1, userId);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    res.add(rs.getInt(1));
                }
                return res;
            }
        }
    }

    public static boolean updateGroup(final Group newGroup, final int ownerId) throws SQLException {
        try(final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE " + GROUP_TABLE +" SET title = ?, description = ? WHERE id = ? AND owner = ?")) {
                statement.setString(1, newGroup.getTitle());
                statement.setString(2, newGroup.getDescription());
                statement.setInt(3, newGroup.getId());
                statement.setInt(4, ownerId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static List<User> getUsersInGroup(final int groupId) throws SQLException {
        final List<User> res = new ArrayList<>();
        try(final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT u.id, u.email FROM " + USERS_TABLE + " AS u JOIN " + GROUP_REF_TABLE + " AS g ON u.id = g.iduser WHERE g.groupId = ?")) {
                statement.setInt(1, groupId);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    res.add(new User(rs.getInt("id"), rs.getString("email"), new ArrayList<>()));
                }
                return res;
            }
        }
    }

    public static boolean updateProjects(final Project toUpdate) throws SQLException{
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE " + PROJECT_TABLE + " SET title = ?, description = ?, color = ? WHERE id = ?")) {
                statement.setString(1, toUpdate.getTitle());
                statement.setString(2, toUpdate.getDescription());
                statement.setString(3, toUpdate.getColor());
                statement.setInt(4, toUpdate.getId());
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static List<Project> getProjectsFromGroup(final int refId, final boolean all) throws SQLException {
        try (final Connection con = getConnection()) {
            final String sql = "SELECT * FROM "+ PROJECT_TABLE +" WHERE ref = ? AND reftype = '" + RefType.GROUP +"'" + (!all ? " AND archived = false" : "");
            try (final PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setInt(1, refId);
                final ResultSet rs = statement.executeQuery();
                final List<Project> res = new ArrayList<>();
                while (rs.next()) {
                    res.add(getAllUserTrackedMinutesForGroupProject(rs));
                }
                return res;
            }
        }
    }

    public static Optional<User> getUserInfo(final int id) throws SQLException {
        try (final Connection con = getConnection()) {
            final String sql = "SELECT * FROM "+ USERS_TABLE +" WHERE id = ?";
            try (final PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setInt(1, id);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(parseUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public static List<Group> getUserGroups(final int userId) throws SQLException {
        final List<Group> groups = new ArrayList<>();
        try (final Connection con = getConnection()) {
            final String sql = "SELECT * FROM "+ GROUP_TABLE + " g JOIN " + GROUP_REF_TABLE + " r ON g.id = r.groupId WHERE idUser = ?";
            try (final PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setInt(1, userId);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    groups.add(parseGroup(rs));
                }
            }
        }
        return groups;
    }

    public static boolean canUserManageProject(final int userID, final int projectID) throws SQLException {
        final Optional<Project> project = Database.getProjectById(projectID);
        if (project.isPresent()) {
            if (project.get().getRefType().equals(RefType.GROUP)) {
                final List<Group> groups = Database.getManagedGroups(userID);
                return groups.stream().anyMatch(g -> g.getId() == project.get().getRef());
            } else {
                return project.get().getRef() == userID;
            }
        }
        return false;
    }

    public static List<Project> getUserGroupProjects(final int refId) throws SQLException {
        final List<Integer> groupsUserIsIn = Database.getGroupIdForUser(refId);
        groupsUserIsIn.addAll(getManagedGroups(refId).stream().map(Group::getId).toList());
        if (!groupsUserIsIn.isEmpty()) {
            try (final Connection con = getConnection()) {
                final String sql = "SELECT * FROM " + PROJECT_TABLE + " WHERE reftype = '" + RefType.GROUP + "' AND ref IN (" + groupsUserIsIn.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
                try (final PreparedStatement statement = con.prepareStatement(sql)) {
                    for (int i = 0; i < groupsUserIsIn.size(); i++) {
                        statement.setInt((i + 1), groupsUserIsIn.get(i));
                    }
                    final ResultSet rs = statement.executeQuery();
                    final List<Project> res = new ArrayList<>();
                    while (rs.next()) {
                        res.add(parseGroupProjectForUser(rs, refId));
                    }
                    return res;
                }
            }
        }
        return new ArrayList<>();
    }

    public static List<Tracked> getTrackedForRange(final int userid, final Instant start, final Instant end) throws SQLException {
        final List<Tracked> res = new ArrayList<>();
        final Timestamp fromStart = Timestamp.from(start);
        final Timestamp fromEnd = Timestamp.from(end);
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM " + TRACKING_TABLE + " WHERE idUser = ? and start_time >= ? and end_time <= ?")) {
                statement.setInt(1, userid);
                statement.setTimestamp(2, fromStart);
                statement.setTimestamp(3, fromEnd);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    res.add(parseTracked(rs));
                }
                return res;
            }
        }
    }

    public static List<Tracked> getGroupTrackedForRange(final List<Integer> groupProjectIds, final Instant start, final Instant end) throws SQLException {
        final List<Tracked> res = new ArrayList<>();
        final Timestamp fromStart = Timestamp.from(start);
        final Timestamp fromEnd = Timestamp.from(end);
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM " + TRACKING_TABLE + " WHERE project in ("+ groupProjectIds.stream().map(id -> "?").collect(Collectors.joining(",")) + ") and start_time >= ? and end_time <= ?")) {
                int index = 0;
                for (int i = 0; i < groupProjectIds.size(); i++) {
                    statement.setInt((i + 1), groupProjectIds.get(i));
                    index++;
                }
                statement.setTimestamp(index + 1, fromStart);
                statement.setTimestamp(index + 2, fromEnd);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    res.add(parseTracked(rs));
                }
                return res;
            }
        }
    }

    public static double getTrackedMinutesThisMonth(final int userId) throws SQLException {
        double trackedMin = 0.0;
        final Timestamp limitStart = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1).atStartOfDay());
        final Timestamp limitEnd = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth().atStartOfDay());
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT EXTRACT(EPOCH FROM (end_time - start_time)) / 60 AS difference FROM "+ TRACKING_TABLE + " WHERE " +
                    "idUser = ? AND start_time > ? AND end_time < ?")) {
                statement.setInt(1, userId);
                statement.setTimestamp(2, limitStart);
                statement.setTimestamp(3, limitEnd);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    trackedMin += (rs.getDouble("difference"));
                }
                return trackedMin;
            }
        }
    }


    public static double getUserTrackedMinutesForProject(final int id, final int user) throws SQLException {
        double trackedMin = 0.0;
        final Timestamp limitStart = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1).atStartOfDay());
        final Timestamp limitEnd = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth().atStartOfDay());
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT EXTRACT(EPOCH FROM (end_time - start_time)) / 60 AS difference FROM "+ TRACKING_TABLE + " WHERE project = ? " +
                    "AND idUser = ? AND start_time > ? AND end_time < ?")) {
                statement.setInt(1,id);
                statement.setInt(2, user);
                statement.setTimestamp(3,limitStart);
                statement.setTimestamp(4,limitEnd);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    trackedMin += rs.getDouble("difference");
                }
                return trackedMin;
            }
        }
    }

    public static double getAllUserTrackedMinutesForGroupProject(final int id) throws SQLException {
        double trackedMin = 0;
        final Timestamp limitStart = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1).atStartOfDay());
        final Timestamp limitEnd = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth().atStartOfDay());
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT EXTRACT(EPOCH FROM (end_time - start_time)) / 60 AS difference FROM "+ TRACKING_TABLE + " WHERE project = ? " +
                    "AND start_time > ? AND end_time < ?")) {
                statement.setInt(1,id);
                statement.setTimestamp(2,limitStart);
                statement.setTimestamp(3,limitEnd);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    trackedMin= (rs.getLong("difference"));
                }
                return trackedMin;
            }
        }
    }

    public static Optional<Project> getProjectById(final int id) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT id,ref,refType,title,description,color,archived FROM "+ PROJECT_TABLE +" WHERE id = ?")) {
                statement.setInt(1, id);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(parseProject(rs));
                }
                return Optional.empty();
            }
        }
    }

    public static Optional<Tracked> getActiveTracking(final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM "+ TRACKING_TABLE +" WHERE active = true AND idUser = ?")) {
                statement.setInt(1, userId);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(parseTracked(rs));
                }
                return Optional.empty();
            }
        }
    }

    public static List<Group> getManagedGroups(final int ownerId) throws SQLException {
        final List<Group> groups = new ArrayList<>();
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM "+ GROUP_TABLE +" WHERE owner = ?")) {
                statement.setInt(1, ownerId);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    groups.add(parseGroup(rs));
                }
                return groups;
            }
        }
    }

    public static Optional<Group> getGroup(final int groupId, final int ownerId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM "+ GROUP_TABLE +" WHERE id = ? AND owner = ?")) {
                statement.setInt(1, groupId);
                statement.setInt(2, ownerId);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(parseGroup(rs));
                }
                return Optional.empty();
            }
        }
    }

    public static boolean addNewGroup(final Group newGroup, final int ownerId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO "+ GROUP_TABLE +" (title,description,owner) VALUES(?,?,?)")) {
                statement.setString(1, newGroup.getTitle());
                statement.setString(2, newGroup.getDescription());
                statement.setInt(3, ownerId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean deleteGroup(final int groupId, final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM "+ GROUP_TABLE +" WHERE id = ? AND owner = ?")) {
                statement.setInt(1, groupId);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean addUserToGroup(final int groupId, final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO "+ GROUP_REF_TABLE +" (idUser,groupId) VALUES(?,?)")) {
                statement.setInt(1, userId);
                statement.setInt(2, groupId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean removeUserFromGroup(final int groupId, final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM "+ GROUP_REF_TABLE +" WHERE groupId = ? AND idUser = ?")) {
                statement.setInt(1, groupId);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean addGroupProject(final Project newProject) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO "+ PROJECT_TABLE +" (refType, ref, title, description, color, archived) VALUES(?,?,?,?,?,?)")) {
                statement.setString(1, RefType.GROUP.name());
                statement.setInt(2, newProject.getRef());
                statement.setString(3, newProject.getTitle());
                statement.setString(4, newProject.getDescription());
                statement.setString(5, newProject.getColor());
                statement.setBoolean(6, false);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static int addProject(final Project project, final int refId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO "+ PROJECT_TABLE +" (title,refType,ref,description,color,archived) VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, project.getTitle());
                statement.setString(2, project.getRefType().name());
                statement.setInt(3, refId);
                statement.setString(4, project.getDescription());
                statement.setString(5, project.getColor());
                statement.setBoolean(6, false);
                statement.executeUpdate();
                final ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return -1;
            }
        }
    }

    public static boolean deleteTracking(final int trackingId, final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM "+ TRACKING_TABLE +" WHERE id = ? AND idUser = ?")) {
                statement.setInt(1, trackingId);
                statement.setInt(2, userId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean deleteProject(final int userID, final int projectId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM "+ PROJECT_TABLE +" WHERE ref = ? AND id = ?")) {
                statement.setInt(1, userID);
                statement.setInt(2, projectId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean deleteAllUserProjects(final int userID) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM "+ PROJECT_TABLE +" WHERE ref = ?")) {
                statement.setInt(1, userID);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static int addTracking(final Tracked tracked) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO "+ TRACKING_TABLE +" (project, idUser, start_time, end_time, timezone, active) " +
                    "VALUES(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, tracked.getProjectId());
                statement.setInt(2, tracked.getUser());
                statement.setTimestamp(3, tracked.getStart());
                statement.setTimestamp(4, tracked.getEnd());
                statement.setString(5, tracked.getTimezone());
                statement.setBoolean(6, tracked.isActive());
                statement.executeUpdate();
                final ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return -1;
            }
        }
    }

    public static boolean stopTracking(final int id, final int user) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE "+ TRACKING_TABLE +" SET active = false, end_time = ? WHERE id = ? AND idUser = ? AND active = true")) {
                statement.setTimestamp(1, Timestamp.from(Instant.now()));
                statement.setInt(2, id);
                statement.setInt(3, user);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean updateTracking(final Tracked tracked, final int userId) throws SQLException {
        if (tracked.getId() == -1) {
            tracked.overrideUser(userId);
            return addTracking(tracked) > 0;
        }
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("UPDATE "+ TRACKING_TABLE +" SET start_time = ?," +
                    "end_time = ?, timezone = ?, active = ?, project = ? WHERE id = ? AND idUser = ?")) {
                statement.setTimestamp(1, tracked.getStart());
                statement.setTimestamp(2, tracked.getEnd());
                statement.setString(3, tracked.getTimezone());
                statement.setBoolean(4, tracked.isActive());
                statement.setInt(5, tracked.getProjectId());
                statement.setInt(6, tracked.getId());
                statement.setInt(7, userId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean deleteUser(final int idToDelete) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM " + USERS_TABLE + " WHERE id = ?")) {
                statement.setInt(1, idToDelete);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean addNewUser(final User user, final String pass) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + USERS_TABLE + "(email, hash) VALUES(?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.mail());
                statement.setString(2, Auth.hashPassword(pass));
                statement.executeUpdate();
                final ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    final int userId = rs.getInt(1);
                    setNewUserRole(userId, user.roles());
                    return userId > 0;
                }
            }
        }
        return false;
    }

    public static boolean setNewUserRole(final int userId, final List<Role> roles) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + ROLE_TABLE + "(idUser, roleType) VALUES(?,?)")) {
                for (final Role role : roles) {
                    statement.setInt(1, userId);
                    statement.setString(2, role.name());
                    statement.addBatch();
                }
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean updateUserRole(final int userId, final List<Role> roles) throws SQLException {
        final List<Role> currentRoles = Database.getUserRoles(userId);
        final List<Role> toDel = currentRoles.stream().filter(o -> !roles.contains(o)).toList();
        final List<Role> toAdd = roles.stream().filter(n -> !currentRoles.contains(n)).toList();
        return (deleteRoles(userId, toDel) && addRoles(userId, toAdd));
    }

    private static boolean deleteRoles(final int userID, final List<Role> roles) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM " + ROLE_TABLE + " WHERE idUser = ? and roleType = ?")) {
                for (final Role role: roles) {
                    statement.setInt(1, userID);
                    statement.setString(2, role.name());
                    statement.addBatch();
                }
                return isSuccess(statement.executeBatch());
            }
        }
    }

    private static boolean addRoles(final int userID, final List<Role> roles) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + ROLE_TABLE + " (user,roleType) VALUES(?,?)")) {
                for (final Role role: roles) {
                    statement.setInt(1, userID);
                    statement.setString(2, role.name());
                    statement.addBatch();
                }
                return isSuccess(statement.executeBatch());
            }
        }
    }

    public static int addAdminUser(final String username, final String hash) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + USERS_TABLE + "(email, hash) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, username);
                statement.setString(2, Auth.hashPassword(hash));
                statement.executeUpdate();
                final ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return -1;
            }
        }
    }

    public static boolean setAdminRole(final int userID) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + ROLE_TABLE + "(idUser, roleType) VALUES(?,?)")) {
                statement.setInt(1, userID);
                statement.setString(2, Role.ADMIN.name());
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean resetPasswordToken(final String token, final int userId) throws SQLException {
        final Timestamp expiration = Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS));
        try (final Connection con = getConnection()){
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + TOKEN_TABLE + "(idUser,token,token_type,expiration,description) VALUES(?,?,?,?,?)")) {
                statement.setInt(1,userId);
                statement.setString(2, token);
                statement.setTimestamp(4, expiration);
                statement.setInt(3,2);
                statement.setString(5, "");
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static Optional<UserToken> getToken(final String token) throws SQLException{
        try (final Connection con = getConnection()){
            try (final PreparedStatement statement = con.prepareStatement("SELECT * FROM " + TOKEN_TABLE + " WHERE token = ?")) {
                statement.setString(1, token);
                final ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    return Optional.of(parseToken(rs));
                }
            }
        }
        return Optional.empty();
    }

    public static List<UserApiToken> getUserApiTokens(final int userId) throws SQLException {
        final List<UserApiToken> result = new ArrayList<>();
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("SELECT id, description, expiration FROM " + TOKEN_TABLE + " WHERE idUser = ? AND token_type = 1")) {
                statement.setInt(1, userId);
                final ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    result.add(new UserApiToken(
                            rs.getInt("id"),
                            rs.getString("description"),
                            Optional.ofNullable(rs.getTimestamp("expiration"))
                    ));
                }
            }
        }
        return result;
    }

    public static boolean deleteUserToken(final int userId, final int tokenId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM " + TOKEN_TABLE + " WHERE idUser = ? AND id = ?")) {
                statement.setInt(1, userId);
                statement.setInt(2, tokenId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean deleteToken(final String token, final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM " + TOKEN_TABLE + " WHERE idUser = ? AND token = ?")) {
                statement.setInt(1, userId);
                statement.setString(2, token);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean deleteAllRefreshTokensForUser(final int userId) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("DELETE FROM " + TOKEN_TABLE + " WHERE idUser = ? AND token_type = ?")) {
                statement.setInt(1, userId);
                statement.setInt(2, TokenType.REFRESH_TOKEN.getId());
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean addUserApiToken(final int userId, final String token, final String description, final Optional<Timestamp> expiration) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + TOKEN_TABLE + "(idUser, token, token_type, description, expiration) VALUES(?,?,?,?,?)")) {
                statement.setInt(1, userId);
                statement.setString(2, token);
                statement.setInt(3, TokenType.API_TOKEN.getId());
                statement.setString(4, description);
                // If expiration is not present, set it to null
                statement.setTimestamp(5, expiration.orElse(null));
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static boolean addRefreshToken(final int userId, final String token) throws SQLException {
        try (final Connection con = getConnection()) {
            try (final PreparedStatement statement = con.prepareStatement("INSERT INTO " + TOKEN_TABLE + "(idUser, token, token_type, description, expiration) VALUES(?,?,?,?,?)")) {
                statement.setInt(1, userId);
                statement.setString(2, token);
                statement.setInt(3, TokenType.REFRESH_TOKEN.getId());
                statement.setString(4, "");
                // If expiration is not present, set it to null
                statement.setTimestamp(5, Timestamp.from(Instant.now().plus(Auth.REFRESH_LIFETIME_SEC, ChronoUnit.SECONDS)));
                return statement.executeUpdate() > 0;
            }
        }
    }

    public static void initDataBase() throws SQLException {
        final String createUserTable = "CREATE TABLE IF NOT EXISTS " + USERS_TABLE + " " +
                "(" +
                "id SERIAL PRIMARY KEY," +
                "email TEXT UNIQUE NOT NULL," +
                "hash TEXT NOT NULL" +
                ")";
        final String createRoleTable = "CREATE TABLE IF NOT EXISTS " + ROLE_TABLE + " " +
                "(" +
                "id SERIAL PRIMARY KEY," +
                "idUser INTEGER REFERENCES " + USERS_TABLE + "(id) ON DELETE CASCADE," +
                "roleType TEXT NOT NULL" +
                ")";
        final String createGroupTable = "CREATE TABLE IF NOT EXISTS " + GROUP_TABLE + " " +
                "(" +
                "id SERIAL PRIMARY KEY," +
                "title TEXT," +
                "description TEXT," +
                "owner INTEGER REFERENCES " + USERS_TABLE + "(id)" +
                ")";
        final String createGroupRefTable = "CREATE TABLE IF NOT EXISTS " + GROUP_REF_TABLE + " " +
                "(" +
                "idUser INTEGER," +
                "groupId INTEGER," +
                "PRIMARY KEY (groupId, idUser),"+
                "FOREIGN KEY (groupId) REFERENCES " + GROUP_TABLE + "(id) ON DELETE CASCADE," +
                "FOREIGN KEY (idUser) REFERENCES " + USERS_TABLE + "(id) ON DELETE CASCADE" +
                ")";
        final String createProjectTable = "CREATE TABLE IF NOT EXISTS " + PROJECT_TABLE + " " +
                "(" +
                "id SERIAL PRIMARY KEY," +
                "refType TEXT NOT NULL," +
                "ref INTEGER NOT NULL," +
                "title TEXT," +
                "description TEXT," +
                "color CHARACTER(7)," +
                "archived BOOLEAN" +
                ")";
        final String createTrackedTable = "CREATE TABLE IF NOT EXISTS " + TRACKING_TABLE + " " +
                "(" +
                "id SERIAL PRIMARY KEY," +
                "project INTEGER REFERENCES " + PROJECT_TABLE + "(id) ON DELETE CASCADE," +
                "idUser INTEGER REFERENCES " + USERS_TABLE + "(id) ON DELETE CASCADE," +
                "start_time TIMESTAMP," +
                "end_time TIMESTAMP," +
                "timezone TEXT," +
                "active BOOLEAN" +
                ")";
        final String createTokenTable = "CREATE TABLE IF NOT EXISTS " + TOKEN_TABLE + " " +
                "(" +
                "id SERIAL PRIMARY KEY," +
                "idUser INTEGER REFERENCES " + USERS_TABLE + "(id) ON DELETE CASCADE," +
                "token TEXT NOT NULL," +
                "token_type INTEGER," +
                "description TEXT," +
                "expiration TIMESTAMP" +
                ")";
        try(final Connection con = getConnection()) {
            try(final Statement st = con.createStatement()) {
                st.execute(createUserTable);
                st.execute(createRoleTable);
                st.execute(createGroupTable);
                st.execute(createGroupRefTable);
                st.execute(createProjectTable);
                st.execute(createTrackedTable);
                st.execute(createTokenTable);
            }
        }
        setAdminIfNotExists();
    }

    private static void setAdminIfNotExists() throws SQLException {
        final Optional<String> adminUser = Util.getEnvVar("ADMIN_USER_NAME", s -> s, true);
        final Optional<String> adminPass = Util.getEnvVar("ADMIN_PASSWORD", s -> s, true);
        final Optional<Integer> exists = getUserID(adminUser.get());
        if (exists.isEmpty()) {
            if (adminUser.isPresent() && adminPass.isPresent()) {
                final int adminId = addAdminUser(adminUser.get(), adminPass.get());
                if (adminId >= 0) {
                    setAdminRole(adminId);
                }
            }
        }
    }
}
