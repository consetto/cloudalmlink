package com.consetto.adt.cloudalmlink.model;

/**
 * Represents an OAuth 2.0 Bearer Token received from SAP authentication server.
 * Deserialized from JSON response via Gson.
 * Tracks token validity based on expiration time with a safety buffer.
 */
public class BearerToken {

	private String access_token;
	private String token_type;
	private String projectId;
	private String scope;
	private String jti;
	private String expires_in;
	private long expirationTime = 0;

	public void setAccessToken(String access_token) {
		this.access_token = access_token;
	}

	public void setTokenType(String token_type) {
		this.token_type = token_type;
	}

	public void setExpiresIn(String expires_in) {
		this.expires_in = expires_in;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public void setJti(String jti) {
		this.jti = jti;
	}

	public String getTokenType() {
		return token_type;
	}

	public String getToken() {
		return access_token;
	}

	/**
	 * Checks if the token is still valid.
	 *
	 * @return true if token has not expired (with 5 second buffer), false otherwise
	 */
	public boolean isValid() {
		return System.currentTimeMillis() < expirationTime - 5000;
	}

	/**
	 * Sets the expiration time based on current time plus the expires_in duration.
	 * Must be called after deserialization to enable validity checking.
	 */
	public void setExpirationTime() {
		this.expirationTime = System.currentTimeMillis() + Integer.parseInt(this.expires_in) * 1000L;
	}

	// Test helper: allows setting expiration time directly for testing
	void setExpirationTimeForTesting(long expirationTime) {
		this.expirationTime = expirationTime;
	}
}
