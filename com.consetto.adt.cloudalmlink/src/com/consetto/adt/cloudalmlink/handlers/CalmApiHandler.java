package com.consetto.adt.cloudalmlink.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.model.BearerToken;
import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;
import com.google.gson.Gson;

public class CalmApiHandler {

	private BearerToken token = null;
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	private HttpPost httpTokenPost = null;
	private String apiURL = null;

	public CalmApiHandler() {

		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		String region = scopedPreferenceStore.getString(PreferenceConstants.P_REG);
		String tenant = scopedPreferenceStore.getString(PreferenceConstants.P_TEN);
		String clientId = scopedPreferenceStore.getString(PreferenceConstants.P_CID);
		String clientSecret = scopedPreferenceStore.getString(PreferenceConstants.P_KEY);

		apiURL = "https://" + tenant + "." + region + ".alm.cloud.sap/api/calm-features/v1";

		String tokenUrl = "https://" + tenant + ".authentication." + region + ".hana.ondemand.com/oauth/token";

		// prepare the HTTP POST request for the access token
		String base64Credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
		httpTokenPost = new HttpPost(tokenUrl);
		httpTokenPost.setHeader("Authorization", "Basic " + base64Credentials);
		String grant_type = "client_credentials";
		httpTokenPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		// add grant_type and scope to body
		httpTokenPost.setEntity(new StringEntity("grant_type=" + grant_type));

	}

	// method getParentFeature
	public FeatureElement getFeature(String transportID, VersionElement version) {

		if (token == null || !token.isValid()) {
			getOAuthToken();
		}

		FeatureElement feature = null;

		// Prepare the HTTP GET request to get the parent feature of the transport
		String transportAPIUrl = apiURL + "/Transports/" + transportID + "/parent";

		HttpGet httpGet = new HttpGet(transportAPIUrl);

		// Set the API key header
		String apiKey = token.getToken();
//		httpGet.setHeader("APIKey", apiKey);
		httpGet.setHeader("Authorization", "Bearer " + apiKey);

		// Set the Accept header
		httpGet.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");

		System.out.println("Executing request " + httpGet.toString());

		try {

			// execute the request with response handler
			httpClient.execute(httpGet, new FeatureResponseHandler(version));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return feature;

	}

	private void getOAuthToken() {

		try {
			CloseableHttpResponse response = httpClient.execute(httpTokenPost);

			// Check the response status code
			int statusCode = response.getCode();
			System.out.println("Response status code: " + statusCode);
			if (statusCode != 200) {
				System.out.println("Error: " + response.getReasonPhrase());
//				return null; // or throw an exception
			}
			// Get the response entity
			HttpEntity entity = response.getEntity();
			// Get response body
			if (entity != null) {

				String content = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);

				// serialize the JSON content to a FeatureElement object using Gson
				Gson gson = new Gson();
				token = gson.fromJson(content, BearerToken.class);
				token.setExpirationTime();

				System.out.println("Token: " + token.getToken());

			}
			response.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
