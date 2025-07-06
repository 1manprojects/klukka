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
import { Fragment, ReactElement, useEffect, useState } from "react";
import { Group, PrivacyInfo, Role, User } from "../../datatypes/types";
import { adminDelUser, adminInvite, adminUpdateUserRole, getAdminData, getPrivacyInfo, updatePrivacyInfo } from "../../Api";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck, faPenAlt, faTrashAlt, faXmark } from "@fortawesome/free-solid-svg-icons";
import Select, { MultiValue } from 'react-select'

import './admin.scss'

export const Admin = (): ReactElement => {

    const emptyUser: User = {id:-1,mail:"",roles:["USER"]};
    const [users, setUsers] = useState<User[]>([]);
    const [newUser, setNewUser] = useState<User>(emptyUser);
    const [editing, setEditing] = useState<number| null>(null)
    const [groups, setGroups] = useState<Group[]>([]);
    const [privacyInfo, setPrivacyInfo] = useState<PrivacyInfo>({link: "", html: ""});

    const fetchAndSetData = async () : Promise<void> => {
        const data = await getAdminData();
        if (data !== null) {
            setUsers(data.users);
            setGroups(data.groups);
        }
        const res = await getPrivacyInfo();
        if (res !== null) {
            setPrivacyInfo(res);
        }
    }

    useEffect(() => {
        fetchAndSetData();
    },[])

    const clearEditMode = async () : Promise<void> => {
        setEditing(null);
        fetchAndSetData();
    }

    const regexMail = new RegExp(/^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/);

    const addNewUser = async() : Promise<void> => {
        if (newUser.mail.length > 0) {
            if (regexMail.test(newUser.mail)) {
                await adminInvite(newUser);
                setNewUser(emptyUser)
                fetchAndSetData();
            } else {
                //error
            }
        }
    }

    const updatePrivacyHtml = async () : Promise<void> => {
        if (privacyInfo !== null) {
            await updatePrivacyInfo({link:"", html: privacyInfo.html});
            fetchAndSetData();
        }
    }

    const updateUserRole = (id: number, e :MultiValue<{value: string;label: string;} | undefined>) : void => {
        const currentUser = [...users] 
        const index = currentUser.findIndex(u => u.id === id);
        if (index >= 0) {
            const newRoles: Role[] = e.map(t => toRole(t?.value));
            const unique: Role[] = [...new Set(newRoles.map(item => item))];
            currentUser[index].roles = unique;
            setUsers(currentUser);
        }
    }

    const deleteUser = async(id :number): Promise<void> => {
        if (window.confirm("This will delete the User and all his Data from the Server!\nWarning this cannot be undone!")) {
            await adminDelUser(id)
            fetchAndSetData();
        }
    }

    const updateUserRoles = async (id: number): Promise<void> => {
        if (editing !== null) {
            const userToUpdate: User | undefined = users.find(u => u.id === id);
            if (userToUpdate) {
                await adminUpdateUserRole(userToUpdate);
                setEditing(null);
                fetchAndSetData();
            }
        }
    }

    const roles = [
        {value: "ADMIN", label: "Admin"},
        {value: "GROUP", label: "Group Admin"},
        {value: "USER", label: "User"}
    ]

    const mapRoles = (rs: Role[]): {value: string; label: string; }[] => {
        return rs.map(r => roles.find(t => t.value === r));
    }

    const toRole = (val: string | undefined) :Role=> {
        if (val) {
            return val as Role;
        }
        else {
            return "USER";
        }
    }

    const updateRoles = (e :MultiValue<{value: string;label: string;} | undefined>) : void => {
        if (e) {
            const newRoles: Role[] = e.map(t => toRole(t?.value));
            const unique: Role[] = [...new Set(newRoles.map(item => item))];
            setNewUser({...newUser, roles: unique});
        }
    }

    const groupOwners = users.map(u => {return {value: u.id, label: u.mail}})

    const renderUser = () : ReactElement => {
        return <div className="users">
            <h2>User Management</h2>
            <table>
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Mail</th>
                    <th>Roles</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {users.map(u => <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.mail}</td>
                    <td>{((editing !== null && editing === u.id) ? <Select isMulti value={mapRoles(u.roles)} options={roles} onChange={(e) => updateUserRole(u.id, e)}/>: u.roles.join(","))}</td>
                    <td>
                        <FontAwesomeIcon icon={faTrashAlt} onClick={()=>deleteUser(u.id)}/>
                        {((editing !== null && editing === u.id))? 
                        <Fragment>
                            <FontAwesomeIcon icon={faXmark} onClick={()=>clearEditMode()}/>
                            <FontAwesomeIcon icon={faCheck} onClick={()=>updateUserRoles(u.id)}/>
                        </Fragment> : <FontAwesomeIcon icon={faPenAlt} onClick={()=>setEditing(u.id)}/>}
                    </td>
                </tr>)}
                <tr>
                    <td>new</td>
                    <td><input value={newUser.mail} onChange={(e)=> setNewUser({...newUser, mail:e.target.value})}/></td>
                    <td>
                        <Select
                            isMulti
                            value={mapRoles(newUser.roles)}
                            options={roles}
                            onChange={(e) => updateRoles(e)}
                        />
                    </td>
                    <td><button onClick={addNewUser}>Invite</button></td>
                </tr>
            </tbody>
            </table>
        </div>
    }

    const renderGroups = () : ReactElement => {
        return <div className="groups">
            <h2>Group Management</h2>
            <table>
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Title</th>
                    <th>Description</th>
                    <th>Owner</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {groups.map(g => <tr key={g.id}>
                    <td>{g.id}</td>
                    <td>{g.title}</td>
                    <td>{g.description}</td>
                    <td>{users.find(u => u.id === g.owner)?.mail}</td>
                    <td><FontAwesomeIcon icon={faTrashAlt}/></td>
                </tr>)}
                <tr>
                    <td>new</td>
                    <td><input/></td>
                    <td><input/></td>
                    <td><Select options={groupOwners}/></td>
                    <td><button>create</button></td>
                </tr>
            </tbody>
            </table>
        </div>
    }

    const renderPrivacyEditor = () : ReactElement => {
        return <div className="privacy">
            <h2>Privacy Html</h2>
            <p>If no URL ist set by envVar PRIVACY_URL you can edit the html to display youre privacy policy here</p>
            {(privacyInfo !== null && privacyInfo.link !== "")? <span>PRIVACY_URL is set any input here will be ignored</span> : null}
            <textarea className="privacy-editor" placeholder="Privacy Policy HTML" value={privacyInfo.html} onChange={e => setPrivacyInfo({...privacyInfo, html: e.target.value})}/>
            <button onClick={updatePrivacyHtml}>Save</button>
        </div>
    }

    return <div className="admin-view">
        <div className="management">
            {renderUser()}
            {renderGroups()}
            {renderPrivacyEditor()}
        </div>
    </div>
}
