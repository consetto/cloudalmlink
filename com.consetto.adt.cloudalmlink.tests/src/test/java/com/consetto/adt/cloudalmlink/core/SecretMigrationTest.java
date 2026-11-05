package com.consetto.adt.cloudalmlink.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SecretMigration}: moving the client secret out of the plain preferences.
 */
@DisplayName("SecretMigration")
class SecretMigrationTest {

	/** A plain store holding one value. */
	private static final class Plain implements SecretMigration.PlainSource {
		String value;

		Plain(String value) {
			this.value = value;
		}

		@Override
		public String read() {
			return value;
		}

		@Override
		public void clear() {
			value = "";
		}
	}

	/** A secure store that remembers what it got. */
	private static final class Secure implements SecretMigration.SecureTarget {
		String value;

		@Override
		public void store(String secret) {
			value = secret;
		}
	}

	@Test
	@DisplayName("should move a plain secret and remove it from the plain store")
	void shouldMovePlainSecret() throws Exception {
		Plain plain = new Plain("s3cr3t");
		Secure secure = new Secure();

		assertThat(SecretMigration.migrate(plain, secure)).isTrue();
		assertThat(secure.value).isEqualTo("s3cr3t");
		assertThat(plain.value).isEmpty();
	}

	@Test
	@DisplayName("should do nothing when there is no plain secret")
	void shouldIgnoreMissingSecret() throws Exception {
		Secure secure = new Secure();
		secure.value = "existing";

		assertThat(SecretMigration.migrate(new Plain(null), secure)).isFalse();
		assertThat(SecretMigration.migrate(new Plain("  "), secure)).isFalse();
		assertThat(secure.value).isEqualTo("existing");
	}

	@Test
	@DisplayName("should replace a secret already in the secure store with the plain one")
	void shouldPreferPlainSecret() throws Exception {
		Secure secure = new Secure();
		secure.value = "older";

		SecretMigration.migrate(new Plain("newer"), secure);

		assertThat(secure.value).isEqualTo("newer");
	}

	@Test
	@DisplayName("should keep the plain secret when the secure store fails")
	void shouldKeepPlainSecretOnFailure() {
		Plain plain = new Plain("s3cr3t");

		assertThatThrownBy(() -> SecretMigration.migrate(plain, secret -> {
			throw new IOException("storage locked");
		})).isInstanceOf(IOException.class);
		assertThat(plain.value).isEqualTo("s3cr3t");
	}
}
