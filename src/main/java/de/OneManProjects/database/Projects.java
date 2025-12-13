package de.OneManProjects.database;

import de.OneManProjects.data.Group;
import de.OneManProjects.data.Project;
import de.OneManProjects.data.Tracked;
import de.OneManProjects.data.enums.RefType;

import java.sql.*;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Projects {
    static Project parseProject(final ResultSet rs) throws SQLException {
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

    static Project getAllUserTrackedMinutesForGroupProject(final ResultSet rs) throws SQLException {
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

    public static List<Project> getProjects(final int refId, final boolean all) throws SQLException {
        return Database.executeQueryList(
                "SELECT id,ref,refType,title,description,color,archived FROM "+ Database.PROJECT_TABLE +
                        " WHERE reftype = '" + RefType.USER + "' AND ref = ?",
                Projects::parseProject,
                refId
        );
    }

    public static List<Project> getGroupProjects(final int groupId, final boolean all) throws SQLException {
        return Database.executeQueryList(
                "SELECT id,ref,refType,title,description,color,archived FROM "+ Database.PROJECT_TABLE +
                        " WHERE reftype = '" + RefType.GROUP + "' AND ref = ?",
            Projects::parseProject,
            groupId
        );
    }

    public static boolean setProjectArchive(final int projectId, final boolean value) throws SQLException {
        return Database.executeUpdate(
                "UPDATE "+ Database.PROJECT_TABLE + " SET archived = ? WHERE id = ?",
                value, projectId
        ) > 0;
    }

    public static boolean updateProjects(final Project toUpdate) throws SQLException{
        return Database.executeUpdate(
                "UPDATE " + Database.PROJECT_TABLE + " SET title = ?, description = ?, color = ? WHERE id = ?",
                toUpdate.getTitle(),
                toUpdate.getDescription(),
                toUpdate.getColor(),
                toUpdate.getId()
        ) > 0;
    }

    public static List<Project> getProjectsFromGroup(final int refId, final boolean all) throws SQLException {
        return Database.executeQueryList(
                "SELECT * FROM "+ Database.PROJECT_TABLE +" WHERE ref = ? AND reftype = '" + RefType.GROUP +"'" +
                        (!all ? " AND archived = false" : ""),
                Projects::getAllUserTrackedMinutesForGroupProject,
                refId
        );
    }

    public static boolean canUserManageProject(final int userID, final int projectID) throws SQLException {
        final Optional<Project> project = getProjectById(projectID);
        if (project.isPresent()) {
            if (project.get().getRefType().equals(RefType.GROUP)) {
                final List<Group> groups = Groups.getManagedGroups(userID);
                return groups.stream().anyMatch(g -> g.getId() == project.get().getRef());
            } else {
                return project.get().getRef() == userID;
            }
        }
        return false;
    }

    public static List<Project> getUserGroupProjects(final int refId) throws SQLException {
        final List<Integer> groupsUserIsIn = Groups.getGroupIdForUser(refId);
        groupsUserIsIn.addAll(Groups.getManagedGroups(refId).stream().map(Group::getId).toList());
        if (!groupsUserIsIn.isEmpty()) {
            try (final Connection con = Database.getConnection()) {
                final String sql = "SELECT * FROM " + Database.PROJECT_TABLE + " WHERE reftype = '" + RefType.GROUP +
                        "' AND ref IN (" + groupsUserIsIn.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
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
        return Database.executeQueryList(
                "SELECT * FROM " + Database.TRACKING_TABLE + " WHERE idUser = ? and start_time >= ? and end_time <= ?",
                Projects::parseTracked,
                userid, fromStart, fromEnd
        );
    }

    public static List<Tracked> getGroupTrackedForRange(final List<Integer> groupProjectIds, final Instant start, final Instant end) throws SQLException {
        final Timestamp fromStart = Timestamp.from(start);
        final Timestamp fromEnd = Timestamp.from(end);
        final Object[] params = Stream.concat(
                groupProjectIds.stream().map(i -> (Object) i),
                Stream.of(fromStart, fromEnd)
        ).toArray();
        return Database.executeQueryList(
                "SELECT * FROM " + Database.TRACKING_TABLE +
                        " WHERE project in ("+ groupProjectIds.stream().map(id -> "?").collect(Collectors.joining(",")) +
                        ") and start_time >= ? and end_time <= ?",
                Projects::parseTracked,
                params
        );
    }

    public static double getTrackedMinutesThisMonth(final int userId) throws SQLException {
        final Timestamp limitStart = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1).atStartOfDay());
        final Timestamp limitEnd = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth().atStartOfDay());
        return Database.executeQueryList(
                "SELECT EXTRACT(EPOCH FROM (end_time - start_time)) / 60 AS difference FROM "+ Database.TRACKING_TABLE +
                        " WHERE idUser = ? AND start_time > ? AND end_time < ?",
                rs -> rs.getDouble("difference"),
                userId, limitStart, limitEnd
        ).stream().mapToDouble(Double::doubleValue).sum();
    }

    public static double getUserTrackedMinutesForProject(final int id, final int user) throws SQLException {
        final Timestamp limitStart = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1).atStartOfDay());
        final Timestamp limitEnd = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth().atStartOfDay());
        return Database.executeQueryList(
                "SELECT EXTRACT(EPOCH FROM (end_time - start_time)) / 60 AS difference FROM "+ Database.TRACKING_TABLE +
                        " WHERE project = ? AND idUser = ? AND start_time > ? AND end_time < ?",
                rs -> rs.getDouble("difference"),
                id, user, limitStart, limitEnd
        ).stream().mapToDouble(Double::doubleValue).sum();
    }

    public static double getAllUserTrackedMinutesForGroupProject(final int id) throws SQLException {
        final Timestamp limitStart = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atDay(1).atStartOfDay());
        final Timestamp limitEnd = Timestamp.valueOf(YearMonth.from(Instant.now().atZone(ZoneId.of("UTC"))).atEndOfMonth().atStartOfDay());
        return Database.executeQueryList(
                "SELECT EXTRACT(EPOCH FROM (end_time - start_time)) / 60 AS difference FROM "+ Database.TRACKING_TABLE + " WHERE project = ? " +
                        "AND start_time > ? AND end_time < ?",
                rs -> rs.getLong("difference"),
                id, limitStart, limitEnd
        ).stream().mapToLong(Long::longValue).sum();
    }

    public static Optional<Project> getProjectById(final int id) throws SQLException {
        return Database.executeQuery(
                "SELECT id,ref,refType,title,description,color,archived FROM "+ Database.PROJECT_TABLE +" WHERE id = ?",
                Projects::parseProject,
                id
        );
    }

    public static Optional<Tracked> getActiveTracking(final int userId) throws SQLException {
        return Database.executeQuery(
                "SELECT * FROM "+ Database.TRACKING_TABLE +" WHERE active = true AND idUser = ?",
                Projects::parseTracked,
                userId
        );
    }

    public static boolean addGroupProject(final Project newProject) throws SQLException {
        return Database.executeUpdate(
                "INSERT INTO "+ Database.PROJECT_TABLE +" (refType, ref, title, description, color, archived) VALUES(?,?,?,?,?,?)",
                RefType.GROUP.name(),
                newProject.getRef(),
                newProject.getTitle(),
                newProject.getDescription(),
                newProject.getColor(),
                false
        ) < 0;
    }

    public static int addProject(final Project project, final int refId) throws SQLException {
        return Database.executeInsertReturningId(
                "INSERT INTO "+ Database.PROJECT_TABLE + " (title,refType,ref,description,color,archived) VALUES(?,?,?,?,?,?)",
                project.getTitle(),
                project.getRefType().name(),
                refId,
                project.getDescription(),
                project.getColor(),
                false
        );
    }

    public static boolean deleteTracking(final int trackingId, final int userId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM "+ Database.TRACKING_TABLE +" WHERE id = ? AND idUser = ?",
                trackingId,
                userId
        ) > 0;
    }

    public static boolean deleteProject(final int userID, final int projectId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM "+ Database.PROJECT_TABLE +" WHERE ref = ? AND id = ?",
                userID, projectId
        ) > 0;
    }

    public static boolean deleteAllUserProjects(final int userID) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM "+ Database.PROJECT_TABLE +" WHERE ref = ?",
                userID
        ) > 0;
    }

    public static int addTracking(final Tracked tracked) throws SQLException {
        return Database.executeInsertReturningId(
                "INSERT INTO "+ Database.TRACKING_TABLE +" (project, idUser, start_time, end_time, timezone, active) VALUES(?,?,?,?,?,?)",
                tracked.getProjectId(),
                tracked.getUser(),
                tracked.getStart(),
                tracked.getEnd(),
                tracked.getTimezone(),
                tracked.isActive()
        );
    }

    public static boolean stopTracking(final int id, final int user) throws SQLException {
        return Database.executeUpdate(
                "UPDATE "+ Database.TRACKING_TABLE +" SET active = false, end_time = ? WHERE id = ? AND idUser = ? AND active = true",
                Timestamp.from(Instant.now()),
                id,
                user
        ) > 0;
    }

    public static boolean updateTracking(final Tracked tracked, final int userId) throws SQLException {
        if (tracked.getId() == -1) {
            tracked.overrideUser(userId);
            return addTracking(tracked) > 0;
        }
        return Database.executeUpdate(
                "UPDATE "+ Database.TRACKING_TABLE +" SET start_time = ?," +
                        "end_time = ?, timezone = ?, active = ?, project = ? WHERE id = ? AND idUser = ?",
                tracked.getStart(),
                tracked.getEnd(),
                tracked.getTimezone(),
                tracked.isActive(),
                tracked.getProjectId(),
                tracked.getId(),
                userId
        ) > 0;
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
}
