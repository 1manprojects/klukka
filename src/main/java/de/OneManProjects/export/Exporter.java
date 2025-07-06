package de.OneManProjects.export;

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

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import de.OneManProjects.Database;
import de.OneManProjects.data.Project;
import de.OneManProjects.data.Tracked;
import de.OneManProjects.data.dto.ExportFilter;

public class Exporter {

    private static final String HEADERS_DETAILED = String.join(";", "Project", "Start", "End", "Duration hh::mm", "Description");
    private static final String HEADERS_GROUP_DETAILED = String.join(";", "Project", "Start", "End", "Duration hh::mm", "User", "Description");
    private static final String HEADERS = String.join(";", "Project", "Duration hh::mm", "Description");

    private static String formatDuration(final long duration) {
        final long hours = duration / 1000 / 60 / 60;
        final long minutes = (duration / 1000 / 60) % 60;
        return String.format("%d:%02d", hours, minutes);
    }

    private static String getDuration(final Tracked tracked) {
        if (tracked.getEnd() == null) {
            return "-";
        }
        final long duration = tracked.getEnd().getTime() - tracked.getStart().getTime();
        return formatDuration(duration);
    }

    private static String getProjectTitle(final Optional<Project> userProject, final Optional<Project> groupProject) {
        if (groupProject.isPresent()) {
            return groupProject.get().getTitle();
        } else {
            if (userProject.isPresent()) {
                return userProject.get().getTitle();
            }
        }
        return "Unknown";
    }

    private static String getDescription(final Optional<Project> userProject, final Optional<Project> groupProject) {
        if (groupProject.isPresent()) {
            return groupProject.get().getDescription();
        } else {
            if (userProject.isPresent()) {
                return userProject.get().getDescription();
            }
        }
        return "Unknown";
    }

    private static byte[] exportDetailedCsv(final List<Tracked> tracked, final List<Project> userProjects, final List<Project> groupProjects) {
        final List<String> rows = tracked.stream().map(trackedItem -> {
            final Optional<Project> p = userProjects.stream().filter(project -> project.getId() == trackedItem.getProjectId()).findFirst();
            final Optional<Project> g = groupProjects.stream().filter(project -> project.getId() == trackedItem.getProjectId()).findFirst();
            return String.format("%s,%s,%s,%s,%s",
                getProjectTitle(p, g),
                trackedItem.getStart().toString(),
                trackedItem.getEnd().toString(),
                getDuration(trackedItem),
                getDescription(p, g));
        }).toList();

        final StringBuilder csvContent = new StringBuilder();
        csvContent.append(HEADERS_DETAILED).append("\n");
        rows.forEach(row -> csvContent.append(row).append("\n"));
        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] exportDetailedCsvforGroup(final List<Tracked> tracked, final List<Project> groupProjects) {
        final List<String> rows = tracked.stream().map(trackedItem -> {
            final Optional<Project> g = groupProjects.stream().filter(project -> project.getId() == trackedItem.getProjectId()).findFirst();
            Optional<String> user;
            try {
                user = Database.getUserMail(trackedItem.getUser());
            } catch (final SQLException e) {
                user = Optional.empty();
            }
            return String.format("%s,%s,%s,%s,%s,%s",
                    g.isPresent()? g.get().getTitle() : "Unknown",
                    trackedItem.getStart().toString(),
                    trackedItem.getEnd().toString(),
                    getDuration(trackedItem),
                    user.orElse("Error Unkown"),
                    g.isPresent()? g.get().getDescription() : "Unknown");
        }).toList();

        final StringBuilder csvContent = new StringBuilder();
        csvContent.append(HEADERS_GROUP_DETAILED).append("\n");
        rows.forEach(row -> csvContent.append(row).append("\n"));
        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] exportCsv(final List<Tracked> tracked, final List<Project> userProjects, final List<Project> groupProjects) {
        final HashMap<Integer, Long> data = new HashMap<>();
        for (final Tracked track : tracked) {
            if (data.containsKey(track.getProjectId())) {
                data.compute(track.getProjectId(), (k, old) -> old + track.getEnd().getTime() - track.getStart().getTime());
            } else {
                data.put(track.getProjectId(), track.getEnd().getTime() - track.getStart().getTime());
            }
        }

        final List<String> rows = data.entrySet().stream().map(entry -> {
            final Optional<Project> p = userProjects.stream().filter(project -> project.getId() == entry.getKey()).findFirst();
            final Optional<Project> g = groupProjects.stream().filter(project -> project.getId() == entry.getKey()).findFirst();
            return String.format("%s,%s,%s",
                    getProjectTitle(p, g),
                    formatDuration(entry.getValue()),
                    p.isPresent() ? p.get().getDescription() : "Unknown");
        }).toList();

        final StringBuilder csvContent = new StringBuilder();
        csvContent.append(HEADERS).append("\n");
        rows.forEach(row -> csvContent.append(row).append("\n"));
        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] exportUserData(final ExportFilter filter, final int userId) throws SQLException {
        final List<Project> userProjects = Database.getProjects(userId, true);
        final List<Project> groupProjects = Database.getUserGroupProjects(userId);
        final List<Tracked> tracked = Database.getTrackedForRange(userId, Instant.parse(filter.filter().start()), Instant.parse(filter.filter().end()));
        if (filter.detailed()) {
            return exportDetailedCsv(tracked, userProjects, groupProjects);
        } else {
            return exportCsv(tracked, userProjects, groupProjects);
        }
    }

    public static byte[] exportGroupData(final ExportFilter filter, final int groupId) throws SQLException {
        final List<Project> groupProjects = Database.getGroupProjects(groupId, true);
        final List<Integer> groupProjectIds = groupProjects.stream().map(Project::getId).toList();
        final List<Tracked> tracked = Database.getGroupTrackedForRange(groupProjectIds, Instant.parse(filter.filter().start()), Instant.parse(filter.filter().end()));
        if (filter.detailed()) {
            return exportDetailedCsvforGroup(tracked, groupProjects);
        } else {
            return exportCsv(tracked, new ArrayList<>(), groupProjects);
        }
    }
}
