package com.consetto.adt.cloudalmlink.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.consetto.adt.cloudalmlink.handlers.CalmApiHandler;
import com.sap.adt.communication.content.ContentHandlerException;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.tools.core.content.AdtStaxContentHandlerUtility;

/**
 * Singleton enum holding version/transport data retrieved from ADT.
 * Parses ATOM+XML responses and enriches versions with Cloud ALM feature data.
 */
public enum VersionData {
	INSTANCE;

	private List<VersionElement> versions;
	private CalmApiHandler calmApiHandler = new CalmApiHandler();

	VersionData() {
		versions = new ArrayList<VersionElement>();
	}

	/**
	 * Parses the ATOM+XML response body and populates version elements.
	 *
	 * @param body The message body containing ATOM+XML feed data
	 */
	public void parseBody(IMessageBody body) {
		versions = new ArrayList<VersionElement>();
		AdtStaxContentHandlerUtility xmlUtility = new AdtStaxContentHandlerUtility();

		XMLStreamReader xsr = null;
		try {
			xsr = xmlUtility.getXMLStreamReader(body);
			VersionElement versionElement = null;
			String previousElement = null;

			for (int event = xsr.next(); event != XMLStreamReader.END_DOCUMENT; event = xsr.next()) {
				switch (event) {
				case XMLStreamReader.START_ELEMENT:
					if ("entry".contentEquals(xsr.getLocalName())) {
						versionElement = new VersionElement();
						versions.add(versionElement);
						break;
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
		for (VersionElement version : versions) {
			if (version.getTransportId() != null && !version.getTransportId().isEmpty()) {
				calmApiHandler.getFeature(version.getTransportId(), version);
			}
		}
	}

	public Iterable<VersionElement> getVersions() {
		return versions;
	}
}
