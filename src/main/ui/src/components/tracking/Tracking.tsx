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
import { Fragment, ReactElement, useEffect, useState, useRef } from "react";

import { CircularProgressbarWithChildren } from 'react-circular-progressbar';
import 'react-circular-progressbar/dist/styles.css';

import './tracking.scss'
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleStop } from "@fortawesome/free-solid-svg-icons";
import { Tracked } from "../../datatypes/final";
import { convertUtcToLocalDate, convertUtcToLocalTime, showToast } from "../../Func";
import { getActive, updateComment } from "../../Api";

export interface TrackingProps {
    tracking: Tracked | null;
    title: string;
    onStop: () => Promise<void>;
}

export const Tracking = (props: TrackingProps): ReactElement => {

    const getStartSeconds = (): number => {
        if (props.tracking) {
            const t1 = new Date(props.tracking.start);
            const t2 = new Date();
            const dif: number = Math.floor(Math.abs(t2.getTime() - t1.getTime()) / 1000);
            const hours = Math.floor((dif / (60 * 60)));
            const minutes = Math.floor(((dif / 60) - (hours * 60)));
            return dif - (minutes * 60) - (hours * 60 * 60)
        }
        return 0;
    }

    const [duration, setDuration] = useState<string>("00:00");
    const [seconds, setSeconds] = useState<number>(getStartSeconds());
    const [isHover, setIsHover] = useState(false);
    const [newComment, setNewComment] = useState<string>(props.tracking? props.tracking.comment : "");

    const handleMouseEnter = (): void => {
        setIsHover(true);
     };
  
     const handleMouseLeave = (): void => {
        setIsHover(false);
     };

    useEffect(() => {
        const tick = setInterval(() => {
            setSeconds(seconds + 1);
            if (seconds >= 60) {
                setSeconds(0);
            }
        }, 1000);
        return (): void => clearInterval(tick);
    }, [seconds])

    useEffect(() => {
        window.scrollTo({ top: 0, behavior: "smooth" });
        const update = setInterval(() => {
            if (props && props.tracking !== null) {
                const t1 = new Date(props.tracking.start);
                const t2 = new Date().getTime();

                const timezoneOffset = t1.getTimezoneOffset() * 60;
                const dif: number = Math.floor(Math.abs(t2 - t1.getTime()) / 1000) + timezoneOffset;

                const hours = Math.floor((dif / (60 * 60)));
                const minutes = Math.floor(((dif / 60) - (hours * 60)));
                setDuration(hours + "H " + minutes + "m");
            }
        }, 500);

        return (): void =>  {
            clearInterval(update);
        };
    }, [])

    useEffect(() => {
        const update = setInterval(async () => {
            const res: Tracked | null = await getActive();
            if (res === null) {
                location.reload();
            } else if ( res.id !== props.tracking?.id) {
                location.reload();
            }
        }, 5000);

        return (): void =>  {
            clearInterval(update);
        };
    }, [])

    const stop = async (): Promise<void> => {
        if (props.tracking && saveTimeout.current) {
            window.clearTimeout(saveTimeout.current);
            saveTimeout.current = undefined;
            await setComment(props.tracking.id, newComment);
        }
        await props.onStop();
    }

    const getStartTime = (): ReactElement => {
        return <Fragment>
            <span className="time">{props.tracking? convertUtcToLocalTime(props.tracking.start, props.tracking.timezone) : "-"}</span>
            <span className="date">{props.tracking? convertUtcToLocalDate(props.tracking.start, props.tracking.timezone): "-"}</span>
        </Fragment>
    }

    const setComment = async(id: number, comment: string): Promise<void> => {
        const res = await updateComment(id, comment);
        showToast(res, "Comment saved", "Error updating comment");
    }

    const saveTimeout = useRef<number | undefined>(undefined);

    useEffect(() => {
        if (!props.tracking) return;
        if (saveTimeout.current) {
            window.clearTimeout(saveTimeout.current);
        }
        saveTimeout.current = window.setTimeout(() => {
            setComment(props.tracking!.id, newComment);
        }, 2000);

        return (): void => {
            if (saveTimeout.current) {
                window.clearTimeout(saveTimeout.current);
                saveTimeout.current = undefined;
            }
        };
    }, [newComment, props.tracking?.id]);


    return <div className="tracked">
        <span className="title">{props.title}</span>
        <CircularProgressbarWithChildren className="progress-Bar" value={seconds} maxValue={59} styles={{path: { stroke: isHover? '#753333' : '#4DA4E6' }, trail: { stroke: '#d6d6d6' } }}>
            <div className={"progress-center"}
                onClick={() => stop()}
                onMouseEnter={handleMouseEnter}
                onMouseLeave={handleMouseLeave}
            >
                <span>started</span>
                {getStartTime()}
                <span className="duration">{duration}</span>
                    <FontAwesomeIcon className={"stop-icon" + (isHover? " stop": "")} icon={faCircleStop} />
            </div>
        </CircularProgressbarWithChildren >
        <div className="comment">
            <textarea onChange={(e) => setNewComment(e.target.value)} value={newComment} placeholder="Details about your work..." />
        </div>
    </div>
}
