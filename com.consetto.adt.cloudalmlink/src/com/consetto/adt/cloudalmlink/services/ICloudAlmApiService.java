package com.consetto.adt.cloudalmlink.services;

import com.consetto.adt.cloudalmlink.model.FeatureElement;

/**
 * Service interface for Cloud ALM API operations.
 * Abstracts the REST API communication for dependency injection and testing.
 */
public interface ICloudAlmApiService extends AutoCloseable {

	/**
	 * Fetches the Cloud ALM feature associated with a transport.
	 *
	 * @param transportId The transport request ID
	 * @return The feature element, or null if not found
	 */
	FeatureElement getFeature(String transportId);

	/**
	 * Checks if the service is properly configured and ready to make API calls.
	 *
	 * @return true if the service is configured
	 */
	boolean isConfigured();

	/**
	 * Closes the service and releases any resources.
	 */
	@Override
	void close();
}
