package com.consetto.adt.cloudalmlink.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.xml.stream.XMLStreamException;

import com.consetto.adt.cloudalmlink.core.VersionFeedParser;
import com.consetto.adt.cloudalmlink.core.VersionList;
import com.consetto.adt.cloudalmlink.handlers.CalmApiHandler;
import com.consetto.adt.cloudalmlink.services.ICloudAlmApiService;
import com.sap.adt.communication.content.ContentHandlerException;
import com.sap.adt.communication.message.IMessageBody;

/**
 * Holds version/transport data retrieved from ADT.
 * Parses the ATOM+XML versions feed with {@link VersionFeedParser}; {@link #arrange(String)} and
 * {@link #assignFeatures(Function)} then complete it.
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
	 * @return A new VersionData instance with the parsed versions
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
	 * @return A new VersionData instance with the parsed versions
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
		try (InputStream content = body.getContent()) {
			if (content != null) {
				versions.addAll(VersionFeedParser.parse(content));
			}
		} catch (XMLStreamException | IOException e) {
			throw new ContentHandlerException(e.getMessage(), e);
		}
	}

	/**
	 * Merges the object's current transport into the active version and sorts newest first.
	 *
	 * @param activeTransportId The transport from the object's transports endpoint, or null
	 */
	public void arrange(String activeTransportId) {
		VersionList.arrange(versions, activeTransportId);
	}

	/**
	 * Assigns Cloud ALM features to the versions. Does nothing if Cloud ALM is not configured.
	 *
	 * @param parentResolver Returns the request of a task, or its argument if it is none
	 */
	public void assignFeatures(Function<String, String> parentResolver) {
		if (apiService == null || !apiService.isConfigured()) {
			return;
		}
		VersionList.assignFeatures(versions, apiService::getFeature, parentResolver);
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
}
