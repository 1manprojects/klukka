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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.OneManProjects.api.Admins;
import de.OneManProjects.api.Groups;
import de.OneManProjects.api.Responses;
import de.OneManProjects.api.Users;
import de.OneManProjects.data.dto.*;
import de.OneManProjects.mail.Mail;
import de.OneManProjects.security.Auth;
import de.OneManProjects.security.UserToken;
import de.OneManProjects.utils.OptionalTypeAdapter;
import de.OneManProjects.utils.Util;
import io.javalin.Javalin;
import io.javalin.http.*;
import io.javalin.http.ContentType;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import io.javalin.openapi.*;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import jakarta.mail.MessagingException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static de.OneManProjects.api.Admins.PRIVACY_HTML;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final Optional<Integer> port = Util.getEnvVar("APPLICATION_PORT", Integer::parseInt, false);
        final Optional<String> AppUrl = Util.getEnvVar("APPLICATION_URL", s->s, true);
        final boolean DEBUG = Boolean.getBoolean("debug");

        try{
            Database.initDataBase();
        } catch (final SQLException e) {
            System.out.println(e.getMessage());
            System.exit(2);
        }

        final Javalin app = createJavalinApp(DEBUG, AppUrl);
        app.start(port.orElse(3001));
    }

    public static Javalin createJavalinApp(final boolean DEBUG, final Optional<String> AppUrl) {
        final Gson gson = new GsonBuilder()
        .registerTypeAdapter(
                new TypeToken<Optional<Timestamp>>() {}.getType(),
                new OptionalTypeAdapter<>(new Gson().getAdapter(Timestamp.class))
        )
        .registerTypeAdapter(
                new TypeToken<Optional<Integer>>() {}.getType(),
                new OptionalTypeAdapter<>(new Gson().getAdapter(Integer.class))
        )
                .registerTypeAdapter(
                        new TypeToken<Optional<String>>() {}.getType(),
                        new OptionalTypeAdapter<>(new Gson().getAdapter(String.class))
                )
        .create();
        final JsonMapper gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull final Object obj, @NotNull final Type type) {
                return gson.toJson(obj, type);
            }

            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull final String json, @NotNull final Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };

        final Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    if (DEBUG) {
                        it.allowHost("http://localhost:3000");
                    } else {
                        it.allowHost(AppUrl.orElseThrow());
                    }
                    it.allowCredentials = true;
                    it.exposeHeader("x-server");
                    it.exposeHeader("Content-Disposition");
                });
            });
            config.jsonMapper(gsonMapper);
            config.registerPlugin(new OpenApiPlugin(openConfig ->
                    openConfig.withDocumentationPath("/openapi")
                            .withPrettyOutput()));
            config.registerPlugin(new SwaggerPlugin());
            config.staticFiles.add(staticFiles -> {
                staticFiles.location = Location.CLASSPATH;
                staticFiles.directory = "/frontend";
                staticFiles.mimeTypes.add(ContentType.TEXT_JS);
            });
            config.spaRoot.addFile("/", "/frontend/index.html", Location.CLASSPATH);
        });

        app.before(ctx -> {
            if (DEBUG) {
                ctx.header("Access-Control-Allow-Origin", "http://localhost:3000");
            } else {
                ctx.header("Access-Control-Allow-Origin", AppUrl.orElseThrow());
            }
            ctx.header("Access-Control-Allow-Credentials", "true");
            ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        });

        app.post("api/login", ctx -> runAction(ctx, Main::login, false));
        app.post("api/logout", ctx -> runAction(ctx, Main::logout, false));
        app.post("api/login/reset", ctx -> runAction(ctx, Main::sendResetPasswordLink, false));
        app.post("api/login/token", ctx -> runAction(ctx, Main::resetPasswordByToken, false));
        app.post("api/login/check", ctx -> runAction(ctx, Main::validToken, false));
        app.get("api/refresh", ctx -> runAction(ctx, Main::refresh, false));

        app.post("api/start", ctx -> runAction(ctx, Users::startTracking, true));
        app.post("api/stop", ctx -> runAction(ctx, Users::stopTracking, true));
        app.post("api/add", ctx -> runAction(ctx, Users::addPersonalProject, true));
        app.post("api/active", ctx -> runAction(ctx, Users::getActive, true));
        app.post("api/month", ctx -> runAction(ctx, Users::getMonth, true));
        app.post("api/deleteProject", ctx -> runAction(ctx, Users::deleteUserProject, true));
        app.post("api/data", ctx -> runAction(ctx, Users::getDataToAnalyse, true));
        app.post("api/delete", ctx -> runAction(ctx, Users::deleteTracking, true));
        app.post("api/edit", ctx -> runAction(ctx, Users::updateProject, true));
        app.post("api/update", ctx -> runAction(ctx, Users::updateTracking, true));
        app.post("api/export", ctx -> runAction(ctx, Users::exportData, true));
        app.post("api/archive", ctx -> runAction(ctx, Users::archiveProject, true));
        app.post("api/user/updatePassword", ctx -> runAction(ctx, Users::updatePassword, true));
        app.post("api/user/changeMail", ctx -> runAction(ctx, Users::updateUserMail, true));
        app.post("api/user/createToken", ctx -> runAction(ctx, Users::createUserApiToken, true));
        app.post("api/user/deleteToken", ctx -> runAction(ctx, Users::deleteToken, true));
        app.get("api/role", ctx -> runAction(ctx, Users::getUserRole, true));
        app.get("api/archived", ctx -> runAction(ctx, Users::getUserArchivedProjects, true));
        app.get("api/projects", ctx -> runAction(ctx, Users::getUserProjects, true));
        app.get("api/user/leaveGroup", ctx -> runAction(ctx, Groups::userLeaveGroup, true));
        app.get("api/user/data", ctx -> runAction(ctx, Users::getUserData, true));
        app.get("api/user/delete", ctx -> runAction(ctx, Users::deleteAccount, true));
        app.get("api/user/listTokens", ctx -> runAction(ctx, Users::getUserTokens, true));

        app.post("api/group/create", ctx -> runAction(ctx, Groups::groupUserCreateGroup, true));
        app.post("api/group/update", ctx -> runAction(ctx, Groups::groupUpdate, true));
        app.post("api/group/invite", ctx -> runAction(ctx, Groups::groupUserInvite, true));
        app.post("api/group/remove", ctx -> runAction(ctx, Groups::groupUserRemove, true));
        app.post("api/group/addProject", ctx -> runAction(ctx, Groups::groupAddProject, true));
        app.post("api/group/deleteProject", ctx -> runAction(ctx, Groups::groupDeleteProject, true));
        app.post("api/group/deleteGroup", ctx -> runAction(ctx, Groups::groupDelete, true));
        app.post("api/group/details", ctx -> runAction(ctx, Groups::getGroupDetails, true));
        app.post("api/group/data", ctx -> runAction(ctx, Groups::getGroupDataToAnalyse, true));
        app.post("api/group/export", ctx -> runAction(ctx, Groups::exportData, true));
        app.get("api/group", ctx -> runAction(ctx, Groups::getManagedGroups, true));

        app.post("api/admin/invite", ctx -> runAction(ctx, Admins::adminAddNewUser, true));
        app.post("api/admin/updateRole", ctx -> runAction(ctx, Admins::adminUpdateRoles, true));
        app.post("api/admin/deleteUser", ctx -> runAction(ctx, Admins::adminDeleteUser, true));
        app.post("api/admin/setPrivacy", ctx -> runAction(ctx, Admins::setPrivacyHtml, true));
        app.get("api/admin", ctx -> runAction(ctx, Admins::getAdminData, true));

        app.get("api/validate", ctx -> runAction(ctx, Main::validate, true));
        app.get("api/info", ctx -> runAction(ctx, Main::getDepInfo, false));
        app.get("api/version", ctx -> runAction(ctx, Main::getVersion, false));
        app.get("api/privacy", ctx -> runAction(ctx, Main::getPrivacyInfo, false));
        return app;
    }

    @OpenApi(
            summary = "Refresh token from RefreshToken",
            operationId = "refresh",
            path = "/api/refresh",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
            }
    )
    private static void refresh(final Context ctx) throws SQLException {
        final String refreshToken = ctx.cookie("refresh");
        final Optional<Integer> userID = Auth.validateRefreshToken(refreshToken);
        if (userID.isPresent()) {
            Database.deleteToken(refreshToken, userID.get());
            Auth.setCookies(ctx, userID.get());
            final Response response = new Response(true);
            ctx.json(response);
            ctx.status(HttpStatus.OK);
        } else {
            ctx.status(HttpStatus.UNAUTHORIZED);
        }
    }

    @OpenApi(
            summary = "Logout",
            operationId = "logout",
            path = "/api/logout",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
            }
    )
    private static void logout(final Context ctx) throws SQLException {
        final int userId = Auth.getUserFromContext(ctx);
        ctx.removeCookie("jwt", "/api");
        ctx.removeCookie("refresh", "/api");
        Database.deleteAllRefreshTokensForUser(userId);
        ctx.status(200).result("Logged out");
    }

    @OpenApi(
            summary = "Login",
            operationId = "login",
            path = "/api/login",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = Login.class)},
                    description = "Login Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
                    @OpenApiResponse(status = "406", description = "NOT_ACCEPTABLE")
            }
    )
    private static void login(final Context ctx) throws SQLException {
        final Login login = ctx.bodyAsClass(Login.class);
        if (Auth.login(login)) {
            final Optional<Integer> userID = Database.getUserID(login.mail());
            if (userID.isPresent()) {
                Auth.setCookies(ctx, userID.get());
                final Response response = new Response(true);
                ctx.json(response);
            } else {
            ctx.status(HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            ctx.status(HttpStatus.UNAUTHORIZED);
        }
    }

    @OpenApi(
            summary = "Request Password reset",
            operationId = "login/reset",
            path = "/api/login/reset",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = String.class, example = "user@mail.com")},
                    description = "Email String",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "204", description = "NO_CONTENT")
            }
    )
    private static void sendResetPasswordLink(final Context ctx) throws SQLException, MessagingException, IOException {
        final String mail = ctx.bodyAsClass(String.class);
        final Optional<Integer> userId = Database.getUserID(mail);
        if (userId.isPresent()) {
            final String token = UUID.randomUUID().toString();
            final boolean result = Database.resetPasswordToken(token, userId.get());
            Mail.sendPasswordReset(mail, token);
            Responses.setResponseOrError(ctx, result);
        }
    }

    @OpenApi(
            summary = "Validate Token for Password reset",
            operationId = "login/check",
            path = "/api/login/check",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = String.class, example = "PASSWORD_RESET_TOKEN")},
                    description = "Password Reset Token String",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "400", content = @OpenApiContent(from = Response.class, example = "{\"payload\":false}")),
                    @OpenApiResponse(status = "204", description = "NO_CONTENT")
            }
    )
    private static void validToken(final Context ctx) throws SQLException {
        final String token = ctx.bodyAsClass(String.class);
        final Optional<UserToken> userToken = Database.getToken(token);
        if (userToken.isPresent()) {
            Responses.setResponseOrError(ctx, true);
        } else {
            Responses.setResponseOrError(ctx, false);
        }
    }

    @OpenApi(
            summary = "Reset User Password",
            operationId = "login/token",
            path = "/api/login/token",
            methods = HttpMethod.POST,
            requestBody = @OpenApiRequestBody(
                    content = {@OpenApiContent(from = PasswordReset.class)},
                    description = "Password Reset Object",
                    required = true
            ),
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class, example = "{\"payload\":true}")),
                    @OpenApiResponse(status = "404", description = "NOT_FOUND"),
                    @OpenApiResponse(status = "406", description = "NOT_ACCEPTABLE")
            }
    )
    private static void resetPasswordByToken(final Context ctx) throws SQLException {
        final PasswordReset reset = ctx.bodyAsClass(PasswordReset.class);
        final Optional<UserToken> userToken = Database.getToken(reset.token());
        if (userToken.isPresent() && userToken.get().expiration().isPresent() && userToken.get().expiration().get().after(Timestamp.from(Instant.now()))) {
            final Optional<String> userMail = Database.getUserMail(userToken.get().user());
            if (userMail.isPresent()) {
                final boolean res = Database.updatePassword(userToken.get().user(), Auth.hashPassword(reset.newPassword()));
                Responses.setResponseOrError(ctx, res);
            } else {
                ctx.status(HttpStatus.NOT_FOUND);
            }
        } else {
            ctx.status(HttpStatus.NOT_ACCEPTABLE);
        }
    }

    @OpenApi(
            summary = "Validate if cookie is still valid",
            operationId = "validate",
            path = "/api/validate",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
                    @OpenApiResponse(status = "401", description = "UNAUTHORIZED"),
            }
    )
    private static void validate(final Context ctx) {
        Responses.setResponseOrError(ctx, true);
    }

    @OpenApi(
            summary = "Get version ",
            operationId = "version",
            path = "/api/version",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = Response.class)),
            }
    )
    private static void getVersion(final Context ctx) {
        Responses.setResponseOrError(ctx, Util.getVersionInfo());
    }

    private static void runAction(final Context ctx, final Action func, final boolean authRequired){
        if (authRequired) {
            if (!Auth.validateToken(ctx)) {
                ctx.status(HttpStatus.UNAUTHORIZED);
                return;
            }
        }
        try {
            func.run(ctx);
        } catch (final Exception e) {
            logger.error("for path: {}", ctx.path(), e);
            ctx.status(500);
            System.out.println(ctx.contextPath());
            System.out.println(e.getMessage());
        }
    }

    public static List<Deps> loadFrontendDeps() throws IOException {

        try (
                final InputStream is = Objects.requireNonNull(Main.class.getResourceAsStream("/frontend-deps.csv"));
                final BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            return reader.lines().skip(1).map(line -> Arrays.asList(line.split(",")))
                    .map(l -> {
                                final String[] moduleName = l.get(0).substring(1, l.get(0).length()-1).split("@");
                                return moduleName.length == 3 ?
                                new Deps( moduleName[1], moduleName[2], l.get(3).replace("\"",""), l.get(1).replace("\"",""))
                                        : new Deps( moduleName[0], moduleName[1], l.get(3).replace("\"",""), l.get(1).replace("\"",""));
                            }).toList();
        }
    }

    public static List<Deps> loadBackendDeps() throws IOException {
        try (
                final InputStream is = Objects.requireNonNull(Main.class.getResourceAsStream("/backend-deps.csv"));
                final BufferedReader reader = new BufferedReader(new InputStreamReader(is))
        ) {
            return reader.lines()
                    .skip(1) // Skip header
                    .map(line -> Arrays.asList(line.split(";")))
                    .map(l -> new Deps(l.get(1), l.get(2), l.get(4), l.get(3)))
                    .toList();
        }
    }

    @OpenApi(
            summary = "Info",
            operationId = "info",
            path = "/api/info",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = DepInfo.class)),
            }
    )
    private static void getDepInfo(final Context ctx) throws IOException {
        final DepInfo info = new DepInfo(loadFrontendDeps(), loadBackendDeps(), Util.getVersionInfo());
        Responses.setResponseOrError(ctx, info);
    }

    @OpenApi(
            summary = "Get Link to Privacy Policy or Html as String",
            operationId = "privacy",
            path = "/api/privacy",
            methods = HttpMethod.GET,
            responses = {
                    @OpenApiResponse(status = "200", content = @OpenApiContent(from = PrivacyInfo.class)),
            }
    )
    private static void getPrivacyInfo(final Context ctx) {
        final Optional<String> url = Util.getEnvVar("PRIVACY_URL", s -> s, false);
        if (url.isPresent()) {
            Responses.setResponseOrError(ctx, new PrivacyInfo(url.get(), ""));
        } else {
            if (PRIVACY_HTML.toFile().exists()) {
                try {
                    final String privacyHtml = Jsoup.clean(Files.readString(PRIVACY_HTML), Safelist.relaxed());
                    Responses.setResponseOrError(ctx, new PrivacyInfo("", privacyHtml));
                    return;
                } catch (final IOException e) {
                    logger.info("No data available or set for privacy link, nothing will be displayed. To Remove this warning set envVar PRIVACY_URL or set it in Settings");
                }
            }
        }
        Responses.setResponseOrError(ctx, new PrivacyInfo("", ""));
    }
}
