package com.consetto.adt.cloudalmlink.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
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

	private BearerToken token;
	private Gson gson;

	@BeforeEach
	void setUp() {
		token = new BearerToken();
		gson = new Gson();
	}

	@Nested
	@DisplayName("Token Properties")
	class TokenProperties {

		@Test
		@DisplayName("should store and retrieve access token")
		void shouldStoreAndRetrieveAccessToken() {
			token.setAccessToken("test-access-token-123");
			assertThat(token.getToken()).isEqualTo("test-access-token-123");
		}

		@Test
		@DisplayName("should store and retrieve token type")
		void shouldStoreAndRetrieveTokenType() {
			token.setTokenType("Bearer");
			assertThat(token.getTokenType()).isEqualTo("Bearer");
		}

		@Test
		@DisplayName("should handle null access token")
		void shouldHandleNullAccessToken() {
			token.setAccessToken(null);
			assertThat(token.getToken()).isNull();
		}
	}

	@Nested
	@DisplayName("Token Validity")
	class TokenValidity {

		@Test
		@DisplayName("should be valid when expiration is in the future")
		void shouldBeValidWhenExpirationInFuture() {
			token.setExpiresIn("3600"); // 1 hour
			token.setExpirationTime();

			assertThat(token.isValid()).isTrue();
		}

		@Test
		@DisplayName("should be invalid when expiration has passed")
		void shouldBeInvalidWhenExpirationPassed() {
			// Set expiration time to 10 seconds ago
			token.setExpirationTimeForTesting(System.currentTimeMillis() - 10000);

			assertThat(token.isValid()).isFalse();
		}

		@Test
		@DisplayName("should be invalid when within 5-second safety buffer")
		void shouldBeInvalidWithinSafetyBuffer() {
			// Set expiration time to 3 seconds from now (within 5-second buffer)
			token.setExpirationTimeForTesting(System.currentTimeMillis() + 3000);

			assertThat(token.isValid()).isFalse();
		}

		@Test
		@DisplayName("should be valid when just outside 5-second safety buffer")
		void shouldBeValidJustOutsideSafetyBuffer() {
			// Set expiration time to 6 seconds from now (outside 5-second buffer)
			token.setExpirationTimeForTesting(System.currentTimeMillis() + 6000);

			assertThat(token.isValid()).isTrue();
		}

		@Test
		@DisplayName("should be invalid with default expiration time of 0")
		void shouldBeInvalidWithDefaultExpiration() {
			// Default expiration time is 0
			assertThat(token.isValid()).isFalse();
		}
	}

	@Nested
	@DisplayName("Expiration Time Calculation")
	class ExpirationTimeCalculation {

		@Test
		@DisplayName("should calculate expiration time correctly for 1 hour")
		void shouldCalculateExpirationFor1Hour() {
			long beforeTime = System.currentTimeMillis();
			token.setExpiresIn("3600");
			token.setExpirationTime();
			long afterTime = System.currentTimeMillis();

			// Token should be valid for close to 1 hour (minus 5 second buffer)
			assertThat(token.isValid()).isTrue();

			// Check that 3595 seconds from now it's still valid
			token.setExpirationTimeForTesting(afterTime + 3600_000);
			assertThat(token.isValid()).isTrue();
		}

		@Test
		@DisplayName("should calculate expiration time correctly for short duration")
		void shouldCalculateExpirationForShortDuration() {
			token.setExpiresIn("10"); // 10 seconds
			token.setExpirationTime();

			// With only 10 seconds and 5-second buffer, should still be valid (5 seconds left)
			assertThat(token.isValid()).isTrue();
		}

		@Test
		@DisplayName("should handle very short expiration that falls within buffer")
		void shouldHandleVeryShortExpiration() {
			token.setExpiresIn("3"); // 3 seconds
			token.setExpirationTime();

			// 3 seconds is within the 5-second buffer, so should be invalid immediately
			assertThat(token.isValid()).isFalse();
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

			BearerToken deserializedToken = gson.fromJson(json, BearerToken.class);

			assertThat(deserializedToken.getToken()).isEqualTo("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9");
			assertThat(deserializedToken.getTokenType()).isEqualTo("Bearer");
		}

		@Test
		@DisplayName("should handle deserialized token expiration")
		void shouldHandleDeserializedTokenExpiration() {
			String json = """
				{
					"access_token": "token123",
					"token_type": "Bearer",
					"expires_in": "7200"
				}
				""";

			BearerToken deserializedToken = gson.fromJson(json, BearerToken.class);
			deserializedToken.setExpirationTime();

			assertThat(deserializedToken.isValid()).isTrue();
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

			BearerToken deserializedToken = gson.fromJson(json, BearerToken.class);

			assertThat(deserializedToken.getToken()).isEqualTo("token");
		}
	}
}
