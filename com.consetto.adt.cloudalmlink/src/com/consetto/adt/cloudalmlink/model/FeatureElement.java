package com.consetto.adt.cloudalmlink.model;

public class FeatureElement {
	
//	the class serializes the following JSON:	// {
//    "@context": "$metadata#Features/$entity",
//    "@metadataEtag": "W/\"577a8880a1e05a0dc92b127259f1c30a74f0b9c364bb075c8fa73159e3bad41b\"",
//    "uuid": "62c425d6-c4ba-4ae8-aac9-22f4e81f948d",
//    "displayId": "6-27",
//    "title": "New Feature",
//    "projectId": "6ed5952a-333c-497b-9203-2725bdbd93f6",
//    "description": "<p>Feature Testing 1</p>",
//    "modifiedAt": "2023-08-03T07:33:31Z",
//    "scopeId": "e6af5d23-5887-4b43-acdc-49d3c2cdf13f",
//    "statusCode": "IN_REALIZATION",
//    "priorityCode": 10,
//    "type": "CALMFEAT",
//    "responsibleId": null,
//    "releaseId": null,
//    "workstreamId": "WS009",
//    "tags": []
//}
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

	public String getDisplayId() {
		return displayId;

	}

	public String getStatus() {
		return statusCode;

	}
	
	public String getProjectId() {
		return projectId;
	}
	
	// get responsible
	public String getResponsibleId() {
		return responsibleId;
	}

}
