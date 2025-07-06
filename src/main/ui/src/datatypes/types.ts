/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.1.1185 on 2025-07-06 17:18:33.

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
