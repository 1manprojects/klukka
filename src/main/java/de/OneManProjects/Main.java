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
import de.OneManProjects.api.*;
import de.OneManProjects.data.dto.DepInfo;
import de.OneManProjects.data.dto.Deps;
import de.OneManProjects.data.dto.PrivacyInfo;
import de.OneManProjects.data.dto.Response;
import de.OneManProjects.database.Database;
import de.OneManProjects.scheduler.Scheduler;
import de.OneManProjects.security.Auth;
import de.OneManProjects.utils.OptionalTypeAdapter;
import de.OneManProjects.utils.Util;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
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
import java.util.*;

import static de.OneManProjects.api.Admins.PRIVACY_HTML;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static void main() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final Optional<Integer> port = Util.getEnvVar("APPLICATION_PORT", Integer::parseInt, false);
        final Optional<String> AppUrl = Util.getEnvVar("APPLICATION_URL", s -> s, true);
        final boolean DEBUG = Boolean.getBoolean("debug");

        try {
            Database.initDataBase();
        } catch (final SQLException e) {
            System.out.println(e.getMessage());
            System.exit(2);
        }

        final Scheduler scheduler = new Scheduler();
        scheduler.start();

        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::stop));

        final Javalin app = createJavalinApp(DEBUG, AppUrl);
        app.start(port.orElse(3001));
    }

    public static Javalin createJavalinApp(final boolean DEBUG, final Optional<String> AppUrl) {

        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        new TypeToken<Optional<Timestamp>>() {
                        }.getType(),
                        new OptionalTypeAdapter<>(new Gson().getAdapter(Timestamp.class))
                )
                .registerTypeAdapter(
                        new TypeToken<Optional<Integer>>() {
                        }.getType(),
                        new OptionalTypeAdapter<>(new Gson().getAdapter(Integer.class))
                )
                .registerTypeAdapter(
                        new TypeToken<Optional<String>>() {
                        }.getType(),
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

        return Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> {
                if (DEBUG) {
                    it.allowHost("http://localhost:3000");
                } else {
                    it.allowHost(AppUrl.orElseThrow());
                }
                it.allowCredentials = true;
                it.exposeHeader("x-server");
                it.exposeHeader("Content-Disposition");
            }));
            config.jsonMapper(gsonMapper);
            config.registerPlugin(new OpenApiPlugin(openConfig ->
                    openConfig
                            .withDocumentationPath("/openapi")
                            .withPrettyOutput()
                            .withDefinitionConfiguration((version, definition) -> definition
                                    .withCookieAuth("jwtCookie", "jwt")
                                    .withBearerAuth("Authorization"))
            ));
            config.registerPlugin(new SwaggerPlugin());

            config.staticFiles.add(staticFiles -> {
                staticFiles.location = Location.CLASSPATH;
                staticFiles.directory = "/frontend";
                staticFiles.mimeTypes.add(ContentType.TEXT_JS);
            });
            config.spaRoot.addFile("/", "/frontend/index.html", Location.CLASSPATH);

            config.routes.before(ctx -> {
                if (DEBUG) {
                    ctx.header("Access-Control-Allow-Origin", "http://localhost:3000");
                } else {
                    ctx.header("Access-Control-Allow-Origin", AppUrl.orElseThrow());
                }
                ctx.header("Access-Control-Allow-Credentials", "true");
                ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
                ctx.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            });

            config.routes.apiBuilder(() -> {
                post("api/login", ctx -> runAction(ctx, Logins::login, false));
                post("api/logout", ctx -> runAction(ctx, Logins::logout, false));
                post("api/login/reset", ctx -> runAction(ctx, Logins::sendResetPasswordLink, false));
                post("api/login/token", ctx -> runAction(ctx, Logins::resetPasswordByToken, false));
                post("api/login/check", ctx -> runAction(ctx, Logins::validToken, false));
                get("api/refresh", ctx -> runAction(ctx, Logins::refresh, false));
                get("api/validate", ctx -> runAction(ctx, Logins::validate, true));

                post("api/start", ctx -> runAction(ctx, Users::startTracking, true));
                post("api/stop", ctx -> runAction(ctx, Users::stopTracking, true));
                post("api/add", ctx -> runAction(ctx, Users::addPersonalProject, true));
                get("api/active", ctx -> runAction(ctx, Users::getActive, true));
                get("api/month", ctx -> runAction(ctx, Users::getMonth, true));
                post("api/deleteProject", ctx -> runAction(ctx, Users::deleteUserProject, true));
                post("api/data", ctx -> runAction(ctx, Users::getDataToAnalyse, true));
                post("api/delete", ctx -> runAction(ctx, Users::deleteTracking, true));
                post("api/edit", ctx -> runAction(ctx, Users::updateProject, true));
                post("api/update", ctx -> runAction(ctx, Users::updateTracking, true));
                post("api/export", ctx -> runAction(ctx, Users::exportData, true));
                post("api/archive", ctx -> runAction(ctx, Users::archiveProject, true));
                post("api/comment", ctx -> runAction(ctx, Users::updateComment, true));
                get("api/role", ctx -> runAction(ctx, Users::getUserRole, true));
                get("api/archived", ctx -> runAction(ctx, Users::getUserArchivedProjects, true));
                get("api/projects", ctx -> runAction(ctx, Users::getUserProjects, true));

                post("api/user/updatePassword", ctx -> runAction(ctx, Users::updatePassword, true));
                post("api/user/changeMail", ctx -> runAction(ctx, Users::updateUserMail, true));
                post("api/user/createToken", ctx -> runAction(ctx, Users::createUserApiToken, true));
                post("api/user/deleteToken", ctx -> runAction(ctx, Users::deleteToken, true));
                get("api/user/leaveGroup", ctx -> runAction(ctx, Groups::userLeaveGroup, true));
                get("api/user/data", ctx -> runAction(ctx, Users::getUserData, true));
                get("api/user/delete", ctx -> runAction(ctx, Users::deleteAccount, true));
                get("api/user/listTokens", ctx -> runAction(ctx, Users::getUserTokens, true));

                post("api/group/create", ctx -> runAction(ctx, Groups::groupUserCreateGroup, true));
                post("api/group/update", ctx -> runAction(ctx, Groups::groupUpdate, true));
                post("api/group/invite", ctx -> runAction(ctx, Groups::groupUserInvite, true));
                post("api/group/remove", ctx -> runAction(ctx, Groups::groupUserRemove, true));
                post("api/group/addProject", ctx -> runAction(ctx, Groups::groupAddProject, true));
                post("api/group/deleteProject", ctx -> runAction(ctx, Groups::groupDeleteProject, true));
                post("api/group/deleteGroup", ctx -> runAction(ctx, Groups::groupDelete, true));
                post("api/group/details", ctx -> runAction(ctx, Groups::getGroupDetails, true));
                post("api/group/data", ctx -> runAction(ctx, Groups::getGroupDataToAnalyse, true));
                post("api/group/export", ctx -> runAction(ctx, Groups::exportData, true));
                get("api/group", ctx -> runAction(ctx, Groups::getManagedGroups, true));

                post("api/admin/invite", ctx -> runAction(ctx, Admins::adminAddNewUser, true));
                post("api/admin/updateRole", ctx -> runAction(ctx, Admins::adminUpdateRoles, true));
                post("api/admin/deleteUser", ctx -> runAction(ctx, Admins::adminDeleteUser, true));
                post("api/admin/deleteGroup", ctx -> runAction(ctx, Admins::adminDeleteGroup, true));
                post("api/admin/setPrivacy", ctx -> runAction(ctx, Admins::setPrivacyHtml, true));
                get("api/admin", ctx -> runAction(ctx, Admins::getAdminData, true));

                get("api/info", ctx -> runAction(ctx, Main::getDepInfo, false));
                get("api/version", ctx -> runAction(ctx, Main::getVersion, false));
                get("api/privacy", ctx -> runAction(ctx, Main::getPrivacyInfo, false));
            });
        });
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

    private static void runAction(final Context ctx, final Action func, final boolean authRequired) {
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
                        final String[] moduleName = l.get(0).substring(1, l.get(0).length() - 1).split("@");
                        return moduleName.length == 3 ?
                                new Deps(moduleName[1], moduleName[2], l.get(3).replace("\"", ""), l.get(1).replace("\"", ""))
                                : new Deps(moduleName[0], moduleName[1], l.get(3).replace("\"", ""), l.get(1).replace("\"", ""));
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