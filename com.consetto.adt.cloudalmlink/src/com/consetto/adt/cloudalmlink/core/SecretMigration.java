package com.consetto.adt.cloudalmlink.core;

/**
 * Moves a secret from a plain-text store into an encrypted one.
 * Free of Eclipse dependencies so that it is unit-tested directly (see the tests module).
 */
public final class SecretMigration {

	/** Where the secret is stored encrypted. */
	public interface SecureTarget {

		/**
		 * Stores the secret.
		 *
		 * @param secret The secret, never blank
		 * @throws Exception If it could not be stored; the plain value is then kept
		 */
		void store(String secret) throws Exception;
	}

	/** Where the secret was stored in plain text. */
	public interface PlainSource {

		/** @return The plain secret, or null or blank if there is none */
		String read();

		/** Removes the plain secret. */
		void clear();
	}

	private SecretMigration() {
	}

	/**
	 * Moves the plain secret, if there is one, into the secure target. The plain value is removed
	 * only after the secure target stored it, so a failure never loses the secret. A plain value
	 * wins over one already in the target: only an older plugin version writes it, so it is the
	 * newer one.
	 *
	 * @param source The plain-text store
	 * @param target The encrypted store
	 * @return true if a secret was moved
	 * @throws Exception If the target could not store it
	 */
	public static boolean migrate(PlainSource source, SecureTarget target) throws Exception {
		String plain = source.read();
		if (plain == null || plain.isBlank()) {
			return false;
		}
		target.store(plain);
		source.clear();
		return true;
	}
}
