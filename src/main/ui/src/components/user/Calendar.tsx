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
import { DataFilter, Project } from "../../datatypes/types";
import { getEndOfWeek, getStartOfWeek, showToast } from "../../Func";
import { deleteTracking, getAnalysisData, updateTracking } from "../../Api";
import { Calendar, DateRange, EventWrapperProps, momentLocalizer, View } from 'react-big-calendar'
import moment from 'moment'
import 'moment-timezone';
import 'react-big-calendar/lib/css/react-big-calendar.css'
import './calendar.scss'
import { AnalysisData, Tracked } from "../../datatypes/final";
import { Modal } from "../common/modal/Modal";
import { EditEvent } from "../common/editEvent/EditEvent";
import { useScreenWidth } from "../hooks/useScreenWidth";

export interface TrackedEvent {
    id: number;
    projectId: number;
    title: string;
    start: Date;
    end: Date;
    color: string;
}

export const CalendarView = () : ReactElement => {

    const [dialog, setDialog] = useState<TrackedEvent| null>(null);
    const [projects, setProjects] = useState<Project[]>([]);
    const [tracking, setTracking] = useState<Tracked[]>([]);
    const [start, setStart] = useState<string>(getStartOfWeek(new Date()));
    const [end, setEnd] = useState<string>(getEndOfWeek(new Date()))
    const [view , setView] = useState<View>("week");

    const screen = useScreenWidth();

    const localizer = momentLocalizer(moment)
    moment.locale("de-DE", {
        week: {
            dow: 1 //Monday is the first day of the week.
        }
    });

    useEffect(() => {
        if (screen.width < 800) {
            setView("day");
        } else {
            setView("week");
        }
      }, [screen]);


    const deleteItem = async (event: TrackedEvent): Promise<void> => {
        if (window.confirm("Are you sure you want to delete this event?")) {
            const res = await deleteTracking(event.id);
            showToast(res, "Event deleted", "Error deleting event");
            fetchAndSetData({start:start, end: end});
        }
        setDialog(null)
    }

    const saveOrUpdateEvent = async (event: TrackedEvent): Promise<void> => {
        const track: Tracked = {active: false,
            end: event.end.toISOString(),
            id: event.id,
            projectId: event.projectId,
            start: event.start.toISOString(),
            user: 0,
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
        };
        const res = await updateTracking(track);
        showToast(res, "Event updated", "Error updating event");
        fetchAndSetData({start:start, end: end});
        setDialog(null);
    }

    const fetchAndSetData = async (filter: DataFilter): Promise<AnalysisData| null> => {
        const data = await getAnalysisData(filter);
        if (data !== null) {
            setProjects(data.projects.concat(data.groupProjects));
            setTracking(data.tracked);
        }
        return data;
    }

    const getProjectTitle = (pId: number): string => {
        const res = projects.find(p => p.id === pId)?.title;
        if (res) {
            return res;
        }
        return "error";
    }

    const getProjectColor = (pId: number): string => {
        const res = projects.find(p => p.id === pId)?.color;
        if (res) {
            return res;
        }
        return "red";
    }

    const parseDate = (val: string, tz: string) : Date => {
        return moment.tz(val, tz).toDate();
    }

    const getEvents = () : TrackedEvent[] => {
        const res = tracking.map(t => {
            return {
                id: t.id,
                title: getProjectTitle(t.projectId),
                start: parseDate(t.start, t.timezone),
                end: parseDate(t.end, t.timezone),
                projectId: t.projectId,
                color: getProjectColor(t.projectId)
            }
        });
        res.sort((a, b) => a.start.getTime() - b.start.getTime());
        return res;
    }
/*
    const getTimeOfDayMs = (dateStr): number => {
        const d = new Date(dateStr);
        return d.getHours() * 3600000 + d.getMinutes() * 60000 + d.getSeconds() * 1000 + d.getMilliseconds();
    };

    const getEarliestStart = (): Date => {
        if (tracking.length === 0) {
            return new Date(new Date().getFullYear(), new Date().getMonth(), new Date().getDate(), 23, 0);
        }
        const start = tracking.reduce((latest, curr) => {
            return getTimeOfDayMs(curr.start) < getTimeOfDayMs(latest.start) ? curr : latest;
        }, tracking[0]);

        const offsetMinutes = new Date().getTimezoneOffset();
        const startDate = new Date(new Date(start.start).getTime() - offsetMinutes * 60000);
        return new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate(), startDate.getHours() , 0);
    }

    const getLatestEnd = (): Date => {
        if (tracking.length === 0) {
            return new Date(new Date().getFullYear(), new Date().getMonth(), new Date().getDate(), 23, 0);
        }
        const end = tracking.reduce((latest, curr) => {
            return getTimeOfDayMs(curr.end) > getTimeOfDayMs(latest.end) ? curr : latest;
        }, tracking[0]);

        const offsetMinutes = new Date().getTimezoneOffset();
        const endDate = new Date(new Date(end.end).getTime() - offsetMinutes * 60000);
        return new Date(endDate.getFullYear(), endDate.getMonth(), endDate.getDate(), endDate.getHours() +1 , 0);
    }
*/
    useEffect(()=>{
        fetchAndSetData({start:start, end: end});
    },[])

    const trackingFormat = (dr: DateRange): string => {
        return localizer.format(dr.start, "HH:mm") + " - " + localizer.format(dr.end, "HH:mm")
    }

    const ownEvent = (e: EventWrapperProps<TrackedEvent>): ReactElement => {
        const newStyle = {
            ...e.style,
            top: e.style?.top + "%",
            left: e.style?.left + "%",
            backgroundColor: e.event.color,
            width: e.style?.width === "calc(100% - 0px)"? "100%" : e.style?.width,
            color: "black",
            backgroundImage: "linear-gradient(to right,"+e.event.color+", white 100%)"
        }
        return<div className="rbc-event" style={newStyle}
            onClick={() => handleSelectEvent(e.event)}
            >
                <div className="rbc-event-label">
                    {trackingFormat({start: e.event.start, end: e.event.end})}
                </div>
                <div className="rbc-event-content">
                    {e.event.title}
                </div>
            </div>
    }

    const handleSelectEvent = (e: TrackedEvent): void => {
        setDialog(e);
    }

    const handleSelectSlot = (e: {start: Date, end:Date}): void => {
        setDialog({id: -1, title: "New Event", start: e.start, end: e.end, color: "white", projectId: 0});
    }

    return <div className="calendar">
        <div className="cal-view">
            <Calendar
                components={{eventWrapper: ownEvent}}
                localizer={localizer}
                dayLayoutAlgorithm={"no-overlap"}
                events={getEvents()}
                startAccessor={"start"}
                endAccessor={"end"}
                defaultDate={new Date()}
                onSelectEvent={handleSelectEvent}
                onSelectSlot={handleSelectSlot}
                selectable
                view={view}
                defaultView={"week"}
                step={15}
                //min={getEarliestStart()}
                //max={getLatestEnd()}
                views={["day", "work_week","week"]}
                onView={(v) => setView(v)}
                onNavigate={(date) => {
                    const start = getStartOfWeek(date);
                    const end = getEndOfWeek(date);
                    setStart(start);
                    setEnd(end);
                    fetchAndSetData({start:start, end: end});
                }}
                formats={{timeGutterFormat: "HH:mm", eventTimeRangeFormat: trackingFormat}}
                style={{ height: 800 }}
            />
        </div>
        <Modal show={dialog !== null} onClose={() => setDialog(null)}>
                {dialog && (
                    <EditEvent event={dialog} projects={projects} onCancel={() => setDialog(null)} onSave={(e)=>saveOrUpdateEvent(e)} onDelete={(e) => deleteItem(e)} />
                )}
            </Modal>
    </div>
}
