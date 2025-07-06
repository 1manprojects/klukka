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
import { Fragment, ReactElement, useEffect, useState } from "react"
import { createUserToken, deleteAccount, deleteUserToken, getUserDetails, leaveGroup, setProjectArchive, updateEmail, updatePassword, userDelProject } from "../../Api";
import './profile.scss'
import { Project, UserApiToken, UserData } from "../../datatypes/types";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faBoxArchive, faBoxOpen, faDoorOpen, faEdit, faTrash } from "@fortawesome/free-solid-svg-icons";
import { EditProject } from "../common/editProject/EditProject";
import { getShortDate } from "../../Func";


export type DialogType = "NONE" | "PROJECT" | "GROUP";

export const Profile = (): ReactElement => {

    const [data, setData] = useState<UserData>({groups:[], projects:[], user: { id: -1, mail: "", roles: [] }, tokens: []});
    const [password, setPassword] = useState<{p1: string, p2: string}>({p1: "", p2: ""});
    const [mail, setMail] = useState<{edit: boolean, mail: string}>({edit: false, mail: ""});
    const [editProject, setProject] = useState<number>(-1);
    const [dialog, setDialog] = useState<DialogType>("NONE");
    const [newToken, setNewToken] = useState<UserApiToken>({id: -1, description: "", expiration: null});
    const [tokenValue, setTokenValue] = useState<string| null>(null);

    const fetchAndSetData = async (): Promise<void> => {
        const data = await getUserDetails();
        if (data !== null) {
            setData(data);
            setMail({mail: data.user.mail, edit: false});
        }
    }

    useEffect(() => {
        fetchAndSetData();
    },[])

    const isValidEmail = (email: string): boolean => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    const onUpdatePassword = async (): Promise<void> => {
        if (password.p1 === password.p2 && password.p1.length > 8) {
            const res = await updatePassword(password.p1);
            if (res) {
                window.sessionStorage.removeItem("authorization");
                window.location.href = "/login";
            }
        }
    }

    const onArchive = async (id: number, archive: boolean): Promise<void> => {

        const archiveString = archive ? "Do you want to unarchive the project?" : 
            "Do you want to archive this project?\n" +
            "Archived projects wont be visible in the project list.\n" +
            "You can unarchive them at any time.";

        if (window.confirm(archiveString)) {
                const res = await setProjectArchive({archive: archive, projectId: id});
                if (res) {
                    fetchAndSetData();
                }
        }
    }

    const onUpdateMail = async (): Promise<void> => {
        if (isValidEmail(mail.mail)) {
            if (window.confirm("Do you really want to change your mail to "+ mail.mail +"?\n" +
                "You will no longer be able to log in with your old mail address")) {
                if (window.confirm("After changing your mail you will be looged out and have to log in again with youre new mail address")) {
                    const res = await updateEmail(mail.mail);
                    if (res) {
                        window.sessionStorage.removeItem("authorization");
                        window.location.href = "/login";
                    }
                }
            }
            setMail({mail: "", edit: false});
        } else {
            alert("Invalid mail address");
        }
    }

    const onDeleteProject = async (id: number): Promise<void> => {
        if (window.confirm("Do you really want to delete this project?\n" +
            "This action cannot be undone and all data will be lost.")) {
                const res = await userDelProject(id);
                if (res) {
                    fetchAndSetData();
                }
        }
    }

    const onDeleteAccount = async (): Promise<void> => {
        if (window.confirm("Do you really want to delete your account?\n" +
            "This action cannot be undone and all data will be lost.")) {
                if (window.confirm("Are you absolutely sure you want to delete your account?")) {
                const res = await deleteAccount();
                if (res) {
                    window.sessionStorage.removeItem("authorization");
                    window.location.href = "/login";
                }
            }
        }
    }

    const onLeaveGroup = async (id: number): Promise<void> => {
        if (window.confirm("Do you really want to leave this group?\n" +
            "This action cannot be undone and all data will be lost.")) {
                const res = await leaveGroup(id);
                if (res) {
                    fetchAndSetData();
                }
        }
    }

    const onDeleteToken = async (id: number): Promise<void> => {
        if (window.confirm("Do you really want to delete this Api-Token?\n" +
            "This action cannot be undone and you can no longer access data with this token.")) {
                const res = await deleteUserToken(id);
                if (res) {
                    fetchAndSetData();
                }
        }
    }

    const onCreateApiToken = async (): Promise<void> => {
        if (newToken.description.length > 0) {
            const res = await createUserToken(newToken);
            setTokenValue(res);
            fetchAndSetData();
        } else {
            alert("Please enter a description for the token");
            return;
        }
    }

    const onCloseDialog = (): void => {
        setDialog("NONE");
        setProject(-1);
        fetchAndSetData();
    }

    const render = (): ReactElement => {
        switch (dialog) {
            case "PROJECT": {
                    const p: Project | undefined = data.projects.find((p) => p.id === editProject);
                    if (p) {
                        return <EditProject project={p} close={onCloseDialog}/>
                    }
                    return <></>
                }
            case "GROUP":
                return <></>
            default:
                return renderDefault();
        }
    }

    const renderDefault = (): ReactElement => {
        return  <Fragment><h1>Profile</h1>
        <div className="data">
            <div className="profile-info">
                <h2>User</h2>
                <div className="detail-info">
                    <label>E-mail:</label>
                    {
                        mail.edit ? 
                        <div>
                            <input type="text" value={mail.mail} onChange={(e) => setMail({...mail, mail: e.target.value})}/>
                            <button onClick={onUpdateMail}>Save</button>
                            <button onClick={() => setMail({mail:"", edit:false})}>Cancel</button>
                        </div>
                        : <p>{data.user.mail} <FontAwesomeIcon icon={faEdit} onClick={()=>setMail({...mail, edit: true})}/></p>
                    }
                </div>
                <div className="detail-info">
                <label>Roles:</label>
                <p>{data.user.roles.join(";")}</p>
                </div>
                <div className="change-password">
                    <label>Change Password</label>
                    <input type="password" placeholder="New Password" value={password.p1} onChange={(e) => setPassword({...password, p1: e.target.value})}/>
                    <input type="password" placeholder="Repeat Password" value={password.p2} onChange={(e) => setPassword({...password, p2: e.target.value})}/>
                    {password.p1 !== password.p2 ? <p className="warning">Passwords do not match</p> : null}
                    {password.p1.length < 8 ? <p className="warning">Passwords is to short</p> : null}
                    <button disabled={password.p1 !== password.p2} onClick={onUpdatePassword}>Change Password</button>
                </div>
            </div>
            <hr/>
            <h2>API Tokens</h2>
                {data.tokens.length === 0 ? <p>You do not have any Tokens</p> : null}
                <div className="new-token">
                    <input type="text" placeholder="Description" value={newToken.description} onChange={(e) => setNewToken({...newToken, description: e.target.value})}/>
                    <input type="date" value={newToken.expiration? getShortDate(newToken.expiration) : ""} onChange={(e) => setNewToken({...newToken, expiration: e.target.value ? new Date(e.target.value) : null})}/>
                    <button disabled={newToken.description.length <= 0} onClick={onCreateApiToken}>Create Token</button>
                    {tokenValue ? <div className="token-info">
                        <span className="info-text">New Api-Token created please save it you wont be shown this token again!</span>
                        <p className="token-value">API-Token: <span>{tokenValue}</span></p>
                        </div> : null}
                </div>
                <table className="tokens">
                    <thead>
                        <tr>
                            <th>Description</th>
                            <th>Expiration</th>
                            <th>Delete</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.tokens.map((token, index) => (
                            <tr key={index}>
                                <td>{token.description}</td>
                                <td>{token.expiration? new Date(token.expiration).toLocaleDateString() + "" : "-"}</td>
                                <td><FontAwesomeIcon icon={faTrash} onClick={() => onDeleteToken(token.id)}/></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            <hr/>
            <div className="project-info">
                <h2>Projects</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Description</th>
                            <th>Color</th>
                            <th>Archive</th>
                            <th>Delete</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.projects.map((project, index) => (
                            <tr key={index} className={project.archived ? "archived" : ""} onClick={() => { setProject(project.id); setDialog("PROJECT")}}>
                                <td>{project.title}</td>
                                <td>{project.description}</td>
                                <td style={{backgroundColor: project.color}}></td>
                                <td>{project.archived? 
                                    <FontAwesomeIcon className="action-icon" title="Unarchive Project" icon={faBoxOpen} onClick={()=> onArchive(project.id, false)}/>: 
                                    <FontAwesomeIcon className="action-icon" title="Archive Project" icon={faBoxArchive} onClick={()=> onArchive(project.id, true)}/>}</td>
                                <td><FontAwesomeIcon className="action-icon" title="Delete Archive" icon={faTrash} onClick={() => onDeleteProject(project.id)}/></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <hr/>
                <h2>Groups</h2>
                {data.groups.length === 0 ? <p>You are not in any group</p> : null}
                <table>
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Description</th>
                            <th>Leave Group</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.groups.map((group, index) => (
                            <tr key={index}>
                                <td>{group.title}</td>
                                <td>{group.description}</td>
                                <td><FontAwesomeIcon icon={faDoorOpen} onClick={() => onLeaveGroup(group.id)}/></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <hr/>
            <h2>Delete Account</h2>
            <span>This action cannot be undone</span>
            <button className="alert" onClick={onDeleteAccount}>Delete Account</button>
        </div>
        </Fragment>
    }

    return <div className="profile">
       {render()}
    </div>

}
