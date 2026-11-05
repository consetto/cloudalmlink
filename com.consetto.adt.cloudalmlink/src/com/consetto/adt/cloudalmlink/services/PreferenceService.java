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
	 * Reads the client secret of the configured account from Eclipse Secure Storage. A secret that
	 * an older plugin version kept in the plain-text preferences is moved there first. While Secure
	 * Storage cannot be used, the plain secret is returned, so the plugin keeps working and nothing
	 * is lost.
	 */
	@Override
	public String getClientSecret() {
		String plain = preferenceStore.getString(PreferenceConstants.P_KEY);
		String account = secretAccount();
		try {
			if (moveToSecureStorage(account)) {
				return plain;
			}
			return SecretStore.read(account);
		} catch (Exception e) {
			// The message names the storage problem, never the secret
			CloudAlmLinkLogger.logWarning("Cannot use Eclipse Secure Storage for the client secret: " + e.getMessage());
			return plain;
		}
	}

	/**
	 * Moves a client secret from the plain-text preferences into Secure Storage. The plain value is
	 * removed only after Secure Storage holds it, so a failure keeps it for the next attempt.
	 *
	 * @return true if a secret was moved
	 */
	private synchronized boolean moveToSecureStorage(String account) throws Exception {
		boolean moved = SecretMigration.migrate(new SecretMigration.PlainSource() {
			@Override
			public String read() {
				return preferenceStore.getString(PreferenceConstants.P_KEY);
			}

			@Override
			public void clear() {
				preferenceStore.setToDefault(PreferenceConstants.P_KEY);
			}
		}, secret -> SecretStore.write(account, secret));
		if (moved) {
			preferenceStore.save();
			CloudAlmLinkLogger.logInfo("Moved the Cloud ALM client secret to Eclipse Secure Storage");
		}
		return moved;
	}

	/**
	 * Stores the client secret of the configured account in Eclipse Secure Storage and removes any
	 * plain-text copy, so an old copy can never replace it later.
	 *
	 * @param secret The secret, or empty to remove it
	 * @throws StorageException If it cannot be encrypted
	 * @throws IOException If the storage or the preferences cannot be written
	 */
	public synchronized void setClientSecret(String secret) throws StorageException, IOException {
		SecretStore.write(secretAccount(), secret);
		if (!preferenceStore.isDefault(PreferenceConstants.P_KEY)) {
			preferenceStore.setToDefault(PreferenceConstants.P_KEY);
			preferenceStore.save();
		}
	}

	private String secretAccount() {
		return CloudAlmConfig.secretAccount(getTenant(), getRegion(), getClientId());
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
