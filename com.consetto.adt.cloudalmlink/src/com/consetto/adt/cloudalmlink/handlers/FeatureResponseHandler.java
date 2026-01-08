package com.consetto.adt.cloudalmlink.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.google.gson.Gson;

/**
 * HTTP response handler for Cloud ALM feature API requests.
 * Deserializes JSON responses into FeatureElement objects and associates them with versions.
 */
public class FeatureResponseHandler implements HttpClientResponseHandler<String> {

	private VersionElement version;

	/**
	 * Creates a handler that will associate the retrieved feature with the specified version.
	 *
	 * @param version The version element to receive the feature data
	 */
	public FeatureResponseHandler(VersionElement version) {
		this.version = version;
	}

	@Override
	public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
		int statusCode = response.getCode();

		if (statusCode != 200) {
			return null;
		}

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			String content = new String(entity.getContent().readAllBytes(), StandardCharsets.UTF_8);

			// Deserialize JSON response to FeatureElement and associate with version
			Gson gson = new Gson();
			FeatureElement feature = gson.fromJson(content, FeatureElement.class);
			version.setFeature(feature);
		}

		return null;
	}
}
