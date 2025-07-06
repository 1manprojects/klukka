package de.OneManProjects.security;

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

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.OneManProjects.Database;
import de.OneManProjects.data.dto.Login;
import de.OneManProjects.data.enums.Role;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.HttpStatus;
import io.javalin.http.SameSite;


import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class Auth {
    final static String ISSUER = "1ManProjects";
    public final static int JWT_LIFETIME_SEC = 3600 * 5;
    public final static int REFRESH_LIFETIME_SEC = 7 * 24 * 3600;

    private static Algorithm getAlgorithm() {
        //For testing only should create one at startup new
        return Algorithm.HMAC256("ofrqR3H0VG^xIZLnfYtP8WNn968n8@uX3xfZd%EA1S0Z@1");
    }

    private static Optional<DecodedJWT> validateAndDecodeToken(final String token) {
        if (token != null) {
            final JWTVerifier verifier = JWT.require(getAlgorithm())
                    .withIssuer(ISSUER)
                    .withClaimPresence("user")
                    .build();
            try {
                return Optional.of(verifier.verify(token));
            } catch (final JWTVerificationException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static void setCookies(final Context ctx, final int userID) throws SQLException {
        final String token = Auth.genJWT(userID);
        final String refreshToken= Auth.genRefreshToken(userID);
        final Cookie ct = new Cookie("jwt", token);
        final Cookie cr = new Cookie("refresh", refreshToken);
        ct.setHttpOnly(true);
        ct.setSecure(true);
        ct.setSameSite(SameSite.STRICT);
        ct.setMaxAge(JWT_LIFETIME_SEC);
        ct.setPath("/api");
        cr.setHttpOnly(true);
        cr.setSecure(true);
        cr.setSameSite(SameSite.STRICT);
        cr.setMaxAge(REFRESH_LIFETIME_SEC);
        cr.setPath("/api");

        ctx.cookie(ct);
        ctx.cookie(cr);
    }

    public static String hashPassword(final String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    public static boolean login(final Login login) throws SQLException {
        final Optional<String> optionalString = Database.getUserHash(login.mail());
        return optionalString.filter(s ->
                BCrypt.verifyer()
                    .verify(login.password().toCharArray(), s)
                    .verified).isPresent();
    }

    public static String genJWT(final int user) {
        return JWT.create()
                .withClaim("user", user)
                .withExpiresAt(Instant.now().plus(JWT_LIFETIME_SEC, ChronoUnit.SECONDS))
                .withIssuer(ISSUER)
                .sign(getAlgorithm());
    }

    public static String genRefreshToken(final int user) throws SQLException {
        final String refreshToken = UUID.randomUUID().toString();
        Database.addRefreshToken(user, refreshToken);
        return refreshToken;
    }

    public static boolean validateToken(final Context ctx) {
        if (ctx.cookieMap().containsKey("jwt")) {
            final String token = ctx.cookie("jwt");
            return validateAndDecodeToken(token).isPresent();
        } else {
            final String apiToken = ctx.header("Authorization");
            if (apiToken != null) {
                try {
                    final Optional<UserToken> token = Database.getToken(apiToken.replace("Bearer ", ""));
                    if (token.isPresent()) {
                        if (token.get().expiration().isPresent()) {
                            final Timestamp currentTimeStamp = Timestamp.from(Instant.now());
                            return currentTimeStamp.before(token.get().expiration().get());
                        }
                        return true;
                    }
                } catch (final SQLException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public static Optional<Integer> validateRefreshToken(final String refreshToken) throws SQLException {
        final Optional<UserToken> dbToken = Database.getToken(refreshToken);
        if (dbToken.isPresent()) {
            if (dbToken.get().tokentype().equals(TokenType.REFRESH_TOKEN) && dbToken.get().expiration().isPresent()) {
                final Timestamp currentTimeStamp = Timestamp.from(Instant.now());
                if (currentTimeStamp.before(dbToken.get().expiration().get()) ) {
                    return Optional.of(dbToken.get().user());
                }
            }
        }
        return Optional.empty();
    }

    public static int getUserFromContext(final Context ctx) {
        if (ctx.cookieMap().containsKey("jwt")) {
            final String token = ctx.cookie("jwt");
            final Optional<DecodedJWT> decodedJWT = validateAndDecodeToken(token);
            if (decodedJWT.isPresent()) {
                return decodedJWT.get().getClaim("user").asInt();
            }
        } else {
            final String apiToken = ctx.header("Authorization");
            if (apiToken != null) {
                try {
                    final Optional<UserToken> token = Database.getToken(apiToken.replace("Bearer ", ""));
                    if (token.isPresent()) {
                        return token.get().user();
                    }
                } catch (final SQLException e) {
                    ctx.status(HttpStatus.BAD_REQUEST);
                    return -1;
                }
            }
        }
        ctx.status(HttpStatus.BAD_REQUEST);
        return -1;
    }

    public static boolean isUserAdmin(final Context ctx) throws SQLException {
        final int userId = getUserFromContext(ctx);
        final List<Role> roles = Database.getUserRoles(userId);
        return roles.contains(Role.ADMIN);
    }

    public static boolean isUserGroup(final Context ctx) throws SQLException {
        final int userId = getUserFromContext(ctx);
        final List<Role> roles = Database.getUserRoles(userId);
        return roles.contains(Role.GROUP) || roles.contains(Role.ADMIN);
    }

    public static String generateApiToken() throws IllegalStateException {
        UUID uuid = UUID.randomUUID();
        for (int i = 0; i < 50; i++) {
            try {
                // Check if the UUID already exists in the database
                if (Database.getToken(uuid.toString()).isEmpty()) {
                    return uuid.toString();
                }
            } catch (final SQLException e) {
                throw new IllegalStateException("Error cannot connect to DB to check new Token");
            }
            uuid = UUID.randomUUID();
        }
        throw new IllegalStateException("Could not generate a unique API token after 50 attempts.");
    }

    public static String createRandomPassword() {
        final int size = 12;
        final char[] SYMBOLS = "^$*.[]{}()?-\"!@#%&/\\,><':;|_~`".toCharArray();
        final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        final char[] NUMBERS = "0123456789".toCharArray();
        final char[] ALL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789^$*.[]{}()?-\"!@#%&/\\,><':;|_~`".toCharArray();
        final Random rand = new SecureRandom();

        final char[] password = new char[size];
        password[0] = LOWERCASE[rand.nextInt(LOWERCASE.length)];
        password[1] = UPPERCASE[rand.nextInt(UPPERCASE.length)];
        password[2] = NUMBERS[rand.nextInt(NUMBERS.length)];
        password[3] = SYMBOLS[rand.nextInt(SYMBOLS.length)];
        for (int i = 4; i < size; i++) {
            password[i] = ALL_CHARS[rand.nextInt(ALL_CHARS.length)];
        }
        for (int i = 0; i < password.length; i++) {
            final int randomPosition = rand.nextInt(password.length);
            final char temp = password[i];
            password[i] = password[randomPosition];
            password[randomPosition] = temp;
        }
        return new String(password);
    }
}
