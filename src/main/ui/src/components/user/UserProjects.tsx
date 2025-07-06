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
import { Fragment } from "react/jsx-runtime";
import { Tracking } from "../tracking/Tracking";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTrashAlt } from "@fortawesome/free-solid-svg-icons";
import { Project, UserProjects as P } from "../../datatypes/types";
import { ReactElement, useEffect, useState } from "react";
import { getActive, getMonthMins, getProjects, startTracking, stopTracking, userDelProject } from "../../Api";

import { Add } from "../common/add/Add";
import { AddProject } from "../common/addProject/AddProject";
import { Tracked } from "../../datatypes/final";
import './projects.scss';
import { MinToStringWithoutSeconds, MinToStringWithSeconds } from "../../Func";

interface UserData {
  projects: P | null;
  tracking: Tracked | null;
  monthly: number;
}

export const UserProjects = (): ReactElement => {

    const [projects, setProjects] = useState<P | null>(null)
    const [dialog, setDialog] = useState<boolean>(false);
    const [tracking, setTracking] = useState<Tracked | null>(null);
    const [totalMonth, setTotalMonth] = useState<number>(0);
  
    const fetchAndSetData = async (): Promise<UserData> => {
        const projects = await getProjects();
        const oneActive = await getActive();
        const totalMonth = await getMonthMins();
        setProjects(projects);
        setTracking(oneActive);
        setTotalMonth(totalMonth);
        return {projects: projects, tracking: oneActive, monthly: totalMonth};
    }

    useEffect(() => {
      console.log("fetching data, useEffect");
      fetchAndSetData();
    }, [])
  
    const closeDialog = async (): Promise<void> => {
        setDialog(false);
        const res = await fetchAndSetData();
        console.log("fetching data, closeDialog");
      if (res !== null && res.tracking !== null) {
        setTracking(res.tracking);
      }
    }
  
    const beginTracking = async (id: number): Promise<void> => {
      const res = await startTracking({ projectID: id, timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone })
      if (res) {
        const t = await getActive();
        console.log("timzone: " + Intl.DateTimeFormat().resolvedOptions().timeZone);
        setTracking(t);
      } else {
        setTracking(null);
      }
    }
  
    const endTracking = async (): Promise<void> => {
      if (tracking) {
        const res = await stopTracking(tracking.id);
        if (res === true) {
          setTracking(null);
        }
        await fetchAndSetData();
        console.log("fetching data, endTracking");
      }
    }

    const deleteProject = async (e: React.MouseEvent, id: number): Promise<void> => {
      e.stopPropagation();
      if (confirm("Do you realy want to delete the Project!\nAll tracked data will be deleted!")) {
        await userDelProject(id);
        await fetchAndSetData();
        console.log("fetching data, DeleteProject");
      }
    }
  
    const renderProject = (p: Project): ReactElement => {
      return <div className='project'
        key={p.id}
        onClick={() => beginTracking(p.id)}
        style={{ backgroundColor: p.color, backgroundImage: "linear-gradient(to right,"+p.color+", white 20%)" }}>
        <span className='title'>{p.title}</span>
        <div className="desc">
          <div/>
          <label className='description'>{p.description} </label>
          <label className='tracked'>{"tracked: " + MinToStringWithSeconds(p.trackedThisMonth)} </label>
          <div className="delete"><FontAwesomeIcon icon={faTrashAlt} onClick={(e) => deleteProject(e, p.id)}/></div>
        </div>
      </div>
    }

    const renderGroupProjects = (): ReactElement | undefined => {
      if (projects && projects.group.length > 0) {
        return <Fragment>
          <h2>Group Projects</h2>
          <div className='projects-list'>
            {projects?.group.map(p => renderProject(p))}
            <div className='total-month'>
                <span>{"This Month: " + MinToStringWithoutSeconds(totalMonth)}</span>
            </div>
        </div>
        </Fragment>
      }
    }
  
    const renderMainView = (): ReactElement => {
      return <Fragment>
        <h2>Personal Projects</h2>
        <div className='projects-list'>
            {projects?.own.map(p => renderProject(p))}
            <div className='total-month'>
                <span>{"This Month: " + MinToStringWithoutSeconds(totalMonth)}</span>
            </div>
        </div>
        {renderGroupProjects()}
        <Add onClick={() => setDialog(true)}/>
      </Fragment>
    }
  
    const getTitleForActive = (): string => {
      if (tracking !== null){
        const search = projects?.group.concat(projects.own);
        const found = search?.find(p => p.id === tracking.projectId);
        if (found) {
          return found.title;
        }
      }
      return "Unkown Project";
    }
  
  
    const renderTracking = (): ReactElement => {
      return <Tracking
        onStop={endTracking}
        title={getTitleForActive()}
        tracking={tracking}
      />
    }
  
    const renderMain = (): ReactElement => {
      if (dialog === true) {
        return <AddProject close={closeDialog} />
      } else {
        if (tracking && tracking !== null) {
          return renderTracking();
        } else {
          return renderMainView()
        }
      }
    }
  
    return (
      <div className="App">
        {renderMain()}
      </div>
    );


}
