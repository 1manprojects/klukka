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
import { Project } from "../../../datatypes/types";
import { addProject, addProjectToGroup } from "../../../Api";
import './addProject.scss'
import { showToast } from "../../../Func";

export interface AddProps {
    close: () => void;
    groupId?: number;
}

export const AddProject = (props: AddProps): ReactElement => {

    const [project, setProject] = useState<Project>({
        color: "#"+Math.floor(Math.random() * 16777215).toString(16),
        ref: -1,
        refType: "USER",
        description: "",
        id: -1,
        title: "",
        trackedThisMonth: 0,
        archived: false,
    })
    const [error, setError] = useState<string | undefined>(undefined)

    const add = async (): Promise<void> => {
        if (props.groupId) {
            const newProject = {...project, ref: props.groupId}
            const res = await addProjectToGroup(newProject);
            showToast(res, "Project added", "Error adding project");
            if (res) {
                props.close();
            }
        } else {
            const id = await addProject(project);
            if (id > 0) {
                props.close();
            } else {
                setError("ERROR! Could not add Project")
            }
        }
    }

    return <div className="new-project">
        <label>Project Title</label>
        <input placeholder="Titel" type="text" value={project.title} onChange={(e) => setProject({ ...project, title: e.target.value })} />

        <label>Description of Project</label>
        <input placeholder="Description" type="text" value={project.description} onChange={(e) => setProject({ ...project, description: e.target.value })} />

        <label>Color</label>
        <input className="color-select" placeholder="color" type="color" value={project.color} onChange={(e) => setProject({ ...project, color: e.target.value })} />

        {error ? <span className="Error">{error}</span> : null}

        <div className="buttons">
            <button className="cancel" onClick={() => props.close()}>Cancel</button>
            <button className="ok" onClick={() => add()}>Add</button>
        </div>
    </div>
}
