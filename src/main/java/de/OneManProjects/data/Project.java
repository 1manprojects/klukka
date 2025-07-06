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

import de.OneManProjects.data.enums.RefType;

public class Project {
    private final int id;
    private final int ref;
    private final RefType refType;
    private final String title;
    private final String description;
    private final String color;
    private final boolean archived;
    private final double trackedThisMonth;

    public Project(final int id, final int ref, final String refType, final String title, final String description, final String color, final boolean archived) {
        this.id = id;
        this.ref = ref;
        this.refType = RefType.valueOf(refType);
        this.title = title;
        this.description = description;
        this.color = color;
        this.trackedThisMonth = 0;
        this.archived = archived;
    }

    public Project(final int id, final int ref, final String refType, final String title, final String description, final String color, final boolean archived, final double trackedThisMonth) {
        this.id = id;
        this.ref = ref;
        this.refType = RefType.valueOf(refType);
        this.title = title;
        this.description = description;
        this.color = color;
        this.trackedThisMonth = trackedThisMonth;
        this.archived = archived;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public double getTrackedThisMonth() {
        return trackedThisMonth;
    }

    public int getRef() {
        return ref;
    }

    public RefType getRefType() {
        return refType;
    }

    public boolean isArchived() {
        return archived;
    }
}
