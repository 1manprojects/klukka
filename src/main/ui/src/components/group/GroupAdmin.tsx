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
import { ReactElement, useEffect, useState } from "react";
import { Group } from "../../datatypes/types";

import './groupAdmin.scss'
import { deleteGroup, getGroups } from "../../Api";
import { Add } from "../common/add/Add";
import { AddGroup } from "../common/addGroup/AddGroup";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTrashAlt } from "@fortawesome/free-solid-svg-icons";
import { GroupDetails } from "./GroupDetails";
import { showToast } from "../../Func";

export const GroupAdmin = (): ReactElement => {

    const [groups, setGroups] = useState<Group[]>([]);
    const [dialog, setDialog] = useState<boolean>(false);
    const [selected, setSelected] = useState<number | null>(null)

    const fetchAndSetData = async (): Promise<void> => {
        const data: Group[] = await getGroups();
        setGroups(data);
    }

    useEffect(()=> {
        fetchAndSetData();
    },[])

    const closeDialog = async (): Promise<void> => {
        setDialog(false);
        setSelected(null);
        await fetchAndSetData();
    }

    const onDeleteGroup = async (e: React.MouseEvent, id: number): Promise<void> => {
        e.stopPropagation();
        if (confirm("Do you realy want to delete the Group!")) {
          const res = await deleteGroup(id);
          showToast(res, "Group deleted", "Error deleting group");
          await fetchAndSetData();
        }
    }

    const renderGroup = (g: Group): ReactElement => {
        return (<div className='group'
        key={g.id}
        onClick={()=> setSelected(g.id)}
        >
        <span className='title'>{g.title}</span>
        <label className='description'>{g.description} </label>
        <div className="delete"><FontAwesomeIcon icon={faTrashAlt} onClick={(e) => onDeleteGroup(e, g.id)}/></div>
      </div>)
    }

    const renderView = (): ReactElement => {
        if (selected !== null && selected >= 0) {
            return <GroupDetails groupId={selected} onClose={closeDialog}/>
        }
        if (dialog === true) {
            return <AddGroup close={closeDialog} />
        }
        return (
            <div className="admin-group">
                <h2>Managed Groups</h2>
                {groups.map(g => renderGroup(g))}
                <Add onClick={()=> setDialog(true)}/>
            </div>
        )
    }


    return <div className="admin-group">
        {renderView()}
    </div>
}
