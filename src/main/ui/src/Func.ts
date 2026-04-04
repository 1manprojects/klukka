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
import { toast } from 'react-toastify';


// eslint-disable-next-line @typescript-eslint/explicit-function-return-type
export const showToast = (res: boolean, okMessage: string, failedMessage: string) => {
  if (res) {
    toast.success(okMessage);
  } else {
    toast.error(failedMessage);
  }
};

// eslint-disable-next-line @typescript-eslint/explicit-function-return-type
export const showToastMessage = (message: string) => {
    toast.error(message);
};

export const UtcNow = (): Date => {
    const now = new Date();
    return new Date(now.getTime() + now.getTimezoneOffset() * 60000);
}

export const MinToStringWithoutSeconds = (minutes: number): string => {
    const zeroPad = (num: number, places: number): string => String(num).padStart(places, '0')

    const h = Math.floor(minutes / 60);
    const m = Math.floor(minutes - (h * 60))
    return zeroPad(h, 2) + "h " + zeroPad(m, 2) + "m";
}

export const MinToStringWithSeconds = (minutes: number): string => {
    const zeroPad = (num: number, places: number): string => String(num).padStart(places, '0')

    const h = Math.floor(minutes / 60);
    const m = Math.floor(minutes - (h * 60))
    const s = Math.floor((minutes - (h * 60) - m) * 60);
    return zeroPad(h, 2) + "h " + zeroPad(m, 2) + "m " + s + "s";
}

export const MinToHours = (minutes: number): number => {
    return Math.floor(minutes / 60);
}

export const getStartOfWeek2 = (): string => {
    const d = new Date();
    const dayOfWeek = d.getDay();
    const diff = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    const firstDayOfWeek = new Date(d);
    firstDayOfWeek.setDate(d.getDate() + diff);
    firstDayOfWeek.setUTCHours(0, 0, 0, 0);
    return firstDayOfWeek.toISOString();
}

export const getStartOfWeek = (date: Date): string => {
    const day = date.getDay();
    let diff = 0;
    if (day === 0) {
        diff = -6;
    } else if (day > 1 ) {
        diff =  (-1 * day) +1;
    }
    const monday = new Date(date);
    monday.setDate(date.getDate() + diff);
    monday.setUTCHours(0, 0, 0, 0);
    return monday.toISOString();
};

export const getEndOfWeek = (date: Date): string => {
    const dayOfWeek = date.getDay();
    const diff = dayOfWeek === 0 ? 1 : 8 - dayOfWeek; 
    const sunday = new Date(date);
    sunday.setDate(date.getDate() + diff)
    sunday.setUTCHours(23,59,59,0)
    return sunday.toISOString();
}

export const getDayName = (date: Date, locale: string = 'en-US'): string => {
    return date.toLocaleDateString(locale, { weekday: 'long' });
}

export const getFomratedDateSting = (date: Date): string => {
    return date.toISOString().substring(0, 10);
}

export const getUtc = (date: Date): Date => {
    return new Date(Date.UTC(
        date.getFullYear(),
        date.getMonth(),
        date.getDate()
    ));
}

export const getStartOfDay = (date: Date): Date => {
    const d = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
    d.setUTCHours(0,0,0,0);
    return d;
}

export const getEndOfDay = (date: Date): Date => {
    const endOfDay = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate(), 23, 59, 59, 999));
    return endOfDay;
}

export const getStartOfMonth = (date: Date): Date => {
    const d = new Date(date);
    d.setDate(1);
    d.setUTCHours(0,0,0,0);
    return d;
}

export const getEndOfMonth = (date: Date): Date => {
    const d = new Date(date);
    d.setMonth(d.getMonth() + 1);
    d.setDate(0);
    d.setUTCHours(23,59,59,0);
    return d;
}

export const getShortDate = (date: Date): string => {
    return date.toISOString().substring(0, 10);
}

export const convertUtcToLocalTime = (utcTime: string | null, timeZone: string | null): string =>{
    if (utcTime && timeZone) {
        const date = new Date(`${utcTime} UTC`);
        const options: Intl.DateTimeFormatOptions = {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false,
            timeZone
        };

        return new Intl.DateTimeFormat('en-GB', options).format(date);
    }
    return "-:-"
}

export const convertUtcToLocalDate = (utcTime: string | null, timeZone: string | null): string =>{
    if (utcTime && timeZone) {
        const date = new Date(`${utcTime} UTC`);
        const options: Intl.DateTimeFormatOptions = {
            day: '2-digit',
            month: 'long',
            year: 'numeric',
            timeZone
        };

        return new Intl.DateTimeFormat('en-GB', options).format(date);
    }
    return "-.-.-"
}

export const convertUtcToLocal = (utcTime: string, timeZone: string) : Date => {
    const date = new Date(`${utcTime} UTC`);
    const options: Intl.DateTimeFormatOptions = {
        timeZone,
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false,
    };
    const formatter = new Intl.DateTimeFormat('en-GB', options);
    const formattedDateString = formatter.format(date);
    return new Date(formattedDateString);
}

export const getDurationAsString = (startDate: Date, endDate: Date): string => {
    const totalSeconds = Math.floor((endDate.getTime() - startDate.getTime()) / 1000);

    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    const zeroPad = (num: number): string => String(num).padStart(2, '0');

    return `${zeroPad(hours)}h : ${zeroPad(minutes)}m : ${zeroPad(seconds)}s`;
};
