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
import React, { ReactElement } from "react";
import { Project } from "../../../datatypes/types";
import { updateProject } from "../../../Api";
import './editProject.scss'

export interface ProjectProps {
    project: Project;
    close: () => void;
}

export const EditProject = (props: ProjectProps): ReactElement => {

    const [editProject, setProject] = React.useState<Project>(props.project);

    const onUpdateProject = async (): Promise<void> => {
        const res = await updateProject(editProject);
        if (res) {
            props.close();
        }
    }

    return (
        <div className="editProject">
            <h1>Edit Project</h1>
            <label>Project Title</label>
            <input placeholder="Titel" type="text" value={editProject.title} onChange={(e) => setProject({ ...editProject, title: e.target.value })} />

            <label>Description of Project</label>
            <input placeholder="Description" type="text" value={editProject.description} onChange={(e) => setProject({ ...editProject, description: e.target.value })} />

            <label>Color</label>
            <input className="color-select" placeholder="color" type="color" value={editProject.color} onChange={(e) => setProject({ ...editProject, color: e.target.value })} />

            <div className="buttons">
                <button className="cancel" onClick={() => props.close()}>Cancel</button>
                <button className="ok" onClick={() => onUpdateProject()}>update</button>
            </div>
        </div>
    )

}
