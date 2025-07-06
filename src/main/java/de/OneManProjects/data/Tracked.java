package de.OneManProjects.data;

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
import java.sql.Timestamp;

public class Tracked {
    private final int id;
    private int user;
    private final int projectId;
    private final Timestamp start;
    private final Timestamp end;
    private final String timezone;
    private final boolean isActive;

    public Tracked(int id, int user, int projectId, Timestamp start, Timestamp end, String timezone) {
        this.id = id;
        this.user = user;
        this.projectId = projectId;
        this.start = start;
        this.end = end;
        this.isActive = false;
        this.timezone = timezone;
    }

    public Tracked(int id, int user, int projectId, Timestamp start, Timestamp end, String timezone, boolean active) {
        this.id = id;
        this.user = user;
        this.projectId = projectId;
        this.start = start;
        this.end = end;
        this.isActive = active;
        this.timezone = timezone;
    }

    public Tracked(int id, int user, int projectId, Timestamp start, String timezone) {
        this.id = id;
        this.user = user;
        this.projectId = projectId;
        this.start = start;
        this.end = null;
        this.isActive = true;
        this.timezone = timezone;
    }

    public int getUser() {
        return user;
    }

    public int getId() {
        return id;
    }

    public int getProjectId() {
        return projectId;
    }

    public Timestamp getStart() {
        return start;
    }

    public Timestamp getEnd() {
        return end;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getTimezone() {
        return timezone;
    }

    public void overrideUser(int user) {
        this.user = user;
    }
}
