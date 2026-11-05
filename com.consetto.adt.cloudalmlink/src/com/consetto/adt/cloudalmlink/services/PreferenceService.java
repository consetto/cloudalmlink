package com.consetto.adt.cloudalmlink.services;

import java.io.IOException;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.CloudAlmLinkConstants;
import com.consetto.adt.cloudalmlink.core.SecretMigration;
import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;

/**
 * Default implementation of IPreferenceService using Eclipse's ScopedPreferenceStore.
 * Provides centralized access to plugin preferences.
 */
public class PreferenceService implements IPreferenceService {

	private static volatile PreferenceService instance;
	private final ScopedPreferenceStore preferenceStore;
	private volatile boolean secretMigrated;

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

	/**
	 * Reads the client secret from Eclipse Secure Storage. On first use it moves a secret that an
	 * older plugin version kept in the plain-text preferences.
	 */
	@Override
	public String getClientSecret() {
		migrateSecret();
		try {
			return SecretStore.read();
		} catch (StorageException e) {
			// The message names the storage problem, never the secret
			CloudAlmLinkLogger.logWarning("Cannot read the client secret from Secure Storage: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Moves a client secret from the plain-text preferences into Secure Storage, once per session.
	 * The plain value is removed only after Secure Storage holds it.
	 */
	public synchronized void migrateSecret() {
		if (secretMigrated) {
			return;
		}
		try {
			boolean moved = SecretMigration.migrate(new SecretMigration.PlainSource() {
				@Override
				public String read() {
					return preferenceStore.getString(PreferenceConstants.P_KEY);
				}

				@Override
				public void clear() {
					preferenceStore.setToDefault(PreferenceConstants.P_KEY);
				}
			}, SecretStore::write);
			if (moved) {
				preferenceStore.save();
				CloudAlmLinkLogger.logInfo("Moved the Cloud ALM client secret to Eclipse Secure Storage");
			}
			secretMigrated = true;
		} catch (Exception e) {
			// Keep the plain value and try again next time
			CloudAlmLinkLogger.logWarning("Cannot move the client secret to Secure Storage: " + e.getMessage());
		}
	}

	/**
	 * Stores the client secret in Eclipse Secure Storage.
	 *
	 * @param secret The secret, or empty to remove it
	 * @throws StorageException If it cannot be encrypted
	 * @throws IOException If the storage cannot be written
	 */
	public void setClientSecret(String secret) throws StorageException, IOException {
		SecretStore.write(secret);
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
