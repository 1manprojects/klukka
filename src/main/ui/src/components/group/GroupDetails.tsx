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
import { GroupDetails as G, Project} from "../../datatypes/types";
import { DeleteProjectFromGroup, getGroupDetails, inviteUserToGroup, removeUserFromGroup, updateGroup } from "../../Api";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTrashAlt } from "@fortawesome/free-solid-svg-icons";
import { Add } from "../common/add/Add";
import { AddProject } from "../common/addProject/AddProject";

import './groupDetails.scss'
import { MinToStringWithoutSeconds, showToast, showToastMessage } from "../../Func";
import { Activity } from "../user/Activity";
import { EditProject } from "../common/editProject/EditProject";

export interface GroupDetailProps {
    groupId: number;
    onClose: () => void;
}

export type DialogType = "NONE" | "NEW" | "EDIT";

export const GroupDetails = (props: GroupDetailProps): ReactElement => {

    const [group, setGroup] = useState<G | null>(null)
    const [newUser, setNewUser] = useState<string>("");
    const [dialog, setDialog] = useState<DialogType>("NONE");
    const [selectedProject, setSelectedProject] = useState<number>(-1);

    const fetchData = async(): Promise<void> => {
        const data = await getGroupDetails(props.groupId)
        setGroup(data)
    }

    useEffect(()=>{
        fetchData();
    },[props.groupId])

    const closeDialog = async (): Promise<void> => {
        fetchData();
        setDialog("NONE");
    }

    const validUserMail = () : boolean => {
        const regexp = new RegExp(/^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/);
        return !regexp.test(newUser);
    }

    const totalGroupTime = (): number => {
        if (group) {
            let res = 0;
            group.projects.forEach(p => res += p.trackedThisMonth);
            return res;
        }
        return 0;
    }

    const updateTitle = (newTitle: string): void => {
        if (group) {
            const newGroup = {...group};
            newGroup.group.title = newTitle;
            setGroup(newGroup);
        }
    }

    const updateDescription = (newDesc: string): void => {
        if (group) {
            const newGroup = {...group};
            newGroup.group.description = newDesc;
            setGroup(newGroup);
        }
    }

    const removeUser = async(mail: string): Promise<void> => {
        const res = await removeUserFromGroup({groupId: props.groupId, mail: mail})
        showToast(res, "User removed", "Error removing user");
        if (res) {
            fetchData();
        }
    }

    const inviteUser = async(): Promise<void> => {
        const res = await inviteUserToGroup({groupId: props.groupId, mail: newUser});
        showToastMessage(res);
        if (res) {
            fetchData();
            setNewUser("");
        }
    }

    const update = async (): Promise<void> => {
        if (group) {
            const res = await updateGroup(group?.group);
            showToast(res, "Group updated", "Error updating group");
            fetchData();
        }
    }

    const deleteProject = async (e: React.MouseEvent, projectId: number): Promise<void> => {
        e.preventDefault();
        if (confirm("Do you realy want to delete a Project from the Group!\nAll Tracked Data will be deleted")) {
            const res = await DeleteProjectFromGroup({id1:props.groupId, id2:projectId});
            if (res) {
                fetchData();
            }
        }
    }

    const renderUser = (): ReactElement => {
        return <div className="users">
            <h2>User Management</h2>
            <table>
            <thead>
                <tr>
                    <th>Id</th>
                    <th>Mail</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {group?.users.map(u => <tr key={u.id}>
                    <td>{u.id}</td>
                    <td>{u.mail}</td>
                    <td>
                        <FontAwesomeIcon icon={faTrashAlt} onClick={()=>removeUser(u.mail)}/>
                    </td>
                </tr>)}
                <tr>
                    <td>new</td>
                    <td><input value={newUser} onChange={(e)=> setNewUser(e.target.value)}/></td>
                    <td><button disabled={validUserMail()} onClick={inviteUser}>Invite</button></td>
                </tr>
            </tbody>
            </table>
        </div>
    }

    const renderProject = (p: Project): ReactElement => {
        return <div className='project'
          onClick={() => {setSelectedProject(p.id); setDialog("EDIT")}}
          key={p.id}
          style={{ backgroundColor: p.color, backgroundImage: "linear-gradient(to right,"+p.color+", white 20%)" }}>
          <span className='title'>{p.title}</span>
          <div className="desc">
            <div/>
            <label className='description'>{p.description} </label>
            <label className='tracked'>{"tracked: " + MinToStringWithoutSeconds(p.trackedThisMonth)} </label>
            <div className="delete"><FontAwesomeIcon icon={faTrashAlt} onClick={(e) => deleteProject(e, p.id)}/></div>
          </div>
        </div>
      }

    const renderView = (): ReactElement => {
        if (dialog != "NONE") {
            if (dialog === "EDIT") {
                return <EditProject project={group?.projects.find(p => p.id === selectedProject)} close={closeDialog}/>
            }
            if (dialog === "NEW") {
                return <AddProject close={closeDialog} groupId={props.groupId}/>
            }
        } else {
            return ( <Fragment>
            <div className="group-data">
                <h2>{"Group Details:"}</h2>
                <label>Title</label>
                <input value={group? group.group.title : ""} onChange={(e)=>updateTitle(e.target.value)}/>
                <label>description</label>
                <input value={group? group.group.description: ""} onChange={(e)=>updateDescription(e.target.value)}/>
                <div className="buttons">
                    <button className="cancel" onClick={() => props.onClose()}>Back</button>
                    <button className="ok" onClick={() => update()}>Save</button>
                </div>
            </div>
            {renderUser()}
            <h2>Group Projects</h2>
            <div className='projects-list'>
                {group?.projects.map(p => renderProject(p))}
                <div className='total-month'>
                    <span>{"This Month: " + MinToStringWithoutSeconds(totalGroupTime())}</span>
                </div>
            </div>
            <Add onClick={() => setDialog("NEW")}/>
            <Activity groupId={props.groupId}/>
            </Fragment>
            )
        }
    }

    return <div className="group-details">
        {renderView()}
    </div>
}
