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

import de.OneManProjects.data.dto.UserApiToken;
import de.OneManProjects.security.Auth;
import de.OneManProjects.security.TokenType;
import de.OneManProjects.security.UserToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class Tokens {
    static UserToken parseToken(final ResultSet rs) throws SQLException {
        final Timestamp expiration = rs.getTimestamp("expiration");

        return new UserToken(
                rs.getString("token"),
                rs.getInt("idUser"),
                expiration != null ? Optional.of(expiration) : Optional.empty(),
                TokenType.valueOf(rs.getInt("token_type")),
                rs.getString("description")
        );
    }

    public static boolean resetPasswordToken(final String token, final int userId) throws SQLException {
        final Timestamp expiration = Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS));
        return Database.executeUpdate(
                "INSERT INTO " + Database.TOKEN_TABLE + "(idUser,token,token_type,expiration,description) VALUES(?,?,?,?,?)",
                userId,
                token,
                2,
                expiration,
                ""
        ) > 0;
    }

    public static Optional<UserToken> getToken(final String token) throws SQLException {
        return Database.executeQuery(
                "SELECT * FROM " + Database.TOKEN_TABLE + " WHERE token = ?",
                Tokens::parseToken,
                token
        );
    }

    public static List<UserApiToken> getUserApiTokens(final int userId) throws SQLException {
        return Database.executeQueryList(
                "SELECT id, description, expiration FROM " + Database.TOKEN_TABLE + " WHERE idUser = ? AND token_type = 1",
                rs -> new UserApiToken(
                        rs.getInt("id"),
                        rs.getString("description"),
                        Optional.ofNullable(rs.getTimestamp("expiration"))),
                userId
        );
    }

    public static boolean deleteUserToken(final int userId, final int tokenId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM " + Database.TOKEN_TABLE + " WHERE idUser = ? AND id = ?",
                userId, tokenId
        ) > 0;
    }

    public static boolean deleteToken(final String token, final int userId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM " + Database.TOKEN_TABLE + " WHERE idUser = ? AND token = ?",
                userId, token
        ) > 0;
    }

    public static boolean deleteAllRefreshTokensForUser(final int userId) throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM " + Database.TOKEN_TABLE + " WHERE idUser = ? AND token_type = ?",
                userId, TokenType.REFRESH_TOKEN.getId()
        ) > 0;
    }

    public static boolean addUserApiToken(final int userId, final String token, final String description, final Optional<Timestamp> expiration) throws SQLException {
        return Database.executeUpdate(
                "INSERT INTO " + Database.TOKEN_TABLE + "(idUser, token, token_type, description, expiration) VALUES(?,?,?,?,?)",
                userId, token, TokenType.API_TOKEN.getId(), description, expiration.orElse(null)
        ) > 0;
    }

    public static boolean addRefreshToken(final int userId, final String token) throws SQLException {
        return Database.executeUpdate(
                "INSERT INTO " + Database.TOKEN_TABLE + "(idUser, token, token_type, description, expiration) VALUES(?,?,?,?,?)",
                userId, token, TokenType.REFRESH_TOKEN.getId(), "",
                Timestamp.from(Instant.now().plus(Auth.REFRESH_LIFETIME_SEC, ChronoUnit.SECONDS))
        ) > 0;
    }

    public static boolean clearOldTokens() throws SQLException {
        return Database.executeUpdate(
                "DELETE FROM " + Database.TOKEN_TABLE + " WHERE token_type = 2 or token_type = 3 and expiration < CURRENT_DATE"
        ) > 0;
    }
}