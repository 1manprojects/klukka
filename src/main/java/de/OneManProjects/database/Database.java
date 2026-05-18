package de.OneManProjects.database;

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

import de.OneManProjects.utils.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class Database {

    public static final String PROJECT_TABLE = "projects";
    public static final String TRACKING_TABLE = "tracking";
    public static final String ROLE_TABLE = "roles";
    public static final String USERS_TABLE = "users";
    public static final String GROUP_TABLE = "groups";
    public static final String GROUP_REF_TABLE = "groupRef";
    public static final String TOKEN_TABLE = "tokens";

    public static Connection getConnection() throws SQLException {
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

    public static <T> Optional<T> executeQuery(final String sql, final ResultSetMapper<T> mapper, final Object... params) throws SQLException {
        try (final Connection con = getConnection();
             final PreparedStatement statement = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (final ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapper.map(rs));
                }
                return Optional.empty();
            }
        }
    }

    public static int executeInsertReturningId(final String sql, final Object... params) throws SQLException {
        try (final Connection con = getConnection();
             final PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            try (final ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public static <T> List<T> executeQueryList(final String sql, final ResultSetMapper<T> mapper, final Object... params) throws SQLException {
        final List<T> results = new ArrayList<>();
        try (final Connection con = getConnection();
             final PreparedStatement statement = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (final ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        }
        return results;
    }

    public static int executeUpdate(final String sql, final Object... params) throws SQLException {
        try (final Connection con = getConnection();
             final PreparedStatement statement = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
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
                "token TEXT NOT NULL ," +
                "token_type INTEGER," +
                "description TEXT," +
                "expiration TIMESTAMP" +
                ")";
        final String addCommentColumn =
                "ALTER TABLE " + TRACKING_TABLE +
                        " ADD COLUMN IF NOT EXISTS comment TEXT";
        final String updateTokenColumn =
                "ALTER TABLE " + TOKEN_TABLE+ " ADD CONSTRAINT tokens_token_unique UNIQUE (token)";
        try(final Connection con = getConnection()) {
            try(final Statement st = con.createStatement()) {
                st.execute(createUserTable);
                st.execute(createRoleTable);
                st.execute(createGroupTable);
                st.execute(createGroupRefTable);
                st.execute(createProjectTable);
                st.execute(createTrackedTable);
                st.execute(createTokenTable);
                st.execute(addCommentColumn);
                st.execute(updateTokenColumn);
            }
        }
        setAdminIfNotExists();
    }

    private static void setAdminIfNotExists() throws SQLException {
        final Optional<String> adminUser = Util.getEnvVar("ADMIN_USER_NAME", s -> s, true);
        final Optional<String> adminPass = Util.getEnvVar("ADMIN_PASSWORD", s -> s, true);
        final Optional<Integer> exists = Users.getUserID(adminUser.get());
        if (exists.isEmpty()) {
            if (adminPass.isPresent()) {
                final int adminId = Users.addAdminUser(adminUser.get(), adminPass.get());
                if (adminId >= 0) {
                    Users.setAdminRole(adminId);
                }
            }
        }
    }
}
