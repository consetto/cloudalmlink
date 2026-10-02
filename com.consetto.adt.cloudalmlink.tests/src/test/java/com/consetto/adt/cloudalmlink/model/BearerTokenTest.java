package com.consetto.adt.cloudalmlink.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

/**
 * Unit tests for {@link BearerToken}.
 * Tests OAuth token behavior including expiration logic with 5-second safety buffer.
 */
@DisplayName("BearerToken")
class BearerTokenTest {

	private final Gson gson = new Gson();

	private static BearerToken expiringAt(long expirationTime) {
		return new BearerToken("token", "Bearer", null, null, null, expirationTime);
	}

	@Nested
	@DisplayName("Token Properties")
	class TokenProperties {

		@Test
		@DisplayName("should store and retrieve access token")
		void shouldStoreAndRetrieveAccessToken() {
			BearerToken token = BearerToken.create("test-access-token-123", "Bearer", "3600", null, null);
			assertThat(token.getToken()).isEqualTo("test-access-token-123");
		}

		@Test
		@DisplayName("should store and retrieve token type")
		void shouldStoreAndRetrieveTokenType() {
			BearerToken token = BearerToken.create("token", "Bearer", "3600", null, null);
			assertThat(token.getTokenType()).isEqualTo("Bearer");
		}

		@Test
		@DisplayName("should handle null access token")
		void shouldHandleNullAccessToken() {
			BearerToken token = BearerToken.create(null, "Bearer", "3600", null, null);
			assertThat(token.getToken()).isNull();
		}
	}

	@Nested
	@DisplayName("Token Validity")
	class TokenValidity {

		@Test
		@DisplayName("should be valid when expiration is in the future")
		void shouldBeValidWhenExpirationInFuture() {
			assertThat(BearerToken.create("token", "Bearer", "3600", null, null).isValid()).isTrue();
		}

		@Test
		@DisplayName("should be invalid when expiration has passed")
		void shouldBeInvalidWhenExpirationPassed() {
			assertThat(expiringAt(System.currentTimeMillis() - 10_000).isValid()).isFalse();
		}

		@Test
		@DisplayName("should be invalid when within 5-second safety buffer")
		void shouldBeInvalidWithinSafetyBuffer() {
			assertThat(expiringAt(System.currentTimeMillis() + 3_000).isValid()).isFalse();
		}

		@Test
		@DisplayName("should be valid when just outside 5-second safety buffer")
		void shouldBeValidJustOutsideSafetyBuffer() {
			assertThat(expiringAt(System.currentTimeMillis() + 6_000).isValid()).isTrue();
		}

		@Test
		@DisplayName("should be invalid without an expiration time")
		void shouldBeInvalidWithDefaultExpiration() {
			assertThat(expiringAt(0).isValid()).isFalse();
		}
	}

	@Nested
	@DisplayName("Expiration Time Calculation")
	class ExpirationTimeCalculation {

		@Test
		@DisplayName("should calculate expiration time from expires_in seconds")
		void shouldCalculateExpirationFor1Hour() {
			long before = System.currentTimeMillis();
			BearerToken token = BearerToken.create("token", "Bearer", "3600", null, null);
			long after = System.currentTimeMillis();

			assertThat(token.expirationTime()).isBetween(before + 3_600_000, after + 3_600_000);
		}

		@Test
		@DisplayName("should stay valid for a short duration outside the buffer")
		void shouldCalculateExpirationForShortDuration() {
			assertThat(BearerToken.create("token", "Bearer", "10", null, null).isValid()).isTrue();
		}

		@Test
		@DisplayName("should be invalid when expires_in falls within the buffer")
		void shouldHandleVeryShortExpiration() {
			assertThat(BearerToken.create("token", "Bearer", "3", null, null).isValid()).isFalse();
		}

		@Test
		@DisplayName("should be invalid when expires_in is missing or not a number")
		void shouldBeInvalidForUnparseableExpiresIn() {
			assertThat(BearerToken.create("token", "Bearer", null, null, null).isValid()).isFalse();
			assertThat(BearerToken.create("token", "Bearer", "soon", null, null).isValid()).isFalse();
		}
	}

	@Nested
	@DisplayName("JSON Deserialization")
	class JsonDeserialization {

		@Test
		@DisplayName("should deserialize from JSON correctly")
		void shouldDeserializeFromJson() {
			String json = """
				{
					"access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9",
					"token_type": "Bearer",
					"expires_in": "3600",
					"scope": "calm-api.features.read",
					"jti": "abc123"
				}
				""";

			BearerToken token = gson.fromJson(json, BearerToken.class);

			assertThat(token.getToken()).isEqualTo("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
			assertThat(token.getTokenType()).isEqualTo("Bearer");
		}

		@Test
		@DisplayName("should not be valid until the expiration is calculated")
		void shouldHandleDeserializedTokenExpiration() {
			String json = """
				{
					"access_token": "token123",
					"token_type": "Bearer",
					"expires_in": 7200
				}
				""";

			BearerToken raw = gson.fromJson(json, BearerToken.class);
			assertThat(raw.isValid()).isFalse();
			assertThat(BearerToken.withCalculatedExpiration(raw).isValid()).isTrue();
		}

		@Test
		@DisplayName("should handle JSON with extra fields")
		void shouldHandleJsonWithExtraFields() {
			String json = """
				{
					"access_token": "token",
					"token_type": "Bearer",
					"expires_in": "3600",
					"projectId": "PRJ123",
					"unknown_field": "ignored"
				}
				""";

			assertThat(gson.fromJson(json, BearerToken.class).getToken()).isEqualTo("token");
		}
	}
}
