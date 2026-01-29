package com.consetto.adt.cloudalmlink.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.consetto.adt.cloudalmlink.handlers.CalmApiHandler;
import com.consetto.adt.cloudalmlink.services.ICloudAlmApiService;
import com.sap.adt.communication.content.ContentHandlerException;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.tools.core.content.AdtStaxContentHandlerUtility;

/**
 * Holds version/transport data retrieved from ADT.
 * Parses ATOM+XML responses and enriches versions with Cloud ALM feature data.
 * No longer a singleton - instances are created via factory methods.
 */
public final class VersionData {

	private final List<VersionElement> versions;
	private final ICloudAlmApiService apiService;

	/**
	 * Creates a new VersionData instance with the specified API service.
	 *
	 * @param apiService The Cloud ALM API service for feature lookup
	 */
	public VersionData(ICloudAlmApiService apiService) {
		this.versions = new ArrayList<>();
		this.apiService = apiService;
	}

	/**
	 * Creates a new VersionData instance with a default API handler.
	 */
	public VersionData() {
		this(new CalmApiHandler());
	}

	/**
	 * Factory method to create and populate VersionData from an ATOM+XML response body.
	 *
	 * @param body The message body containing ATOM+XML feed data
	 * @param apiService The Cloud ALM API service for feature lookup
	 * @return A new VersionData instance with parsed and enriched data
	 */
	public static VersionData fromMessageBody(IMessageBody body, ICloudAlmApiService apiService) {
		VersionData data = new VersionData(apiService);
		data.parseBody(body);
		return data;
	}

	/**
	 * Factory method to create and populate VersionData from an ATOM+XML response body.
	 * Uses a default API handler.
	 *
	 * @param body The message body containing ATOM+XML feed data
	 * @return A new VersionData instance with parsed and enriched data
	 */
	public static VersionData fromMessageBody(IMessageBody body) {
		VersionData data = new VersionData();
		data.parseBody(body);
		return data;
	}

	/**
	 * Parses the ATOM+XML response body and populates version elements.
	 *
	 * @param body The message body containing ATOM+XML feed data
	 */
	public void parseBody(IMessageBody body) {
		versions.clear();
		AdtStaxContentHandlerUtility xmlUtility = new AdtStaxContentHandlerUtility();

		XMLStreamReader xsr = null;
		try {
			xsr = xmlUtility.getXMLStreamReader(body);
			VersionElement versionElement = null;
			String previousElement = null;

			for (int event = xsr.next(); event != XMLStreamReader.END_DOCUMENT; event = xsr.next()) {
				if (event == XMLStreamReader.START_ELEMENT) {
					if ("entry".contentEquals(xsr.getLocalName())) {
						versionElement = new VersionElement();
						versions.add(versionElement);
						continue;
					}
					if (versionElement != null) {
						if ("id".contentEquals(xsr.getLocalName())) {
							versionElement.setID(xsr.getElementText());
						}
						if ("link".contentEquals(xsr.getLocalName())) {
							versionElement.setTransport(xsr.getAttributeValue(null, "name"));
						}
						if ("title".contentEquals(xsr.getLocalName())) {
							versionElement.setTitle(xsr.getElementText());
						}
						if ("updated".contentEquals(xsr.getLocalName())) {
							versionElement.setLastUpdate(xsr.getElementText());
						}
						// Author name is nested within the author element
						if ("name".contentEquals(xsr.getLocalName()) && "author".contentEquals(previousElement)) {
							versionElement.setAuthor(xsr.getElementText());
						}
						previousElement = xsr.getLocalName();
					}
				}
			}
		} catch (XMLStreamException e) {
			throw new ContentHandlerException(e.getMessage(), e);
		} catch (NumberFormatException e) {
			throw new ContentHandlerException(e.getMessage(), e);
		} finally {
			if (xsr != null) {
				xmlUtility.closeXMLStreamReader(xsr);
			}
		}

		// Sort versions by ID in descending order (newest first)
		versions.sort((v1, v2) -> v2.getID().compareTo(v1.getID()));

		// Enrich versions with Cloud ALM feature data
		assignFeatures();
	}

	/**
	 * Fetches and assigns Cloud ALM features for all versions with transport IDs.
	 */
	private void assignFeatures() {
		if (apiService == null) {
			return;
		}

		for (VersionElement version : versions) {
			if (version.getTransportId() != null && !version.getTransportId().isEmpty()) {
				FeatureElement feature = apiService.getFeature(version.getTransportId());
				if (feature != null) {
					version.setFeature(feature);
				}
			}
		}
	}

	/**
	 * Gets the list of versions.
	 *
	 * @return An unmodifiable view of the versions list
	 */
	public List<VersionElement> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	/**
	 * Adds an "Active" version entry at the beginning of the versions list.
	 * This represents the current working version with its transport assignment.
	 *
	 * @param transportId The transport request ID for the active version
	 */
	public void addActiveVersion(String transportId) {
		if (transportId == null || transportId.isEmpty()) {
			return;
		}

		// Create active version element
		VersionElement activeVersion = new VersionElement();
		activeVersion.setID("Active");
		activeVersion.setTransport(transportId);
		activeVersion.setTitle("Current working version");
		activeVersion.setLastUpdate(java.time.Instant.now().toString());

		// Fetch Cloud ALM feature for the active transport
		if (apiService != null) {
			FeatureElement feature = apiService.getFeature(transportId);
			if (feature != null) {
				activeVersion.setFeature(feature);
			}
		}

		// Add at the beginning of the list
		versions.add(0, activeVersion);
	}

	/**
	 * Gets the number of versions.
	 *
	 * @return The version count
	 */
	public int size() {
		return versions.size();
	}

	/**
	 * Checks if there are no versions.
	 *
	 * @return true if the versions list is empty
	 */
	public boolean isEmpty() {
		return versions.isEmpty();
	}

	// Legacy singleton support for backward compatibility during migration
	// TODO: Remove after all callers are updated to use instance methods

	private static volatile VersionData instance;

	/**
	 * @deprecated Use instance methods or factory methods instead.
	 *             This method is provided for backward compatibility during migration.
	 */
	@Deprecated
	public static VersionData getInstance() {
		if (instance == null) {
			synchronized (VersionData.class) {
				if (instance == null) {
					instance = new VersionData();
				}
			}
		}
		return instance;
	}

	/**
	 * @deprecated Use instance methods instead.
	 *             Sets the shared instance for backward compatibility.
	 */
	@Deprecated
	public static void setInstance(VersionData data) {
		synchronized (VersionData.class) {
			instance = data;
		}
	}
}
