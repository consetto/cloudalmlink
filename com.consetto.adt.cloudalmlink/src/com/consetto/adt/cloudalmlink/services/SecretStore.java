package com.consetto.adt.cloudalmlink.services;

import java.io.IOException;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import com.consetto.adt.cloudalmlink.CloudAlmLinkConstants;

/**
 * Keeps the Cloud ALM client secret encrypted in Eclipse Secure Storage. On macOS and Windows
 * Eclipse protects the storage with the system keychain; elsewhere it may ask for a master
 * password.
 */
public final class SecretStore {

	private static final String NODE = CloudAlmLinkConstants.PLUGIN_ID;
	private static final String KEY = "clientSecret";

	private SecretStore() {
	}

	/**
	 * Reads the client secret.
	 *
	 * @return The secret, or an empty string if none is stored
	 * @throws StorageException If the storage cannot be decrypted, e.g. the user cancelled the
	 *             master password prompt
	 */
	public static String read() throws StorageException {
		ISecurePreferences node = node();
		return node == null ? "" : node.get(KEY, "");
	}

	/**
	 * Stores the client secret, or removes it if the secret is empty, and writes the storage to disk.
	 *
	 * @param secret The secret, or null or empty to remove it
	 * @throws StorageException If the secret cannot be encrypted
	 * @throws IOException If the storage cannot be written
	 */
	public static void write(String secret) throws StorageException, IOException {
		ISecurePreferences node = node();
		if (node == null) {
			throw new IOException("Eclipse Secure Storage is not available");
		}
		if (secret == null || secret.isEmpty()) {
			node.remove(KEY);
		} else {
			node.put(KEY, secret, true);
		}
		node.flush();
	}

	private static ISecurePreferences node() {
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		return root == null ? null : root.node(NODE);
	}
}
