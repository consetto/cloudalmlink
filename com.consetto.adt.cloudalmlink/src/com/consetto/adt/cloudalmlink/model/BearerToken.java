package com.consetto.adt.cloudalmlink.model;

/**
 * Immutable representation of an OAuth 2.0 Bearer Token received from SAP authentication server.
 * Uses Java 21 record for immutability and reduced boilerplate.
 *
 * Note: Field names use snake_case to match JSON response for Gson deserialization.
 */
public record BearerToken(
		String access_token,
		String token_type,
		String expires_in,
		String scope,
		String jti,
		long expirationTime
) {

	/** Buffer time (5 seconds) before actual expiration to ensure token validity */
	private static final long EXPIRATION_BUFFER_MS = 5000L;

	/**
	 * Compact constructor for validation.
	 */
	public BearerToken {
		// Allow null values from JSON deserialization, but expirationTime defaults to 0
	}

	/**
	 * Creates a BearerToken from Gson deserialization with calculated expiration time.
	 * This is the primary factory method to use after Gson creates the initial record.
	 *
	 * @param token The token deserialized by Gson (with expirationTime = 0)
	 * @return A new token with calculated expiration time
	 */
	public static BearerToken withCalculatedExpiration(BearerToken token) {
		if (token == null) {
			return null;
		}
		long expTime = calculateExpirationTime(token.expires_in());
		return new BearerToken(
				token.access_token(),
				token.token_type(),
				token.expires_in(),
				token.scope(),
				token.jti(),
				expTime
		);
	}

	/**
	 * Creates a BearerToken with all fields including pre-calculated expiration.
	 *
	 * @param accessToken The OAuth access token
	 * @param tokenType The token type (typically "Bearer")
	 * @param expiresIn Expiration duration in seconds
	 * @param scope The token scope
	 * @param jti JWT ID
	 * @return A new BearerToken with calculated expiration time
	 */
	public static BearerToken create(String accessToken, String tokenType, String expiresIn, String scope, String jti) {
		long expTime = calculateExpirationTime(expiresIn);
		return new BearerToken(accessToken, tokenType, expiresIn, scope, jti, expTime);
	}

	/**
	 * Calculates the expiration timestamp from the expires_in duration.
	 *
	 * @param expiresIn Expiration duration in seconds as string
	 * @return The expiration timestamp in milliseconds
	 */
	private static long calculateExpirationTime(String expiresIn) {
		if (expiresIn == null || expiresIn.isEmpty()) {
			return 0L;
		}
		try {
			return System.currentTimeMillis() + Long.parseLong(expiresIn) * 1000L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * Checks if the token is still valid.
	 *
	 * @return true if token has not expired (with 5 second buffer), false otherwise
	 */
	public boolean isValid() {
		return expirationTime > 0 && System.currentTimeMillis() < expirationTime - EXPIRATION_BUFFER_MS;
	}

	/**
	 * Gets the access token value.
	 *
	 * @return The OAuth access token
	 */
	public String getToken() {
		return access_token;
	}

	/**
	 * Gets the token type.
	 *
	 * @return The token type (typically "Bearer")
	 */
	public String getTokenType() {
		return token_type;
	}

	// Legacy support methods for backward compatibility

	/**
	 * @deprecated Use {@link #withCalculatedExpiration(BearerToken)} instead.
	 *             Creates a new token with calculated expiration time.
	 * @return A new BearerToken with expiration time set
	 */
	@Deprecated
	public BearerToken withExpirationTime() {
		return withCalculatedExpiration(this);
	}
}
