package com.consetto.adt.cloudalmlink.model;

/**
 * Represents a single version/transport entry from ADT.
 * Contains version metadata and an optional associated Cloud ALM feature.
 */
public class VersionElement {

	private String versionId;
	private String title;
	private String updated;
	private String transportId;
	private String author;
	private FeatureElement feature;

	public void setID(String versionId) {
		this.versionId = versionId;
	}

	public void setTransport(String transportId) {
		this.transportId = transportId;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setLastUpdate(String lastUpdate) {
		this.updated = lastUpdate;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTransportId() {
		return transportId;
	}

	public void setFeature(FeatureElement feature) {
		this.feature = feature;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public FeatureElement getFeature() {
		return feature;
	}

	public String getID() {
		return versionId;
	}

	public String getLastUpdate() {
		return updated;
	}
}
