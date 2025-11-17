package com.consetto.adt.cloudalmlink.model;

import java.util.Date;

public class BearerToken {

	private String access_token;
	private String token_type;

	private String projectId;
	private String scope;
	private String jti;
	private String expires_in;
	private long expirationTime = 0;

	public void setaAcessToken(String access_token) {
		this.access_token = access_token;
	}

	public void setToken_Type(String token_type) {
		this.token_type = token_type;
	}

	public void setExpires_in(String expires_in) {
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

	public String getToken_Type() {
		return token_type;

	}

	public String getToken() {
		return access_token;
	}

	public boolean isValid() {
		// check if token is expired
		return new Date().getTime() < expirationTime - 5000; // 5 seconds buffer

	}

	public void setExpirationTime() {

		// set expiration time based on current time + expires_in
		this.expirationTime = (long) new Date().getTime() + Integer.parseInt(this.expires_in) * 1000;
	}

}
