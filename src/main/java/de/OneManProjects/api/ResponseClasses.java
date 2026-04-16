package de.OneManProjects.api;

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

import de.OneManProjects.data.Group;
import de.OneManProjects.data.Tracked;
import de.OneManProjects.data.dto.*;
import de.OneManProjects.data.enums.Role;
import io.javalin.openapi.OpenApiByFields;

import java.util.List;

record UserProjectsResponse(UserProjects payload) {
}

record GroupDetailsResponse(GroupDetails payload) {
}

record AnalysisDataResponse(AnalysisData payload) {
}

record AdminDataResponse(AdminData payload) {
}

record GroupResponse(Group payload) {
}

record BooleanResponse(Boolean payload) {
}

record StringResponse(String payload) {
}

record UserApiTokenResponse(List<UserApiToken> payload) {
}

record UserDataResponse(UserData payload) {
}

record RoleResponse(Role payload) {
}

record IntegerResponse(Role payload) {
}

@OpenApiByFields
record TrackedResponse(Tracked payload) {
}

record DoubleResponse(double payload) {
}