package com.consetto.adt.cloudalmlink.model;

/**
 * Represents a Cloud ALM feature entity.
 * Deserialized from JSON API responses via Gson.
 * Contains feature metadata such as display ID, title, status, and project information.
 */
public class FeatureElement {

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

	public String getUuid() {
		return uuid;
	}

	public String getDisplayId() {
		return displayId;
	}

	public String getTitle() {
		return title;
	}

	public String getStatus() {
		return statusCode;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getResponsibleId() {
		return responsibleId;
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

	public String getReleaseId() {
		return releaseId;
	}

	public String getWorkstreamId() {
		return workstreamId;
	}
}
