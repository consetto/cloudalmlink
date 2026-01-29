package com.consetto.adt.cloudalmlink.services;

import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;

/**
 * Service interface for accessing Cloud ALM plugin preferences.
 * Abstracts preference storage to enable dependency injection and testing.
 */
public interface IPreferenceService {

	/**
	 * Gets the current Cloud ALM configuration from preferences.
	 *
	 * @return The configuration, never null (but may have empty fields)
	 */
	CloudAlmConfig getCloudAlmConfig();

	/**
	 * Checks if demo mode is enabled.
	 *
	 * @return true if demo mode is enabled
	 */
	boolean isDemoModeEnabled();

	/**
	 * Gets the tenant name.
	 *
	 * @return The tenant name
	 */
	String getTenant();

	/**
	 * Gets the region.
	 *
	 * @return The region
	 */
	String getRegion();

	/**
	 * Gets the OAuth client ID.
	 *
	 * @return The client ID
	 */
	String getClientId();

	/**
	 * Gets the OAuth client secret.
	 *
	 * @return The client secret
	 */
	String getClientSecret();
}
