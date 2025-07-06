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
import { ReactElement, useState } from "react"
import { Project } from "../../../datatypes/types";
import { TrackedEvent } from "../../user/Calendar";
import Select from 'react-select'
import './editEvent.scss'
import { getDurationAsString } from "../../../Func";

export interface EditEventProps {
    event: TrackedEvent;
    projects: Project[];
    onSave: (event: TrackedEvent) => void;
    onDelete: (event: TrackedEvent) => void;
    onCancel: () => void;
}

export const EditEvent = (props: EditEventProps): ReactElement => {

    const [event, setEvent] = useState<TrackedEvent>(props.event);

    const getProject = (id: number) : Project=> {
        const t: Project | undefined = props.projects.find(p => p.id === id);
        if (t !== undefined) {
            return t;
        }
        return {color: "black", id: -1, title: "No project", description:"missing", trackedThisMonth:0, ref: -1, refType: "USER",archived: false};
    }

    const formatDate = (date: Date): string => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const formatTime = (date: Date): string => {
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    };

    const handleDateStartChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
        const newDate = new Date(e.target.value);
        setEvent((prevEvent) => {
        const updatedStart = new Date(prevEvent.start);
        updatedStart.setFullYear(newDate.getFullYear(), newDate.getMonth(), newDate.getDate());
        return { ...prevEvent, start: updatedStart };
        });
    };

    const handleTimeStartChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
        const [hours, minutes] = e.target.value.split(':');
        setEvent((prevEvent) => {
        const updatedStart = new Date(prevEvent.start);
        updatedStart.setHours(parseInt(hours), parseInt(minutes), 0, 0);
        return { ...prevEvent, start: updatedStart };
        });
    };

    const handleDateEndChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
        const newDate = new Date(e.target.value);
        setEvent((prevEvent) => {
        const updatedStart = new Date(prevEvent.end);
        updatedStart.setFullYear(newDate.getFullYear(), newDate.getMonth(), newDate.getDate());
        return { ...prevEvent, end: updatedStart };
        });
    };

    const handleTimeEndChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
        const [hours, minutes] = e.target.value.split(':');
        setEvent((prevEvent) => {
        const updatedStart = new Date(prevEvent.end);
        updatedStart.setHours(parseInt(hours), parseInt(minutes), 0, 0);
        return { ...prevEvent, end: updatedStart };
        });
    };
    
    return <div className="editEvent">
        <h2>{event.title}</h2>
        <label>{"Tracked: " + getDurationAsString(event.start, event.end)}</label>
        <h3>Project</h3>
        <Select
            className="project-select"
            styles={{
                control: (provided) => ({
                ...provided,
                boxShadow: "none",
                border: "none",
                backgroundColor: event.color,
                backgroundImage: "linear-gradient(to right, " + event.color +", white 100%)",
                color: "#000000",
                width:"100%"
                }),
                option: (provided, state) => ({
                ...provided,
                backgroundImage: "linear-gradient(to right, " + state.data.value.color + ", white 100%)",
                })
            }}
            options={props.projects.map(p => { return { value: p, label: p.title } })}
            onChange={(e) => {
                if (e) {
                    setEvent({ ...event, color: e.value.color, projectId: e.value.id })}
                }
            }
            placeholder="Select project"
            value={{ value: getProject(event.projectId), label: getProject(event.projectId).title }}
        />
        <div className="dates">
            <label htmlFor="date">Start-Date:</label>
            <input
                type="date"
                id="date"
                value={formatDate(event.start)} // Display formatted date
                onChange={handleDateStartChange}
                style={{ width: '100%' }} // Full width for mobile
            />
            <br />
            <label htmlFor="time">Start-Time:</label>
            <input
                type="time"
                id="time"
                value={formatTime(event.start)} // Display formatted time
                onChange={handleTimeStartChange}
                style={{ width: '100%' }} // Full width for mobile
            />
         </div>
         <div className="dates">
            <label htmlFor="date">End-Date:</label>
            <input
                type="date"
                id="date"
                value={formatDate(event.end)} // Display formatted date
                onChange={handleDateEndChange}
                style={{ width: '100%' }} // Full width for mobile
            />
            <br />
            <label htmlFor="time">End-Time:</label>
            <input
                type="time"
                id="time"
                value={formatTime(event.end)} // Display formatted time
                onChange={handleTimeEndChange}
                style={{ width: '100%' }} // Full width for mobile
            />
         </div>
         <div className="buttons">
            <button className="delete" onClick={() => props.onDelete(event)}>Delete</button>
            <button 
                className="ok"
                onClick={() => props.onSave(event)}  
                disabled={getProject(event.projectId).id === -1}
            >
                {props.event.id !== -1 ? "Update" : "Save"}
            </button>
        </div>
    </div>
}
