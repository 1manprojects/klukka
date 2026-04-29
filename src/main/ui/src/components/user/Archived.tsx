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
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faBoxOpen, faTrashAlt } from "@fortawesome/free-solid-svg-icons";
import { Project, UserProjects as P } from "../../datatypes/types";
import { ReactElement, useEffect, useState } from "react";
import { getArchived, setProjectArchive, userDelProject } from "../../Api";

import './projects.scss';


export const Archived = (): ReactElement => {

    const [archived, setArchived] = useState<P | null>(null)
  
    const fetchAndSetData = async (): Promise<P> => {
        const projects = await getArchived();
        setArchived(projects);
        return archived;
    }

    useEffect(() => {
      fetchAndSetData();
    }, [])

    const deleteProject = async (e: React.MouseEvent, id: number): Promise<void> => {
      e.stopPropagation();
      if (confirm("Do you realy want to delete the Project!\nAll tracked data will be deleted!")) {
        await userDelProject(id);
        await fetchAndSetData();
      }
    }

    const unArchive = async (e: React.MouseEvent, id: number): Promise<void> => {
        e.stopPropagation();
        const archiveString = "Do you want to unarchive the project?";

        if (window.confirm(archiveString)) {
            const res = await setProjectArchive({archive: false, projectId: id});
            if (res) {
                fetchAndSetData();
            }
        }
    }
  
    const renderProject = (p: Project): ReactElement => {
      return <div className='project archived'
        key={p.id}
        style={{ backgroundColor: p.color, backgroundImage: "linear-gradient(to right,"+p.color+", white 20%)" }}>
        <span className='title'>{p.title}</span>
        <div className="desc">
          <div/>
          <label className='description'>{p.description} </label>
          <label className='tracked'></label>
          <div className="actions">
            <div className="archive"><FontAwesomeIcon icon={faBoxOpen} onClick={(e) => unArchive(e, p.id)}/></div>
            <div className="delete"><FontAwesomeIcon icon={faTrashAlt} onClick={(e) => deleteProject(e, p.id)}/></div>
          </div>
          
        </div>
      </div>
    }
  
    return (
      <div className="App">
        <h2>Archived Projects</h2>
        <div className='projects-list'>
            {archived?.own.map(p => renderProject(p))}
        </div>
      </div>
    );


}
