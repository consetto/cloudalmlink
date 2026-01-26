package com.consetto.adt.cloudalmlink.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

/**
 * Unit tests for {@link FeatureElement}.
 * Tests Cloud ALM feature data model and JSON deserialization.
 */
@DisplayName("FeatureElement")
class FeatureElementTest {

	private FeatureElement feature;
	private Gson gson;

	@BeforeEach
	void setUp() {
		feature = new FeatureElement();
		gson = new Gson();
	}

	@Nested
	@DisplayName("Basic Properties")
	class BasicProperties {

		@Test
		@DisplayName("should store and retrieve UUID")
		void shouldStoreAndRetrieveUuid() {
			feature.setUuid("550e8400-e29b-41d4-a716-446655440000");
			assertThat(feature.getUuid()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
		}

		@Test
		@DisplayName("should store and retrieve display ID")
		void shouldStoreAndRetrieveDisplayId() {
			feature.setDisplayId("6-1234");
			assertThat(feature.getDisplayId()).isEqualTo("6-1234");
		}

		@Test
		@DisplayName("should store and retrieve title")
		void shouldStoreAndRetrieveTitle() {
			feature.setTitle("Implement payment gateway integration");
			assertThat(feature.getTitle()).isEqualTo("Implement payment gateway integration");
		}

		@Test
		@DisplayName("should store and retrieve status code")
		void shouldStoreAndRetrieveStatusCode() {
			feature.setStatusCode("IN_PROGRESS");
			assertThat(feature.getStatus()).isEqualTo("IN_PROGRESS");
		}

		@Test
		@DisplayName("should store and retrieve project ID")
		void shouldStoreAndRetrieveProjectId() {
			feature.setProjectId("PRJ-2024-001");
			assertThat(feature.getProjectId()).isEqualTo("PRJ-2024-001");
		}

		@Test
		@DisplayName("should store and retrieve responsible ID")
		void shouldStoreAndRetrieveResponsibleId() {
			feature.setResponsibleId("USER123");
			assertThat(feature.getResponsibleId()).isEqualTo("USER123");
		}
	}

	@Nested
	@DisplayName("Extended Properties")
	class ExtendedProperties {

		@Test
		@DisplayName("should store and retrieve description")
		void shouldStoreAndRetrieveDescription() {
			feature.setDescription("Detailed description of the feature");
			assertThat(feature.getDescription()).isEqualTo("Detailed description of the feature");
		}

		@Test
		@DisplayName("should store and retrieve modified at timestamp")
		void shouldStoreAndRetrieveModifiedAt() {
			feature.setModifiedAt("2024-01-15T10:30:00Z");
			assertThat(feature.getModifiedAt()).isEqualTo("2024-01-15T10:30:00Z");
		}

		@Test
		@DisplayName("should store and retrieve scope ID")
		void shouldStoreAndRetrieveScopeId() {
			feature.setScopeId("SCOPE-001");
			assertThat(feature.getScopeId()).isEqualTo("SCOPE-001");
		}

		@Test
		@DisplayName("should store and retrieve priority code")
		void shouldStoreAndRetrievePriorityCode() {
			feature.setPriorityCode(1);
			assertThat(feature.getPriorityCode()).isEqualTo(1);
		}

		@Test
		@DisplayName("should store and retrieve type")
		void shouldStoreAndRetrieveType() {
			feature.setType("EPIC");
			assertThat(feature.getType()).isEqualTo("EPIC");
		}

		@Test
		@DisplayName("should store and retrieve release ID")
		void shouldStoreAndRetrieveReleaseId() {
			feature.setReleaseId("REL-2024-Q1");
			assertThat(feature.getReleaseId()).isEqualTo("REL-2024-Q1");
		}

		@Test
		@DisplayName("should store and retrieve workstream ID")
		void shouldStoreAndRetrieveWorkstreamId() {
			feature.setWorkstreamId("WS-PAYMENTS");
			assertThat(feature.getWorkstreamId()).isEqualTo("WS-PAYMENTS");
		}
	}

	@Nested
	@DisplayName("Status Codes")
	class StatusCodes {

		@Test
		@DisplayName("should handle IN_PROGRESS status")
		void shouldHandleInProgressStatus() {
			feature.setStatusCode("IN_PROGRESS");
			assertThat(feature.getStatus()).isEqualTo("IN_PROGRESS");
		}

		@Test
		@DisplayName("should handle RELEASED status")
		void shouldHandleReleasedStatus() {
			feature.setStatusCode("RELEASED");
			assertThat(feature.getStatus()).isEqualTo("RELEASED");
		}

		@Test
		@DisplayName("should handle COMPLETED status")
		void shouldHandleCompletedStatus() {
			feature.setStatusCode("COMPLETED");
			assertThat(feature.getStatus()).isEqualTo("COMPLETED");
		}

		@Test
		@DisplayName("should handle IN_REVIEW status")
		void shouldHandleInReviewStatus() {
			feature.setStatusCode("IN_REVIEW");
			assertThat(feature.getStatus()).isEqualTo("IN_REVIEW");
		}
	}

	@Nested
	@DisplayName("Display ID Formats")
	class DisplayIdFormats {

		@Test
		@DisplayName("should handle feature ID format (6-NNNN)")
		void shouldHandleFeatureIdFormat() {
			feature.setDisplayId("6-1234");
			assertThat(feature.getDisplayId()).isEqualTo("6-1234");
		}

		@Test
		@DisplayName("should handle task ID format (3-NNNN)")
		void shouldHandleTaskIdFormat() {
			feature.setDisplayId("3-5678");
			assertThat(feature.getDisplayId()).isEqualTo("3-5678");
		}

		@Test
		@DisplayName("should handle document ID format (7-NNNN)")
		void shouldHandleDocumentIdFormat() {
			feature.setDisplayId("7-9012");
			assertThat(feature.getDisplayId()).isEqualTo("7-9012");
		}

		@Test
		@DisplayName("should handle library ID format (15-NNNN)")
		void shouldHandleLibraryIdFormat() {
			feature.setDisplayId("15-3456");
			assertThat(feature.getDisplayId()).isEqualTo("15-3456");
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
					"uuid": "abc123-def456",
					"displayId": "6-1337",
					"title": "Implement Rubber Duck Debugging",
					"statusCode": "IN_PROGRESS",
					"projectId": "PRJ001",
					"responsibleId": "DEBUG_DUCK",
					"priorityCode": 2
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getUuid()).isEqualTo("abc123-def456");
			assertThat(deserializedFeature.getDisplayId()).isEqualTo("6-1337");
			assertThat(deserializedFeature.getTitle()).isEqualTo("Implement Rubber Duck Debugging");
			assertThat(deserializedFeature.getStatus()).isEqualTo("IN_PROGRESS");
			assertThat(deserializedFeature.getProjectId()).isEqualTo("PRJ001");
			assertThat(deserializedFeature.getResponsibleId()).isEqualTo("DEBUG_DUCK");
			assertThat(deserializedFeature.getPriorityCode()).isEqualTo(2);
		}

		@Test
		@DisplayName("should handle partial JSON")
		void shouldHandlePartialJson() {
			String json = """
				{
					"displayId": "6-42",
					"title": "The Answer"
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getDisplayId()).isEqualTo("6-42");
			assertThat(deserializedFeature.getTitle()).isEqualTo("The Answer");
			assertThat(deserializedFeature.getStatus()).isNull();
			assertThat(deserializedFeature.getPriorityCode()).isEqualTo(0); // Default int value
		}

		@Test
		@DisplayName("should handle complete API response JSON")
		void shouldHandleCompleteApiResponseJson() {
			String json = """
				{
					"uuid": "feature-uuid-001",
					"displayId": "6-404",
					"title": "Feature Not Found - But We Fixed It",
					"projectId": "SAP-PROJECT",
					"description": "This feature fixes a critical issue",
					"modifiedAt": "2024-01-20T15:30:00Z",
					"scopeId": "SCOPE-MAIN",
					"statusCode": "COMPLETED",
					"priorityCode": 1,
					"type": "USER_STORY",
					"responsibleId": "BUG_WHISPERER",
					"releaseId": "2024-Q1",
					"workstreamId": "WS-DEV"
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getUuid()).isEqualTo("feature-uuid-001");
			assertThat(deserializedFeature.getDisplayId()).isEqualTo("6-404");
			assertThat(deserializedFeature.getTitle()).isEqualTo("Feature Not Found - But We Fixed It");
			assertThat(deserializedFeature.getProjectId()).isEqualTo("SAP-PROJECT");
			assertThat(deserializedFeature.getDescription()).isEqualTo("This feature fixes a critical issue");
			assertThat(deserializedFeature.getModifiedAt()).isEqualTo("2024-01-20T15:30:00Z");
			assertThat(deserializedFeature.getScopeId()).isEqualTo("SCOPE-MAIN");
			assertThat(deserializedFeature.getStatus()).isEqualTo("COMPLETED");
			assertThat(deserializedFeature.getPriorityCode()).isEqualTo(1);
			assertThat(deserializedFeature.getType()).isEqualTo("USER_STORY");
			assertThat(deserializedFeature.getResponsibleId()).isEqualTo("BUG_WHISPERER");
			assertThat(deserializedFeature.getReleaseId()).isEqualTo("2024-Q1");
			assertThat(deserializedFeature.getWorkstreamId()).isEqualTo("WS-DEV");
		}
	}
}
