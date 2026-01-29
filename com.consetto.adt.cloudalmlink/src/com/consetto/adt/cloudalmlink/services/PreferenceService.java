package com.consetto.adt.cloudalmlink.services;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.CloudAlmLinkConstants;
import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;

/**
 * Default implementation of IPreferenceService using Eclipse's ScopedPreferenceStore.
 * Provides centralized access to plugin preferences.
 */
public class PreferenceService implements IPreferenceService {

	private static volatile PreferenceService instance;
	private final ScopedPreferenceStore preferenceStore;

	/**
	 * Creates a new PreferenceService.
	 */
	public PreferenceService() {
		this.preferenceStore = new ScopedPreferenceStore(
				InstanceScope.INSTANCE,
				CloudAlmLinkConstants.PREFERENCE_QUALIFIER
		);
	}

	/**
	 * Gets the singleton instance of the preference service.
	 * Thread-safe lazy initialization.
	 *
	 * @return The preference service instance
	 */
	public static PreferenceService getInstance() {
		if (instance == null) {
			synchronized (PreferenceService.class) {
				if (instance == null) {
					instance = new PreferenceService();
				}
			}
		}
		return instance;
	}

	@Override
	public CloudAlmConfig getCloudAlmConfig() {
		return new CloudAlmConfig(
				getTenant(),
				getRegion(),
				getClientId(),
				getClientSecret()
		);
	}

	@Override
	public boolean isDemoModeEnabled() {
		return preferenceStore.getBoolean(PreferenceConstants.P_DEMO);
	}

	@Override
	public String getTenant() {
		return preferenceStore.getString(PreferenceConstants.P_TEN);
	}

	@Override
	public String getRegion() {
		return preferenceStore.getString(PreferenceConstants.P_REG);
	}

	@Override
	public String getClientId() {
		return preferenceStore.getString(PreferenceConstants.P_CID);
	}

	@Override
	public String getClientSecret() {
		return preferenceStore.getString(PreferenceConstants.P_KEY);
	}

	/**
	 * Gets the underlying preference store.
	 * Used primarily for the preference page.
	 *
	 * @return The ScopedPreferenceStore
	 */
	public ScopedPreferenceStore getPreferenceStore() {
		return preferenceStore;
	}
}
