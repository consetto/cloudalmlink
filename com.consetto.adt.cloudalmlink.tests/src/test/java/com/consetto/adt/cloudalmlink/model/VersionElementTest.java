package com.consetto.adt.cloudalmlink.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link VersionElement}.
 * Tests version/transport data model behavior.
 */
@DisplayName("VersionElement")
class VersionElementTest {

	private VersionElement version;

	@BeforeEach
	void setUp() {
		version = new VersionElement();
	}

	@Nested
	@DisplayName("Basic Properties")
	class BasicProperties {

		@Test
		@DisplayName("should store and retrieve version ID")
		void shouldStoreAndRetrieveVersionId() {
			version.setID("00001");
			assertThat(version.getID()).isEqualTo("00001");
		}

		@Test
		@DisplayName("should store and retrieve transport ID")
		void shouldStoreAndRetrieveTransportId() {
			version.setTransport("NPLK900042");
			assertThat(version.getTransportId()).isEqualTo("NPLK900042");
		}

		@Test
		@DisplayName("should store and retrieve title")
		void shouldStoreAndRetrieveTitle() {
			version.setTitle("Fix critical bug in payment module");
			assertThat(version.getTitle()).isEqualTo("Fix critical bug in payment module");
		}

		@Test
		@DisplayName("should store and retrieve author")
		void shouldStoreAndRetrieveAuthor() {
			version.setAuthor("DEVELOPER1");
			assertThat(version.getAuthor()).isEqualTo("DEVELOPER1");
		}

		@Test
		@DisplayName("should store and retrieve last update")
		void shouldStoreAndRetrieveLastUpdate() {
			version.setLastUpdate("2024-01-15T10:30:00Z");
			assertThat(version.getLastUpdate()).isEqualTo("2024-01-15T10:30:00Z");
		}
	}

	@Nested
	@DisplayName("Feature Association")
	class FeatureAssociation {

		@Test
		@DisplayName("should store and retrieve associated feature")
		void shouldStoreAndRetrieveFeature() {
			FeatureElement feature = new FeatureElement();
			feature.setDisplayId("6-1234");
			feature.setTitle("Implement new payment flow");

			version.setFeature(feature);

			assertThat(version.getFeature()).isNotNull();
			assertThat(version.getFeature().getDisplayId()).isEqualTo("6-1234");
			assertThat(version.getFeature().getTitle()).isEqualTo("Implement new payment flow");
		}

		@Test
		@DisplayName("should handle null feature")
		void shouldHandleNullFeature() {
			version.setFeature(null);
			assertThat(version.getFeature()).isNull();
		}

		@Test
		@DisplayName("should allow replacing feature")
		void shouldAllowReplacingFeature() {
			FeatureElement feature1 = new FeatureElement();
			feature1.setDisplayId("6-1111");

			FeatureElement feature2 = new FeatureElement();
			feature2.setDisplayId("6-2222");

			version.setFeature(feature1);
			assertThat(version.getFeature().getDisplayId()).isEqualTo("6-1111");

			version.setFeature(feature2);
			assertThat(version.getFeature().getDisplayId()).isEqualTo("6-2222");
		}
	}

	@Nested
	@DisplayName("Null Handling")
	class NullHandling {

		@Test
		@DisplayName("should handle null values gracefully")
		void shouldHandleNullValues() {
			version.setID(null);
			version.setTransport(null);
			version.setTitle(null);
			version.setAuthor(null);
			version.setLastUpdate(null);

			assertThat(version.getID()).isNull();
			assertThat(version.getTransportId()).isNull();
			assertThat(version.getTitle()).isNull();
			assertThat(version.getAuthor()).isNull();
			assertThat(version.getLastUpdate()).isNull();
		}
	}

	@Nested
	@DisplayName("Common Transport ID Formats")
	class TransportIdFormats {

		@Test
		@DisplayName("should handle NPL transport format")
		void shouldHandleNplFormat() {
			version.setTransport("NPLK900001");
			assertThat(version.getTransportId()).isEqualTo("NPLK900001");
		}

		@Test
		@DisplayName("should handle DEV transport format")
		void shouldHandleDevFormat() {
			version.setTransport("DEVK912345");
			assertThat(version.getTransportId()).isEqualTo("DEVK912345");
		}

		@Test
		@DisplayName("should handle S4D transport format")
		void shouldHandleS4dFormat() {
			version.setTransport("S4DK911940");
			assertThat(version.getTransportId()).isEqualTo("S4DK911940");
		}
	}

	@Nested
	@DisplayName("Complete Version Setup")
	class CompleteVersionSetup {

		@Test
		@DisplayName("should correctly store complete version data")
		void shouldStoreCompleteVersionData() {
			FeatureElement feature = new FeatureElement();
			feature.setDisplayId("6-42");
			feature.setTitle("The Answer Feature");
			feature.setStatusCode("IN_PROGRESS");
			feature.setResponsibleId("DEEP_THOUGHT");

			version.setID("Active");
			version.setTransport("DEVK900042");
			version.setTitle("Current working version");
			version.setAuthor("DEVELOPER1");
			version.setLastUpdate("2024-01-20T14:00:00Z");
			version.setFeature(feature);

			assertThat(version.getID()).isEqualTo("Active");
			assertThat(version.getTransportId()).isEqualTo("DEVK900042");
			assertThat(version.getTitle()).isEqualTo("Current working version");
			assertThat(version.getAuthor()).isEqualTo("DEVELOPER1");
			assertThat(version.getLastUpdate()).isEqualTo("2024-01-20T14:00:00Z");
			assertThat(version.getFeature()).isNotNull();
			assertThat(version.getFeature().getDisplayId()).isEqualTo("6-42");
		}
	}
}
