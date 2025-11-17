package com.consetto.adt.cloudalmlink.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.consetto.adt.cloudalmlink.handlers.CalmApiHandler;
import com.sap.adt.communication.content.ContentHandlerException;
//import com.consetto.adt.cloudalmlink.handlers.ContentHandlerException;
//import com.consetto.adt.cloudalmlink.handlers.TodoData;
//import com.consetto.adt.cloudalmlink.handlers.XMLStreamException;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.tools.core.content.AdtStaxContentHandlerUtility;

public enum VersionData {
	INSTANCE;


	private List<VersionElement> versions;
	private CalmApiHandler calmApiHandler = new CalmApiHandler();


	VersionData() {
		versions = new ArrayList<VersionElement>();
		
	}

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
		
//		// order by versionId descending
		versions.sort((v1, v2) -> v2.getID().compareTo
				(v1.getID()));
		
		// Read Features from CALM API and assign to versions
		assignFeatures();
	}
	
	private void assignFeatures(){
		
		// loop through the versions and set a Feaure ID for all versions where the transportId is not empty
		for (VersionElement version : versions) {
			if (version.getTransportId() != null && !version.getTransportId().isEmpty()) {
//				FeatureElement calmFeature = getFeature(version.getTransportId());
				getFeature(version.getTransportId(),version);
//				version.setFeature(calmFeature);
			} 
		}
		
	}

	private FeatureElement getFeature(String transportId, VersionElement version) {
		
	  return  calmApiHandler.getFeature(transportId, version);
	
	}

	public Iterable<VersionElement> getVersions() {
		return versions;

	}



}
