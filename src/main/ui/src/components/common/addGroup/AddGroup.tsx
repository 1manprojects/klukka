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
import { ReactElement, useState } from "react";
import { AddProps } from "../addProject/AddProject";
import { Group } from "../../../datatypes/types";
import { createNewGroup } from "../../../Api";

import './addGroup.scss'
import { showToast } from "../../../Func";

export const AddGroup = (props: AddProps): ReactElement => {

    const [group, setGroup] = useState<Group>({
        id: -1,
        description: "",
        title: "",
        owner: -1
    })

    const add = async (): Promise<void> => {
        const res = await createNewGroup(group);
        showToast(res, "Created new Group", "Error creating Group");
        if (res) {
            props.close();
        }
    }

    return <div className="new-group">
        <label>Group Title</label>
        <input placeholder="Titel" type="text" value={group.title} onChange={(e) => setGroup({ ...group, title: e.target.value })} />

        <label>Description of Group</label>
        <input placeholder="Description" type="text" value={group.description} onChange={(e) => setGroup({ ...group, description: e.target.value })} />

        <div className="buttons">
            <button className="cancel" onClick={() => props.close()}>Cancel</button>
            <button className="ok" onClick={() => add()}>Add</button>
        </div>
    </div>
}
