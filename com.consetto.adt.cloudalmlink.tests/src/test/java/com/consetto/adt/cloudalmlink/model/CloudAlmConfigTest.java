package com.consetto.adt.cloudalmlink.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CloudAlmConfig}. Tenant and region become part of the host that receives
 * the client credentials, so anything but a single host-name label must be refused.
 */
@DisplayName("CloudAlmConfig")
class CloudAlmConfigTest {

	@ParameterizedTest
	@DisplayName("should accept real tenants and regions")
	@ValueSource(strings = { "mycompany", "eu10", "eu10-004", "a", "Tenant-1" })
	void shouldAcceptHostLabels(String value) {
		assertThat(CloudAlmConfig.isHostLabel(value)).isTrue();
	}

	@ParameterizedTest
	@DisplayName("should refuse values that change the host")
	@NullAndEmptySource
	@ValueSource(strings = { "evil.example#", "evil.example", "a/b", "user@evil", "-eu10", "eu10-", "eu 10", "eu10\n" })
	void shouldRefuseOtherValues(String value) {
		assertThat(CloudAlmConfig.isHostLabel(value)).isFalse();
	}

	@Test
	@DisplayName("should not be configured with a tenant that redirects the token request")
	void shouldNotBeValidWithInjectedTenant() {
		var config = new CloudAlmConfig("evil.example#", "eu10", "client", "secret");

		assertThat(config.isValid()).isFalse();
		assertThat(config.hasConnectionSettings()).isFalse();
	}

	@Test
	@DisplayName("should build the token URL on the SAP authentication domain")
	void shouldBuildTokenUrl() {
		var config = new CloudAlmConfig("mycompany", "eu10", "client", "secret");

		assertThat(config.isValid()).isTrue();
		assertThat(config.tokenUrl()).isEqualTo("https://mycompany.authentication.eu10.hana.ondemand.com/oauth/token");
		assertThat(config.apiUrl()).isEqualTo("https://mycompany.eu10.alm.cloud.sap/api/calm-features/v1");
	}
}
