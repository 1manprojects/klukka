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
import { AnalysisData, Tracked } from "./datatypes/final";
import { AdminData, Login, Project, Role, Start, User, UserProjects, Group, GroupDetails, GroupToUser, IdTupel, DataFilter, ExportFilter, UserData, PasswordReset, ArchiveId, Response as R, UserApiToken, DepInfo, PrivacyInfo } from "./datatypes/types";

const BASE_URL = import.meta.env.VITE_BASE_URL as string;

interface BinaryResponse {
    blob: Blob;
    filename: string;
}

const checkResponse = async (response: Response): Promise<boolean> => {
    if (response.status === 200) {
        return true
    } else if (response.status === 401) {
        window.location.href = "/login";
        return Promise.reject();
    }
    return false
}

const runLogin = async (login: Login): Promise<boolean> => {
    const head = new Headers();
    head.append('Content-Type', 'application/json; charset=UTF-8');
    const response = await fetch(BASE_URL + "login", {
            method: 'POST',
            credentials: "include",
            body: JSON.stringify(login),
            headers: head
        });
    return await checkResponse(response);
}

const runLogout = async (): Promise<boolean> => {
    const response = await fetch(BASE_URL + "logout", {
            method: 'POST',
            body: "",
            credentials: "include",
        });
    if (response.ok) {
        window.location.href = "/login";
        return true;
    }
    return false;
}


const runPost = async (method: string, payload: any): Promise<any | null> => {
    const head = new Headers();
    head.append('Content-Type', 'application/json; charset=UTF-8');
    const doPost = async (): Promise<Response> => {
            return fetch(BASE_URL + method, {
                method: 'POST',
                body: JSON.stringify(payload),
                headers: head,
                credentials: 'include', // include cookies
            });
        };
    let response = await doPost();
    if (response.status === 401) {
        //try refresh token
        const refResponse = await fetch(BASE_URL + "refresh", {
            method: 'GET',
            headers: head,
            credentials: "include"
            });
        if (refResponse.status === 200) {
             response = await doPost();
        }
    }
    if (await checkResponse(response)) {
        const res: R = JSON.parse(await response.text())
        if (res.payload !== null) {
            return res.payload;
        }
    }
    return null;
}

const runGet = async (method: string): Promise<any | null> => {
    const head = new Headers();
    head.append('Content-Type', 'application/json; charset=UTF-8');
    const doGet = async (): Promise<Response> => {
                return fetch(BASE_URL + method, {
                    method: 'GET',
                    headers: head,
                    credentials: "include"
                });
            };
    let response: Response = await doGet();
    if (response.status === 401) {
        //try refresh token
        const refResponse = await fetch(BASE_URL + "refresh", {
            method: 'GET',
            headers: head,
            credentials: "include"
            });
        if (refResponse.status === 200) {
             response = await doGet();
        }
    }
    if (await checkResponse(response)) {
        const res: R = JSON.parse(await response.text())
        if (res.payload !== null) {
            return res.payload;
        }
    }
    return null;
}

export const runValidate = async (): Promise<boolean> => {
    const head = new Headers();
    head.append('Content-Type', 'application/json; charset=UTF-8');
    const response: Response = await fetch(BASE_URL + "validate", {
            method: 'GET',
            headers: head,
            credentials: "include"
        });
    if (response.status === 200) {
        return true
    } else if (response.status === 401) {
        return false;
    }
    return false
}

const runBinary = async (method: string, body: any): Promise<BinaryResponse | null> => {
    const head = new Headers();
    head.append('Content-Type', 'application/json; charset=UTF-8');
    const response = await fetch(BASE_URL + method, {
            method: 'POST',
            headers: head,
            credentials: "include",
            body: JSON.stringify(body),
        });
    if (await checkResponse(response)) {
        const blob = await response.blob();
        const contentDisposition = response.headers.get('Content-Disposition');
        let filename = 'export.csv';
        if (contentDisposition) {
            const match = contentDisposition.match(/filename="?(.+)"?/);
            if (match && match[1]) {
                filename = match[1];
            }
        }
        return { blob, filename };
    }
    return null;
}

const returnOrNull = <T> (res: any | null): T | null => {
    if (res !== null) {
        return res;
    }
    return null;
}

const returnOrDefault = <T> (res: any | null, toDefault: T): T => {
    if (res !== null) {
        return res;
    }
    return toDefault;
}

export const getVersion = async (): Promise<string> => {
    const res = await runGet("version");
    if (res !== null) {
        return res as string;
    }
    return "n/a";
}

export const sendLogin = async (login: Login) : Promise<boolean | null> => {
    const res = await runLogin(login);
    return returnOrDefault<boolean>(res, false)
}

export const sendLogout = async () : Promise<boolean> => {
    const res = await runLogout();
    return returnOrDefault<boolean>(res, false)
}

export const getUserRole = async () : Promise<Role> => {
    const res = await runGet("role");
    return returnOrDefault<Role>(res, "USER")
}

export const getAdminData = async(): Promise<AdminData | null> => {
    const res = await runGet("admin");
    return returnOrNull<AdminData>(res)
}

export const addProject = async (p: Project): Promise<number> => {
    const res = await runPost("add", p);
    return returnOrDefault<number>(res, -1)
}

export const getProjects = async (): Promise<UserProjects | null> => {
    const res = await runGet("projects");
    return returnOrNull<UserProjects>(res)
}

export const getActive = async (): Promise<Tracked | null> => {
    const res = await runPost("active", null);
    return returnOrNull<Tracked>(res)
}

export const startTracking = async (start: Start): Promise<boolean> => {
    const res = await runPost("start", start);
    return returnOrDefault<boolean>(res, false);
}

export const deleteTracking = async (id: number): Promise<boolean> => {
    const res = await runPost("delete", id);
    return returnOrDefault<boolean>(res, false);
}

export const stopTracking = async (p: number): Promise<boolean> => {
    const res = await runPost("stop", p);
    return returnOrDefault<boolean>(res, false);
}

export const getMonthMins = async (): Promise<number> => {
    const res = await runPost("month", null);
    return returnOrDefault<number>(res, 0);
}

export const adminInvite = async (user: User): Promise<boolean> => {
    const res = await runPost("admin/invite", user)
    return returnOrDefault<boolean>(res, false);
}

export const adminDelUser = async (id: number): Promise<boolean> => {
    const res = await runPost("admin/deleteUser", id)
    return returnOrDefault<boolean>(res, false);
}

export const adminUpdateUserRole = async (user: User): Promise<boolean> => {
    const res = await runPost("admin/updateRole", user)
    return returnOrDefault<boolean>(res, false);
}

export const userDelProject = async (id: number): Promise<boolean> => {
    const res = await runPost("deleteProject", id)
    return returnOrDefault<boolean>(res, false);
}


export const getGroups = async(): Promise<Group[]> =>{
    const res = await runGet("group")
    return returnOrDefault<Group[]>(res, []);
}

export const createNewGroup = async(newGroup: Group): Promise<boolean> =>{
    const res = await runPost("group/create", newGroup)
    return returnOrDefault<boolean>(res, false);
}

export const deleteGroup = async(groupId: number) : Promise<boolean> => {
    const res = await runPost("group/deleteGroup", groupId)
    return returnOrDefault<boolean>(res, false);
}

export const getGroupDetails = async(groupId: number) : Promise<GroupDetails | null> => {
    const res = await runPost("group/details", groupId)
    return returnOrNull<GroupDetails>(res);
}

export const inviteUserToGroup = async(ref: GroupToUser) : Promise<string> => {
    const res = await runPost("group/invite", ref)
    return returnOrDefault<string>(res, "Something went wrong");
}

export const removeUserFromGroup = async(ref: GroupToUser) : Promise<boolean> => {
    const res = await runPost("group/remove", ref)
    return returnOrDefault<boolean>(res, false);
}

export const updateGroup = async(group: Group) : Promise<boolean> => {
    const res = await runPost("group/update", group)
    return returnOrDefault<boolean>(res, false);
}

export const addProjectToGroup = async(project: Project) : Promise<boolean> => {
    const res = await runPost("group/addProject", project)
    return returnOrDefault<boolean>(res, false);
}

export const DeleteProjectFromGroup = async(tuple: IdTupel) : Promise<boolean> => {
    const res = await runPost("group/deleteProject", tuple)
    return returnOrDefault<boolean>(res, false);
}

export const getAnalysisData = async(filter: DataFilter) : Promise<AnalysisData | null> => {
    const res = await runPost("data", filter)
    return returnOrNull<AnalysisData>(res);
}

export const getGroupAnalysisData = async(filter: DataFilter) : Promise<AnalysisData | null> => {
    const res = await runPost("group/data", filter)
    return returnOrNull<AnalysisData>(res);
}

export const updatePassword = async(password: string) : Promise<boolean> => {
    const res = await runPost("user/updatePassword", password)
    return returnOrDefault<boolean>(res, false);
}

export const updateEmail = async(mail: string) : Promise<boolean> => {
    const res = await runPost("user/changeMail", mail)
    return returnOrDefault<boolean>(res, false);
}

export const deleteAccount = async() : Promise<boolean> => {
    const res = await runGet("user/delete")
    return returnOrDefault<boolean>(res, false);
}

export const leaveGroup = async(groupId: number) : Promise<boolean> => {
    const res = await runPost("user/leaveGroup", groupId)
    return returnOrDefault<boolean>(res, false);
}

export const updateTracking = async(tracked: Tracked) : Promise<boolean> => {
    const res = await runPost("update", tracked)
    return returnOrDefault<boolean>(res, false);
}

export const getUserDetails = async() : Promise<UserData | null> => {
    const res = await runGet("user/data")
    return returnOrNull<UserData>(res);
}

export const resetPassword = async(mail: string) : Promise<boolean> => {
    const res = await runPost("login/reset", mail)
    return returnOrDefault<boolean>(res, false);
}

export const checkToken = async(token: string) : Promise<boolean> => {
    const res = await runPost("login/check", token)
    return returnOrDefault<boolean>(res, false);
}

export const resetPasswordByToken = async(reset: PasswordReset) : Promise<boolean> => {
    const res = await runPost("login/token", reset)
    return returnOrDefault<boolean>(res, false);
}

export const setProjectArchive = async(data: ArchiveId) : Promise<boolean> => {
    const res = await runPost("archive", data)
    return returnOrDefault<boolean>(res, false);
}

export const updateProject = async(project: Project) : Promise<boolean> => {
    const res = await runPost("edit", project)
    return returnOrDefault<boolean>(res, false);
}

export const deleteUserToken = async(tokenId: number) : Promise<boolean> => {
    const res = await runPost("user/deleteToken", tokenId)
    return returnOrDefault<boolean>(res, false);
}

export const createUserToken = async(details: UserApiToken) : Promise<string | null> => {
    const res = await runPost("user/createToken", details)
    if (res !== null) {
        return res as string;
    }
    return null;
}

export const getDepInfo = async(): Promise<DepInfo| null> => {
    const res = await runGet("info");
    return returnOrNull<DepInfo>(res);
}

export const getPrivacyInfo = async(): Promise<PrivacyInfo| null> => {
    const res = await runGet("privacy");
    return returnOrNull<PrivacyInfo>(res);
}

export const updatePrivacyInfo = async(toUpdate: PrivacyInfo): Promise<boolean> => {
    const res = await runPost("admin/setPrivacy", toUpdate);
    return returnOrDefault<boolean>(res, false);
}

export const downloadExport = async(filter: ExportFilter, forGroupId?: number) : Promise<string | null> => {
    let res: BinaryResponse | null = null;
    if (forGroupId && forGroupId >= 0) {
        res = await runBinary("group/export", {...filter, groupId: forGroupId});
    } else {
        res = await runBinary("export", filter);
    }
    if (res !== null) {
        const url = window.URL.createObjectURL(res.blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = res.filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        return res.filename;
    }
    return null;
}
