package com.consetto.adt.cloudalmlink.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;
import com.google.gson.Gson;
import com.sap.adt.communication.content.ContentHandlerException;
//import com.consetto.adt.cloudalmlink.handlers.ContentHandlerException;
//import com.consetto.adt.cloudalmlink.handlers.TodoData;
//import com.consetto.adt.cloudalmlink.handlers.XMLStreamException;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.tools.core.content.AdtStaxContentHandlerUtility;

public enum VersionData {
	INSTANCE;


	private List<VersionElement> versions;

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
		assignFeatures();
	}
	
	private void assignFeatures(){
		
		// loop through the versions and set a Feaure ID for all versions where the transportId is not empty
		for (VersionElement version : versions) {
			if (version.getTransportId() != null && !version.getTransportId().isEmpty()) {
				FeatureElement calmFeature = getFeature(version.getTransportId());
				version.setFeature(calmFeature);
			} 
		}
			
	
		
	}

	private FeatureElement getFeature(String transportId) {
		

		

		// TODO: we should not create  a new HTTP client for each call
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		String baseUrl = scopedPreferenceStore.getString(PreferenceConstants.P_API);
		
	    if (baseUrl.isEmpty()) {
		
	    	return null;
	    	
	    }
		
		FeatureElement feature  = null;
		
		String transportAPIUrl = baseUrl + "/Transports/" + transportId + "/parent";
		// replace double slashes with a single slash but do not replace ://
		// This is to ensure that the URL is correctly formatted
		// and does not contain double slashes
		// but also does not remove the protocol part of the URL
		// e.g. https://abc.xyz.alm.cloud.sap/Transports/12345/parent
		// This is needed because the base URL might end with a slash
		// and the transportId might start with a slash
		// so we need to ensure that the URL is correctly formatted
		transportAPIUrl = transportAPIUrl.replaceAll("([^:])//", "$1/");
		
		//	transportAPIUrl = transportAPIUrl.replaceAll("//", "/");
		

		HttpGet httpGet = new HttpGet(transportAPIUrl);
		
		// Set the API key header
		String apiKey = scopedPreferenceStore.getString(PreferenceConstants.P_KEY);
		httpGet.setHeader("APIKey", apiKey);
		
		// Set the Accept header
		httpGet.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");

		System.out.println("Executing request " + httpGet.toString());

		CloseableHttpResponse response;
		try {
			response = httpClient.execute(httpGet);

			httpClient.close();

			
			// Check the response status code
			int statusCode = response.getCode();
			System.out.println("Response status code: " + statusCode);
			if (statusCode != 200) {
				System.out.println("Error: " + response.getReasonPhrase());
				return null; // or throw an exception
			}
			// Get the response entity
			HttpEntity entity = response.getEntity();
			// Get response body
			if (entity != null) {
				

				String content = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);
				

				// serialize the JSON content to a FeatureElement object using Gson
				 Gson gson = new Gson();	
				 feature = gson.fromJson(content, FeatureElement.class);
				 System.out.println("Feature: " + feature.toString());
			
				
				
				System.out.println("Response body: " + content);
			}
			// Get response information
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return feature;
	}

	public Iterable<VersionElement> getVersions() {
		return versions;
		

	}



}
