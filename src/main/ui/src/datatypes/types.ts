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
/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.1.1185 on 2026-01-08 14:08:22.

export interface Group {
    id: number;
    title: string;
    description: string;
    owner: number;
}

export interface Project {
    id: number;
    ref: number;
    refType: RefType;
    title: string;
    description: string;
    color: string;
    archived: boolean;
    trackedThisMonth: number;
}

export interface Start {
    projectID: number;
    timeZone: string;
}

export interface User {
    id: number;
    mail: string;
    roles: Role[];
}

export interface AdminData {
    users: User[];
    groups: Group[];
    projects: Project[];
}

export interface AnalysisData {
    projects: Project[];
    groupProjects: Project[];
    tracked: any[];
}

export interface ArchiveId {
    projectId: number;
    archive: boolean;
}

export interface CommentUpdate {
    id: number;
    comment: string;
}

export interface DataFilter {
    start: string;
    end: string;
    groupId?: number;
}

export interface DepInfo {
    frontend: Deps[];
    backend: Deps[];
    version: string;
}

export interface Deps {
    name: string;
    version: string;
    url: string;
    license: string;
}

export interface ExportFilter {
    filter: DataFilter;
    detailed: boolean;
    groupId?: number;
}

export interface GroupDetails {
    group: Group;
    users: User[];
    projects: Project[];
}

export interface GroupToUser {
    groupId: number;
    mail: string;
}

export interface IdTupel {
    id1: number;
    id2: number;
}

export interface Login {
    mail: string;
    password: string;
}

export interface PasswordReset {
    token: string;
    newPassword: string;
}

export interface PrivacyInfo {
    link: string;
    html: string;
}

export interface Response {
    payload: any;
}

export interface UserApiToken {
    id: number;
    description: string;
    expiration?: Date;
}

export interface UserData {
    user: User;
    projects: Project[];
    groups: Group[];
    tokens: UserApiToken[];
}

export interface UserProjects {
    own: Project[];
    group: Project[];
}

export type RefType = "USER" | "GROUP";

export type Role = "USER" | "GROUP" | "ADMIN" | "ANALYST";
