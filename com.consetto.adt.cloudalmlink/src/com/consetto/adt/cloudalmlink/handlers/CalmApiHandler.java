package com.consetto.adt.cloudalmlink.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import com.consetto.adt.cloudalmlink.model.BearerToken;
import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.services.ICloudAlmApiService;
import com.consetto.adt.cloudalmlink.services.PreferenceService;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Handles communication with the SAP Cloud ALM REST API.
 * Manages OAuth 2.0 Bearer Token authentication and feature retrieval.
 * Implements ICloudAlmApiService for dependency injection and AutoCloseable for resource cleanup.
 */
public class CalmApiHandler implements ICloudAlmApiService {

	/** Transport IDs go into the request path, so only plain IDs like S4DK911940 are accepted. */
	private static final Pattern TRANSPORT_ID = Pattern.compile("[A-Za-z0-9]{1,20}");

	/** Without timeouts an unreachable host blocks the caller (today: the UI thread) indefinitely. */
	private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(10);
	private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(30);

	private BearerToken token = null;
	private final CloseableHttpClient httpClient;
	private final HttpClientConnectionManager connectionManager;
	private final CloudAlmConfig config;
	private HttpPost httpTokenPost = null;

	/**
	 * Constructor initializes the API handler with credentials from preferences.
	 * Sets up the OAuth token request configuration and connection pool.
	 */
	public CalmApiHandler() {
		this(PreferenceService.getInstance().getCloudAlmConfig());
	}

	/**
	 * Constructor with explicit configuration for testing and dependency injection.
	 *
	 * @param config The Cloud ALM configuration
	 */
	public CalmApiHandler(CloudAlmConfig config) {
		this.config = config;

		// Create connection pool for efficient HTTP connection management
		this.connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setMaxConnTotal(10)
				.setMaxConnPerRoute(5)
				.setDefaultConnectionConfig(ConnectionConfig.custom()
						.setConnectTimeout(CONNECT_TIMEOUT)
						.setSocketTimeout(RESPONSE_TIMEOUT)
						.build())
				.build();
		this.httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectionRequestTimeout(CONNECT_TIMEOUT)
						.setResponseTimeout(RESPONSE_TIMEOUT)
						.build())
				.build();

		if (config.isValid()) {
			initializeTokenRequest();
		}
	}

	/**
	 * Initializes the HTTP POST request for OAuth token retrieval.
	 */
	private void initializeTokenRequest() {
		String base64Credentials = Base64.getEncoder()
				.encodeToString((config.clientId() + ":" + config.clientSecret()).getBytes(StandardCharsets.UTF_8));
		httpTokenPost = new HttpPost(config.tokenUrl());
		httpTokenPost.setHeader("Authorization", "Basic " + base64Credentials);
		httpTokenPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
		httpTokenPost.setEntity(new StringEntity("grant_type=client_credentials"));
	}

	@Override
	public boolean isConfigured() {
		return config != null && config.isValid();
	}

	@Override
	public FeatureElement getFeature(String transportId) {
		if (!isConfigured()) {
			CloudAlmLinkLogger.logWarning("CalmApiHandler is not configured - cannot fetch feature");
			return null;
		}

		if (transportId == null || !TRANSPORT_ID.matcher(transportId).matches()) {
			CloudAlmLinkLogger.logWarning("Ignoring invalid transport ID: " + transportId);
			return null;
		}

		// Ensure we have a valid OAuth token
		if (token == null || !token.isValid()) {
			getOAuthToken();
		}

		if (token == null) {
			CloudAlmLinkLogger.logWarning("No OAuth token available - cannot fetch feature");
			return null;
		}

		// Build API URL for parent feature lookup
		String transportAPIUrl = config.apiUrl() + "/Transports/" + transportId
				+ "/parent?$expand=toWorkstream,toScope,toRelease,toTaskAssignments";
		HttpGet httpGet = new HttpGet(transportAPIUrl);

		// Set authorization header with Bearer token
		httpGet.setHeader("Authorization", "Bearer " + token.getToken());
		httpGet.setHeader("Accept", "application/json");

		try {
			return httpClient.execute(httpGet, response -> {
				int statusCode = response.getCode();
				if (statusCode != 200) {
					CloudAlmLinkLogger.logWarning("Feature API returned status code: " + statusCode + " for transport " + transportId);
					return null;
				}

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try (InputStream inputStream = entity.getContent()) {
						String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
						Gson gson = new Gson();
						return gson.fromJson(content, FeatureElement.class);
					}
				}
				return null;
			});
		} catch (IOException | JsonParseException e) {
			CloudAlmLinkLogger.logWarning("Failed to fetch feature for transport " + transportId + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Obtains an OAuth 2.0 access token from the SAP authentication server.
	 * Token is cached and reused until expiration.
	 */
	private void getOAuthToken() {
		if (httpTokenPost == null) {
			CloudAlmLinkLogger.logWarning("Token request not initialized - check configuration");
			return;
		}

		try {
			token = httpClient.execute(httpTokenPost, response -> {
				int statusCode = response.getCode();
				if (statusCode != 200) {
					CloudAlmLinkLogger.logWarning("OAuth token request failed with status code: " + statusCode);
					return null;
				}

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try (InputStream inputStream = entity.getContent()) {
						String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

						Gson gson = new Gson();
						BearerToken rawToken = gson.fromJson(content, BearerToken.class);
						// Create new token with calculated expiration time
						return BearerToken.withCalculatedExpiration(rawToken);
					}
				}
				return null;
			});
		} catch (IOException | JsonParseException e) {
			CloudAlmLinkLogger.logError("OAuth authentication failed", e);
		}
	}

	/**
	 * Gets the current Cloud ALM configuration.
	 *
	 * @return The configuration
	 */
	public CloudAlmConfig getConfig() {
		return config;
	}

	/**
	 * Closes the HTTP client and releases all associated resources.
	 * Should be called when the handler is no longer needed.
	 */
	@Override
	public void close() {
		try {
			if (httpClient != null) {
				httpClient.close();
			}
		} catch (IOException e) {
			CloudAlmLinkLogger.logWarning("Error closing HTTP client: " + e.getMessage());
		}
		try {
			if (connectionManager != null) {
				connectionManager.close();
			}
		} catch (IOException e) {
			CloudAlmLinkLogger.logWarning("Error closing connection manager: " + e.getMessage());
		}
	}
}
