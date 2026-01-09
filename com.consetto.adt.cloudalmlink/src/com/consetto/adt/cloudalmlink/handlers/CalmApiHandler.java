package com.consetto.adt.cloudalmlink.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.model.BearerToken;
import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;
import com.google.gson.Gson;

/**
 * Handles communication with the SAP Cloud ALM REST API.
 * Manages OAuth 2.0 Bearer Token authentication and feature retrieval.
 */
public class CalmApiHandler {

	private BearerToken token = null;
	private CloseableHttpClient httpClient = HttpClients.createDefault();
	private HttpPost httpTokenPost = null;
	private String apiURL = null;

	/**
	 * Constructor initializes the API handler with credentials from preferences.
	 * Sets up the OAuth token request configuration.
	 */
	public CalmApiHandler() {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		String region = scopedPreferenceStore.getString(PreferenceConstants.P_REG);
		String tenant = scopedPreferenceStore.getString(PreferenceConstants.P_TEN);
		String clientId = scopedPreferenceStore.getString(PreferenceConstants.P_CID);
		String clientSecret = scopedPreferenceStore.getString(PreferenceConstants.P_KEY);

		apiURL = "https://" + tenant + "." + region + ".alm.cloud.sap/api/calm-features/v1";
		String tokenUrl = "https://" + tenant + ".authentication." + region + ".hana.ondemand.com/oauth/token";

		// Prepare HTTP POST request for OAuth access token
		String base64Credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
		httpTokenPost = new HttpPost(tokenUrl);
		httpTokenPost.setHeader("Authorization", "Basic " + base64Credentials);
		httpTokenPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		httpTokenPost.setEntity(new StringEntity("grant_type=client_credentials"));
	}

	/**
	 * Retrieves the parent feature for a given transport from Cloud ALM API.
	 *
	 * @param transportID The SAP transport request ID
	 * @param version The version element to associate the feature with
	 * @return The feature element (set via response handler on version object)
	 */
	public void getFeature(String transportID, VersionElement version) {
		// Ensure we have a valid OAuth token
		if (token == null || !token.isValid()) {
			getOAuthToken();
		}

		// Build API URL for parent feature lookup
		String transportAPIUrl = apiURL + "/Transports/" + transportID + "/parent";
		HttpGet httpGet = new HttpGet(transportAPIUrl);

		// Set authorization header with Bearer token
		httpGet.setHeader("Authorization", "Bearer " + token.getToken());
		httpGet.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");

		try {
			// Execute request; response handler updates the version element
			httpClient.execute(httpGet, new FeatureResponseHandler(version));
		} catch (IOException e) {
			// Log error silently - feature will remain unset for this version
		}
	}

	/**
	 * Obtains an OAuth 2.0 access token from the SAP authentication server.
	 * Token is cached and reused until expiration.
	 */
	private void getOAuthToken() {
		try {
			token = httpClient.execute(httpTokenPost, response -> {
				int statusCode = response.getCode();
				if (statusCode != 200) {
					return null;
				}

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String content = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);

					// Deserialize JSON response to BearerToken object
					Gson gson = new Gson();
					BearerToken newToken = gson.fromJson(content, BearerToken.class);
					newToken.setExpirationTime();
					return newToken;
				}
				return null;
			});
		} catch (IOException e) {
			// Authentication failed - subsequent API calls will retry
		}
	}
}
