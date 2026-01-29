package com.consetto.adt.cloudalmlink.model;

import com.consetto.adt.cloudalmlink.CloudAlmLinkConstants;

/**
 * Immutable configuration record for Cloud ALM connection settings.
 * Centralizes URL building logic that was previously duplicated across multiple classes.
 */
public record CloudAlmConfig(
		String tenant,
		String region,
		String clientId,
		String clientSecret
) {

	/**
	 * Checks if the configuration has all required fields populated.
	 *
	 * @return true if all required fields are non-empty
	 */
	public boolean isValid() {
		return isNotEmpty(tenant) && isNotEmpty(region) && isNotEmpty(clientId) && isNotEmpty(clientSecret);
	}

	/**
	 * Checks if connection settings (tenant and region) are configured.
	 *
	 * @return true if tenant and region are non-empty
	 */
	public boolean hasConnectionSettings() {
		return isNotEmpty(tenant) && isNotEmpty(region);
	}

	/**
	 * Builds the base Cloud ALM URL.
	 *
	 * @return The base URL (e.g., "https://tenant.eu10.alm.cloud.sap")
	 */
	public String baseUrl() {
		return "https://" + tenant + "." + region + CloudAlmLinkConstants.CLOUD_ALM_DOMAIN;
	}

	/**
	 * Builds the Cloud ALM Features API URL.
	 *
	 * @return The API URL (e.g., "https://tenant.eu10.alm.cloud.sap/api/calm-features/v1")
	 */
	public String apiUrl() {
		return baseUrl() + CloudAlmLinkConstants.FEATURES_API_PATH;
	}

	/**
	 * Builds the OAuth token endpoint URL.
	 *
	 * @return The token URL (e.g., "https://tenant.authentication.eu10.hana.ondemand.com/oauth/token")
	 */
	public String tokenUrl() {
		return "https://" + tenant + ".authentication." + region + CloudAlmLinkConstants.AUTH_DOMAIN + CloudAlmLinkConstants.OAUTH_TOKEN_PATH;
	}

	/**
	 * Builds the URL for a Cloud ALM feature.
	 *
	 * @param featureId The feature ID (e.g., "6-1234")
	 * @return The feature URL
	 */
	public String featureUrl(String featureId) {
		return baseUrl() + CloudAlmLinkConstants.FEATURE_LAUNCHPAD_FRAGMENT + featureId;
	}

	/**
	 * Builds the URL for a Cloud ALM task.
	 *
	 * @param taskId The task ID (e.g., "3-5678")
	 * @return The task URL
	 */
	public String taskUrl(String taskId) {
		return baseUrl() + CloudAlmLinkConstants.TASK_LAUNCHPAD_FRAGMENT + taskId;
	}

	/**
	 * Builds the URL for a Cloud ALM document.
	 *
	 * @param documentId The document ID (e.g., "7-9012")
	 * @return The document URL
	 */
	public String documentUrl(String documentId) {
		return baseUrl() + CloudAlmLinkConstants.DOCUMENT_LAUNCHPAD_FRAGMENT + documentId + "')";
	}

	/**
	 * Builds the URL for a Cloud ALM library element.
	 *
	 * @param libraryId The library ID (e.g., "15-3456")
	 * @return The library URL
	 */
	public String libraryUrl(String libraryId) {
		return baseUrl() + CloudAlmLinkConstants.LIBRARY_LAUNCHPAD_FRAGMENT + libraryId + "')";
	}

	private static boolean isNotEmpty(String value) {
		return value != null && !value.isEmpty();
	}
}
