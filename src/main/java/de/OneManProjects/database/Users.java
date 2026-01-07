package de.OneManProjects.database;

/*-
 * #%L
 * Klukka
 * %%
 * Copyright (C) 2025 - 2026 Nikolai Reed reed@1manprojects.de
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

import de.OneManProjects.data.User;
import de.OneManProjects.data.enums.Role;
import de.OneManProjects.security.Auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class Users {
    static User parseUser(final ResultSet rs) throws SQLException {
        final int userID = rs.getInt("id");
        return new User(
                userID,
                rs.getString("email"),
                getUserRoles(userID));
    }

    public static List<Role> getUserRoles(final int userId) throws SQLException {
        return Database.executeQueryList(
                "SELECT DISTINCT roleType FROM " + Database.ROLE_TABLE + " WHERE idUser = ?",
                (rs) -> Role.valueOf(rs.getString("roleType")),
                userId
        );
    }

    public static List<User> getAllUsers() throws SQLException {
        return Database.executeQueryList(
                "SELECT id, email FROM " + Database.USERS_TABLE,
                Users::parseUser
        );
    }

    public static Optional<Integer> getUserID(final String mail) throws SQLException {
        return Database.executeQuery(
                "SELECT id FROM " + Database.USERS_TABLE + " WHERE email = ?",
                rs -> rs.getInt("id"),
                mail
        );
    }

    public static Optional<String> getUserMail(final int id) throws SQLException {
        return Database.executeQuery(
                "SELECT email FROM " + Database.USERS_TABLE + " WHERE id = ?",
                rs -> rs.getString("email"),
                id
        );
    }

    public static Optional<String> getUserHash(final String mail) throws SQLException {
        return Database.executeQuery(
                "SELECT hash FROM "+ Database.USERS_TABLE + " WHERE email = ?",
                rs -> rs.getString("hash"),
                mail
        );
    }

    public static Optional<User> getUserInfo(final int id) throws SQLException {
        return Database.executeQuery(
                "SELECT * FROM "+ Database.USERS_TABLE +" WHERE id = ?",
                Users::parseUser,
                id
        );
    }

    public static boolean deleteUser(final int idToDelete) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM " + Database.USERS_TABLE + " WHERE id = ?",
                idToDelete
        ) > 0;
    }

    public static boolean addNewUser(final User user, final String pass) throws SQLException {
        final int userId = Database.executeInsertReturningId(
                "INSERT INTO " + Database.USERS_TABLE + "(email, hash) VALUES(?,?)",
                user.mail(), Auth.hashPassword(pass)
        );
        if (userId >= 0) {
            setNewUserRole(userId, user.roles());
            return userId > 0;
        }
        return false;
    }

    public static boolean setNewUserRole(final int userId, final List<Role> roles) throws SQLException {
        return roles.stream().allMatch(role ->
        {
            try {
                return Database.executeUpdate(
                        "INSERT INTO " + Database.ROLE_TABLE + "(idUser, roleType) VALUES(?,?)",
                        userId,
                        role.name()
                ) > 0;
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static boolean updateUserRole(final int userId, final List<Role> roles) throws SQLException {
        final List<Role> currentRoles = getUserRoles(userId);
        final List<Role> toDel = currentRoles.stream().filter(o -> !roles.contains(o)).toList();
        final List<Role> toAdd = roles.stream().filter(n -> !currentRoles.contains(n)).toList();
        return (deleteRoles(userId, toDel) && addRoles(userId, toAdd));
    }

    private static boolean deleteRoles(final int userId, final List<Role> roles) throws SQLException {
        return roles.stream().allMatch(role ->
        {
            try {
                return Database.executeUpdate(
                        "DELETE FROM " + Database.ROLE_TABLE + " WHERE idUser = ? and roleType = ?",
                        userId,
                        role.name()
                ) > 0;
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static boolean addRoles(final int userId, final List<Role> roles) throws SQLException {
        return roles.stream().allMatch(role ->
        {
            try {
                return Database.executeUpdate(
                        "INSERT INTO " + Database.ROLE_TABLE + " (idUser, roleType) VALUES(?,?)",
                        userId,
                        role.name()
                ) > 0;
            } catch (final SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static int addAdminUser(final String username, final String pass) throws SQLException {
        return Database.executeInsertReturningId(
                "INSERT INTO " + Database.USERS_TABLE + "(email, hash) VALUES(?,?)",
                username, Auth.hashPassword(pass)
        );
    }

    public static boolean setAdminRole(final int userID) throws SQLException {
        return Database.executeUpdate(
                "INSERT INTO " + Database.ROLE_TABLE + "(idUser, roleType) VALUES(?,?)",
                userID, Role.ADMIN.name()
        ) > 0;
    }

    public static boolean updatePassword(final int userId, final String newHash) throws SQLException {
        return Database.executeUpdate(
                "UPDATE " + Database.USERS_TABLE + " SET hash = ? WHERE id = ?",
                newHash, userId
        ) > 0;
    }

    public static boolean updateUserMail(final int userId, final String newMail) throws SQLException {
        return Database.executeUpdate(
                "UPDATE " + Database.USERS_TABLE + " SET email = ? WHERE id = ?",
                newMail, userId
        ) > 0;
    }
}
