package com.consetto.adt.cloudalmlink.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.google.gson.Gson;

public class FeatureResponseHandler implements  HttpClientResponseHandler<String>{
	
	
	private VersionElement version;
	
	// constructor with version element as input parameter
	
	public FeatureResponseHandler(VersionElement version) {
		this.version = version;
	}

	@Override
	public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
		
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
			FeatureElement feature = gson.fromJson(content, FeatureElement.class);
			System.out.println("Token: " + feature.toString());

			System.out.println("Response body: " + content);
			version.setFeature(feature);
	}
		return null;
	}


}
