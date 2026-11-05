package com.consetto.adt.cloudalmlink.services;

import java.io.IOException;

import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.consetto.adt.cloudalmlink.CloudAlmLinkConstants;

/**
 * Keeps Cloud ALM client secrets encrypted in Eclipse Secure Storage, one per Cloud ALM account.
 * Secure Storage is shared by all workspaces of the user, so keying by account keeps workspaces
 * with different tenants or clients from overwriting each other's secret. On macOS and Windows
 * Eclipse protects the storage with the system keychain; elsewhere it may ask for a master
 * password.
 */
public final class SecretStore {

	private static final String NODE = CloudAlmLinkConstants.PLUGIN_ID + "/clientSecrets";

	private SecretStore() {
	}

	/**
	 * Reads the client secret of an account.
	 *
	 * @param account The account, see CloudAlmConfig#secretAccount
	 * @return The secret, or an empty string if none is stored
	 * @throws StorageException If the storage cannot be decrypted, e.g. the user cancelled the
	 *             master password prompt
	 * @throws IOException If Secure Storage is not available
	 */
	public static String read(String account) throws StorageException, IOException {
		return node().get(key(account), "");
	}

	/**
	 * Stores the client secret of an account, or removes it if the secret is empty, and writes the
	 * storage to disk.
	 *
	 * @param account The account, see CloudAlmConfig#secretAccount
	 * @param secret  The secret, or null or empty to remove it
	 * @throws StorageException If the secret cannot be encrypted
	 * @throws IOException If Secure Storage is not available or cannot be written
	 */
	public static void write(String account, String secret) throws StorageException, IOException {
		ISecurePreferences node = node();
		if (secret == null || secret.isEmpty()) {
			node.remove(key(account));
		} else {
			node.put(key(account), secret, true);
		}
		node.flush();
	}

	/** Keys must not contain a slash; a client ID may. */
	private static String key(String account) {
		return EncodingUtils.encodeSlashes(account);
	}

	private static ISecurePreferences node() throws IOException {
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		if (root == null) {
			throw new IOException("Eclipse Secure Storage is not available");
		}
		return root.node(NODE);
	}
}
