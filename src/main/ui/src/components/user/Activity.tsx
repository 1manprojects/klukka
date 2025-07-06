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
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import './activity.scss'
import { AnalysisData, Tracked } from "../../datatypes/final";
import { getDayName, getEndOfDay, getEndOfMonth, getEndOfWeek, getShortDate, getStartOfDay, getStartOfMonth, getStartOfWeek, getUtc, MinToStringWithoutSeconds } from "../../Func";
import { downloadExport, getAnalysisData, getGroupAnalysisData } from "../../Api";
import { DataFilter, Project } from "../../datatypes/types";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowLeft, faArrowRight } from "@fortawesome/free-solid-svg-icons";

interface DateFilter {
    start: string,
    end: string
}

interface TrackingData {
    name: string,
    [key: string]: number | string
}

type Presets = "Today" | "Week" | "Month" | "Custom"

export const daysOfWeek = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
export const getDaysInMonth = (year: number, month: number): number[] => {
    const date = new Date(year, month, 1);
    const days = [];
    while (date.getMonth() === month) {
        days.push(date.getDate());
        date.setDate(date.getDate() + 1);
    }
    return days;
}

const getWeekDayName = (year: number, month: number, day: number): string => {
    const date = new Date(year, month, day);
    const userLocale = navigator.language;
    return day + ". "+ date.toLocaleDateString(userLocale, { month: 'long' });
}

export const buildMonthlyData = (weeklyActivity: Tracked[]): TrackingData[] => {
    const montlyData: { [key: string]: TrackingData } = {};
    const date = new Date();
    getDaysInMonth(date.getFullYear(), date.getMonth()).forEach(day => {
        const dayName = getWeekDayName(date.getFullYear(), date.getMonth(), day);
        montlyData[day] = { name: dayName };
    });

    weeklyActivity.forEach(activity => {
        const dayNumber = new Date(activity.start).getDate();
        const projectName = `Project${activity.projectId}`;
        const duration = (new Date(activity.end).getTime() - new Date(activity.start).getTime()) / (1000 * 60);

        if (dayNumber)

        if (!montlyData[dayNumber][projectName]) {
            montlyData[dayNumber][projectName] = 0;
        }
        montlyData[dayNumber][projectName] = +montlyData[dayNumber][projectName] + duration;
    });

    return Object.values(montlyData);
}

export const buildWeeklyData = (weeklyActivity: Tracked[]): TrackingData[] => {
    const dailyData: { [key: string]: TrackingData } = {};

    daysOfWeek.forEach(day => {
        dailyData[day] = { name: day };
    });

    weeklyActivity.forEach(activity => {
        const dayName = getDayName(new Date(activity.start));
        const projectName = `Project${activity.projectId}`;
        const duration = (new Date(activity.end).getTime() - new Date(activity.start).getTime()) / (1000 * 60);

        if (!dailyData[dayName][projectName]) {
            dailyData[dayName][projectName] = 0;
        }
        dailyData[dayName][projectName] = +dailyData[dayName][projectName] + duration;
    });

    return Object.values(dailyData);
}

export const buildDailyData = (weeklyActivity: Tracked[]): TrackingData[] => {
    const dailyData: { [key: string]: TrackingData } = {};
    const dayName = getDayName(new Date());
    dailyData[dayName] = { name: dayName };

    weeklyActivity.forEach(activity => {
        const projectName = `Project${activity.projectId}`;
        const duration = (new Date(activity.end).getTime() - new Date(activity.start).getTime()) / (1000 * 60); // Duration in minutes

        if (!dailyData[dayName][projectName]) {
            dailyData[dayName][projectName] = 0;
        }
        dailyData[dayName][projectName] = +dailyData[dayName][projectName] + duration;
    });

    return Object.values(dailyData);
}

export const buildCustomData = (weeklyActivity: Tracked[], start: string, end: string): TrackingData[] => {
    const customData: { [key: string]: TrackingData } = {};

    const currentDate = new Date(start);
    const endDate = new Date(end);
    while (currentDate <= endDate) {
        customData[getShortDate(currentDate)] = { name: getShortDate(currentDate) };
        currentDate.setDate(currentDate.getDate() + 1);
    }

    weeklyActivity.forEach(activity => {
        const dateString = getShortDate(new Date(activity.start));
        if (customData[dateString] !== undefined) {

            const projectName = `Project${activity.projectId}`;
            const duration = (new Date(activity.end).getTime() - new Date(activity.start).getTime()) / (1000 * 60); // Duration in minutes


            if (!customData[dateString][projectName]) {
                customData[dateString][projectName] = 0;
            }
            customData[dateString][projectName] = +customData[dateString][projectName] + duration;
        }
    });

    return Object.values(customData);
}

export interface ActivityProps {
    groupId?: number;
}

export const Activity = (props: ActivityProps) : ReactElement => {

    const [activity, setActivity] = useState<Tracked[]>([])
    const [projects, setProjects] = useState<Project[]>([])
    const [customDate, setCustomDate] = useState<{start: Date, end: Date}>({start: (new Date()), end: getEndOfDay(new Date())})
    const [filter, setFilter] = useState<DateFilter>({start: getStartOfDay(new Date()).toISOString(), end: getEndOfDay(new Date()).toISOString()})
    const [selectedPreset, setSelectedPreset] = useState<Presets>("Today");
    const [detailed, setDetailed] = useState<boolean>(false);

    const getDataByPreset = (): TrackingData[] => {
        switch (selectedPreset) {
            case "Today":
                return buildDailyData(activity);
            case "Week":
                return buildWeeklyData(activity);
            case "Month":
                return buildMonthlyData(activity);
            case "Custom":
                return buildCustomData(activity, filter.start, filter.end);
        }
    }

    const fetchAndSetData = async (filter: DataFilter): Promise<AnalysisData| null> => {
        if (props.groupId !== undefined) {
            const data = await getGroupAnalysisData({...filter, groupId: props.groupId});
            if (data !== null) {
                setProjects(data.groupProjects);
                setActivity(data.tracked);
            }
            return data;
        } else {
            const data = await getAnalysisData(filter);
            if (data !== null) {
                setProjects(data.projects.concat(data.groupProjects));
                setActivity(data.tracked);
            }
            return data;
        }
    }

    useEffect(() => {
        fetchAndSetData(filter)
    },[filter])

    const getProjectName = (id: number): string => {
        const project = projects.find(p => p.id === id);
        if (project) {
            return project.title;
        }
        return "Unknown";
    }

    const changeFilter = (preset: Presets, startDate?: string, endDate?: string): void => {
        switch (preset) {
            case "Today":
                setSelectedPreset("Today");
                setFilter({start: getStartOfDay(new Date()).toISOString(), end: getEndOfDay(new Date()).toISOString()})
                break;
            case "Week":
                setSelectedPreset("Week")
                setFilter({start: getStartOfWeek(new Date()), end: getEndOfWeek(new Date())})
                break;
            case "Month":
                setSelectedPreset("Month")
                setFilter({start: getStartOfMonth(new Date()).toISOString(), end: getEndOfMonth(new Date()).toISOString()})
                break;
            case "Custom":
                if (startDate && endDate) {
                    setSelectedPreset("Custom")
                    setFilter({start: startDate, end: endDate})
                } else {
                    //Default
                    setSelectedPreset("Today");
                    setFilter({start: getStartOfDay(new Date()).toISOString(), end: getEndOfDay(new Date()).toISOString()})
                }
                break;
        }
    }

    const renderBar = (): ReactElement[] => {
        const bars: ReactElement[] = [];
        projects.forEach(project => {
            bars.push(<Bar 
                key={project.id}
                dataKey={"Project"+ project.id}
                stackId={"a"}
                fill={project.color}
                maxBarSize={50}
                >
                
            </Bar>)
        });
        return bars;
    }

    const formatToolTip = (value: string, name: string): string[] => {
        return [MinToStringWithoutSeconds(+value), getProjectName(+name.substring(7))];
    }

    const formatLegend = (name: string): string => {
        return getProjectName(+name.substring(7));
    }

    const getMaxMinutes = (): number => {
        const totals: Record<string, number> = {};
        for (const entry of getDataByPreset()) {
            for (const key of Object.keys(entry)) {
                if (key === "name") continue;
                const value = entry[key];
                if (typeof value === "number") {
                totals[entry.name] = (totals[entry.name] || 0) + value;
                }
            }
        }
        const maxTotal = Math.ceil( Math.max(...Object.values(totals)) / 60);
        return 60 * Math.max(maxTotal, 1);
    }

    const ticks = (): number[] => {
        const ticks: number[] = [];
        for (let i = 0; i <= getMaxMinutes(); i += 60) {
            ticks.push(i);
        }
        return ticks;
    }

    const renderBarChart = (): ReactElement => {
        return <ResponsiveContainer width="100%" height="100%">
            <BarChart
                barGap={50}
                width={500}
                height={500}
                data={getDataByPreset()}
                layout="vertical"
                margin={{
                    top: 20,
                    right: 20,
                    left: 40,
                    bottom: 5,
                }}
                >
                <CartesianGrid strokeDasharray="10 5 10" fill="gray" fillOpacity={0.1}/>

                <XAxis tick type="number"
                    domain={[0, getMaxMinutes()]}
                    ticks={ticks()}
                    tickFormatter={(value) => {
                    const hours = Math.floor(value / 60);
                    const minutes = value % 60;
                    return `${hours}:${minutes.toString().padStart(2, '0')}`;
                }}/>
                <YAxis dataKey="name" type="category" />

                <Tooltip contentStyle={{textAlign: "left"}} formatter={formatToolTip}/>
                <Legend align="left" formatter={formatLegend}/>

                {renderBar()}
            </BarChart>
      </ResponsiveContainer>
    }

    const renderTrackingData = (): ReactElement => {

        interface ProjectData {
            id: number,
            name: string,
            duration: number
        }

        const getProjectsDetails = () : ProjectData[] => {
            const projects: ProjectData[] = [];
            activity.forEach(a => {
                const project = projects.find(p => p.id === a.projectId);
                if (project) {
                    project.duration += (new Date(a.end).getTime() - new Date(a.start).getTime());
                } else {
                    projects.push({id: a.projectId, name: getProjectName(a.projectId), duration: (new Date(a.end).getTime() - new Date(a.start).getTime())})
                }
            });
            return projects.sort((a, b) => b.duration - a.duration);
        }

        const data = getProjectsDetails();

        return <div className="tracked-details">
            <h2>Details</h2>
            <div className="details">
                <span className="label">Overall time tracked:</span>
                <span className="info">{MinToStringWithoutSeconds(data.reduce((acc, a) => acc + a.duration, 0) / (1000 * 60))}</span>
                <span className="label">Nr of Projects worked on:</span>
                <span className="info">{data.length}</span>
                <span className="label bold">Project details:</span>
                <span className="info"></span>
                {data.map(a => {
                    return <Fragment key={a.id}>
                        <span className="label">{a.name}</span>
                        <span className="info">{MinToStringWithoutSeconds( a.duration / (1000 * 60))}</span>
                    </Fragment>
                })}
            </div>
        </div>
    }

    const getDate = (date: Date): string => {
        if ( !isNaN(date.getTime())) {
            const res = date.toISOString().split('T');
            if (res && res.length > 0) {
                return res[0];
            }
        }
        return "";
    }

    const navigatBack = (): void => {
        const startString = filter.start? filter.start : getStartOfDay(new Date()).toISOString();
        const endString = filter.end? filter.end : getStartOfDay(new Date()).toISOString();
        const start = new Date(startString);
        start.setDate(start.getDate() - 1);
        const end = new Date(endString);
        end.setDate(end.getDate() - 1);
        changeFilter("Custom", start.toISOString(), end.toISOString());
    }

    const NavigateNext = (): void => {
                const startString = filter.start? filter.start : getStartOfDay(new Date()).toISOString();
        const endString = filter.end? filter.end : getStartOfDay(new Date()).toISOString();
        const start = new Date(startString);
        start.setDate(start.getDate() + 1);
        const end = new Date(endString);
        end.setDate(end.getDate() + 1);
        changeFilter("Custom", start.toISOString(), end.toISOString());
    }

    return <div className="activity">
        <div className="filter">
            <div className="presets">
                <button className="b1" onClick={navigatBack}><FontAwesomeIcon icon={faArrowLeft}/> Back</button>
                <button className={selectedPreset === "Today"? "b2 active": "b2"} onClick={()=>changeFilter("Today")}>Today</button>
                <button className={selectedPreset === "Week"? "b3 active": "b3"} onClick={()=>changeFilter("Week")}>This Week</button>
                <button className={selectedPreset === "Month"? "b4 active": "b4"} onClick={()=>changeFilter("Month")}>This month</button>
                <button className="b5" onClick={NavigateNext}>Next <FontAwesomeIcon icon={faArrowRight}/></button>
            </div>
            <div className="edit">
                <div className="custom">
                    <h3>Custom</h3>
                    <label>From</label>
                    <input
                        type="date"
                        value={getDate(customDate.start)}
                        onChange={(e) => setCustomDate({ ...customDate, start: getUtc(new Date(e.target.value)) })}
                    />
                    <label>To</label>
                    <input
                        type="date"
                        value={getDate(customDate.end)}
                        onChange={(e) => setCustomDate({ ...customDate, end: getUtc(new Date(e.target.value)) })}
                    />
                    <button onClick={()=> {
                        const start = getStartOfDay(customDate.start).toISOString();
                        const end = getEndOfDay(customDate.end).toISOString();
                        changeFilter("Custom", start, end)
                        }}
                    >Apply</button>
                </div>
            </div>
        </div>
        <div className="tools">
        </div>
        <div className="graph">
            {renderBarChart()}
        </div>
        {renderTrackingData()}
        <div className="export">
            <h3>Export CSV</h3>
            <label>Detailed Export</label>
            <input type="checkbox" checked={detailed} onChange={(e) => setDetailed(e.target.checked)}/>
            <button onClick={() => downloadExport({filter: filter, detailed: detailed}, props.groupId)}>Export</button>
        </div>
    </div>
}
