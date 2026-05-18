package de.OneManProjects.export;

import de.OneManProjects.data.Project;
import de.OneManProjects.data.Tracked;

import java.util.List;

public record UserDataExport(List<Project> projects, List<Tracked> trackedList) {
}
