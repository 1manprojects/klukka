package de.OneManProjects.database;

import de.OneManProjects.data.Group;
import de.OneManProjects.data.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Groups {
    static Group parseGroup(final ResultSet rs) throws SQLException {
        return new Group(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("owner")
        );
    }

    public static boolean leaveGroup(final int userId, final int groupId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM " + Database.GROUP_REF_TABLE + " WHERE idUser = ? AND groupId = ?",
                userId, groupId
        ) > 0;
    }

    public static List<Group> getAllGroups() throws SQLException {
        return Database.executeQueryList(
                "SELECT * FROM " + Database.GROUP_TABLE,
                Groups::parseGroup
        );
    }

    public static boolean updateGroup(final Group newGroup, final int ownerId) throws SQLException {
        return Database.executeUpdate(
                "UPDATE " + Database.GROUP_TABLE +" SET title = ?, description = ? WHERE id = ? AND owner = ?",
                newGroup.getTitle(), newGroup.getDescription(), newGroup.getId(), ownerId
        ) > 0;
    }

    public static List<User> getUsersInGroup(final int groupId) throws SQLException {
        return Database.executeQueryList(
                "SELECT u.id, u.email FROM " + Database.USERS_TABLE + " AS u JOIN " + Database.GROUP_REF_TABLE +
                        " AS g ON u.id = g.iduser WHERE g.groupId = ?",
                rs -> new User(rs.getInt("id"), rs.getString("email"), new ArrayList<>()),
                groupId
        );
    }

    public static List<Group> getUserGroups(final int userId) throws SQLException {
        return Database.executeQueryList(
                "SELECT * FROM "+ Database.GROUP_TABLE + " g JOIN " + Database.GROUP_REF_TABLE + " r ON g.id = r.groupId WHERE idUser = ?",
                Groups::parseGroup,
                userId
        );
    }

    public static List<Group> getManagedGroups(final int ownerId) throws SQLException {
        return Database.executeQueryList(
                "SELECT * FROM "+ Database.GROUP_TABLE +" WHERE owner = ?",
                Groups::parseGroup,
                ownerId
        );
    }

    public static Optional<Group> getGroup(final int groupId, final int ownerId) throws SQLException {
        return Database.executeQuery(
                "SELECT * FROM "+ Database.GROUP_TABLE +" WHERE id = ? AND owner = ?",
                Groups::parseGroup,
                groupId, ownerId
        );
    }

    public static boolean addNewGroup(final Group newGroup, final int ownerId) throws SQLException {
        return Database.executeUpdate(
                "INSERT INTO "+ Database.GROUP_TABLE +" (title,description,owner) VALUES(?,?,?)",
                newGroup.getTitle(), newGroup.getDescription(), ownerId
        ) > 0;
    }

    public static boolean deleteGroup(final int groupId, final int userId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM "+ Database.GROUP_TABLE +" WHERE id = ? AND owner = ?",
                groupId, userId
        ) > 0;
    }

    public static boolean addUserToGroup(final int groupId, final int userId) throws SQLException {
        return Database.executeUpdate(
                "INSERT INTO "+ Database.GROUP_REF_TABLE +" (idUser,groupId) VALUES(?,?)",
                userId, groupId
        ) > 0;
    }

    public static boolean removeUserFromGroup(final int groupId, final int userId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM "+ Database.GROUP_REF_TABLE +" WHERE groupId = ? AND idUser = ?",
                groupId, userId
        ) > 0;
    }

    public static List<Integer> getGroupIdForUser(final int userId) throws SQLException {
        return Database.executeQueryList(
                "SELECT groupId FROM " + Database.GROUP_REF_TABLE + " WHERE idUser = ?",
                rs -> rs.getInt("groupId"),
                userId
        );
    }
}
