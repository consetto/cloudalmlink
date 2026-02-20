package com.consetto.adt.cloudalmlink.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Cloud ALM feature entity.
 * Deserialized from JSON API responses via Gson.
 * Contains feature metadata such as display ID, title, status, and project information.
 */
public class FeatureElement {

	/** Represents a task assignment (e.g., CALMREQU for Requirement). */
	public static class TaskAssignment {
		private String uuid;
		private String parent_uuid;
		private String title;
		private String type;

		public String getTitle() { return title; }
		public String getType() { return type; }

		public void setTitle(String title) { this.title = title; }
		public void setType(String type) { this.type = type; }
	}

	/** Represents an expanded navigation property (e.g., toWorkstream, toScope, toRelease). */
	public static class ExpandedEntity {
		private String uuid;
		private String name;

		public String getUuid() { return uuid; }
		public String getName() { return name; }

		public void setUuid(String uuid) { this.uuid = uuid; }
		public void setName(String name) { this.name = name; }
	}

	/** Maps technical status codes to Cloud ALM Frontend display labels. */
	private static final Map<String, String> STATUS_DISPLAY_MAP = Map.of(
			"CREATED", "In Specification",
			"NOT_PLANNED", "Not Planned",
			"IN_REALIZATION", "In Implementation",
			"IN_TESTING", "In Testing",
			"SUCCESSFULLY_TESTED", "Successfully Tested",
			"APPROVED_FOR_DEPLOYMENT", "Ready for Production",
			"CONFIRMED", "Deployed"
	);

	/** Maps priority codes to display labels. */
	private static final Map<Integer, String> PRIORITY_DISPLAY_MAP = Map.of(
			10, "Very High", 20, "High", 30, "Medium", 40, "Low"
	);

	private String uuid;
	private String displayId;
	private String title;
	private String projectId;
	private String description;
	private String modifiedAt;
	private String scopeId;
	private String statusCode;
	private int priorityCode;
	private String type;
	private String responsibleId;
	private String releaseId;
	private String workstreamId;

	private ExpandedEntity toWorkstream;
	private ExpandedEntity toScope;
	private ExpandedEntity toRelease;
	private List<TaskAssignment> toTaskAssignments;

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setDisplayId(String displayId) {
		this.displayId = displayId;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setModifiedAt(String modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

	public void setScopeId(String scopeId) {
		this.scopeId = scopeId;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	public void setPriorityCode(int priorityCode) {
		this.priorityCode = priorityCode;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setResponsibleId(String responsibleId) {
		this.responsibleId = responsibleId;
	}

	public void setReleaseId(String releaseId) {
		this.releaseId = releaseId;
	}

	public void setWorkstreamId(String workstreamId) {
		this.workstreamId = workstreamId;
	}

	public void setToWorkstream(ExpandedEntity toWorkstream) {
		this.toWorkstream = toWorkstream;
	}

	public void setToScope(ExpandedEntity toScope) {
		this.toScope = toScope;
	}

	public void setToRelease(ExpandedEntity toRelease) {
		this.toRelease = toRelease;
	}

	public void setToTaskAssignments(List<TaskAssignment> toTaskAssignments) {
		this.toTaskAssignments = toTaskAssignments;
	}

	// --- Getters ---

	public String getUuid() {
		return uuid;
	}

	public String getDisplayId() {
		return displayId;
	}

	public String getTitle() {
		return title;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getDescription() {
		return description;
	}

	public String getModifiedAt() {
		return modifiedAt;
	}

	public String getScopeId() {
		return scopeId;
	}

	public int getPriorityCode() {
		return priorityCode;
	}

	public String getType() {
		return type;
	}

	public String getResponsibleId() {
		return responsibleId;
	}

	public String getReleaseId() {
		return releaseId;
	}

	public String getWorkstreamId() {
		return workstreamId;
	}

	public String getStatus() {
		if (statusCode == null) {
			return null;
		}
		return STATUS_DISPLAY_MAP.getOrDefault(statusCode, statusCode);
	}

	/** Returns priority display text (e.g., "Very High", "High", "Medium", "Low"). */
	public String getPriority() {
		return PRIORITY_DISPLAY_MAP.getOrDefault(priorityCode, "");
	}

	/** Returns the modified date as YYYY-MM-DD, or empty string if not set. */
	public String getModifiedDate() {
		if (modifiedAt == null || modifiedAt.length() < 10) {
			return "";
		}
		return modifiedAt.substring(0, 10);
	}

	/** Returns the workstream name from expanded entity, or empty string. */
	public String getWorkstreamName() {
		return toWorkstream != null ? toWorkstream.getName() : "";
	}

	/** Returns the scope name from expanded entity, or empty string. */
	public String getScopeName() {
		return toScope != null ? toScope.getName() : "";
	}

	/** Returns the release name from expanded entity, or empty string. */
	public String getReleaseName() {
		return toRelease != null ? toRelease.getName() : "";
	}

	/** Returns the title of the first CALMREQU task assignment, or empty string. */
	public String getRequirementTitle() {
		if (toTaskAssignments == null) {
			return "";
		}
		return toTaskAssignments.stream()
				.filter(t -> "CALMREQU".equals(t.getType()))
				.map(TaskAssignment::getTitle)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse("");
	}
}
