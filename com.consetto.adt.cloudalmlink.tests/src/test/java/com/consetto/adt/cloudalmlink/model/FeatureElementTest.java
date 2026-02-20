package com.consetto.adt.cloudalmlink.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
		@DisplayName("should store and retrieve status code as display text")
		void shouldStoreAndRetrieveStatusCode() {
			feature.setStatusCode("IN_REALIZATION");
			assertThat(feature.getStatus()).isEqualTo("In Implementation");
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
	@DisplayName("Priority Display")
	class PriorityDisplay {

		@Test
		@DisplayName("should map priority 10 to Very High")
		void shouldMapPriority10ToVeryHigh() {
			feature.setPriorityCode(10);
			assertThat(feature.getPriority()).isEqualTo("Very High");
		}

		@Test
		@DisplayName("should map priority 20 to High")
		void shouldMapPriority20ToHigh() {
			feature.setPriorityCode(20);
			assertThat(feature.getPriority()).isEqualTo("High");
		}

		@Test
		@DisplayName("should map priority 30 to Medium")
		void shouldMapPriority30ToMedium() {
			feature.setPriorityCode(30);
			assertThat(feature.getPriority()).isEqualTo("Medium");
		}

		@Test
		@DisplayName("should map priority 40 to Low")
		void shouldMapPriority40ToLow() {
			feature.setPriorityCode(40);
			assertThat(feature.getPriority()).isEqualTo("Low");
		}

		@Test
		@DisplayName("should return empty string for unknown priority")
		void shouldReturnEmptyForUnknownPriority() {
			feature.setPriorityCode(99);
			assertThat(feature.getPriority()).isEmpty();
		}

		@Test
		@DisplayName("should return empty string for default priority (0)")
		void shouldReturnEmptyForDefaultPriority() {
			assertThat(feature.getPriority()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Modified Date")
	class ModifiedDate {

		@Test
		@DisplayName("should extract date from ISO timestamp")
		void shouldExtractDateFromIsoTimestamp() {
			feature.setModifiedAt("2025-06-15T09:30:00Z");
			assertThat(feature.getModifiedDate()).isEqualTo("2025-06-15");
		}

		@Test
		@DisplayName("should return empty string when modifiedAt is null")
		void shouldReturnEmptyWhenNull() {
			assertThat(feature.getModifiedDate()).isEmpty();
		}

		@Test
		@DisplayName("should return empty string when modifiedAt is too short")
		void shouldReturnEmptyWhenTooShort() {
			feature.setModifiedAt("2025");
			assertThat(feature.getModifiedDate()).isEmpty();
		}

		@Test
		@DisplayName("should handle date-only string")
		void shouldHandleDateOnlyString() {
			feature.setModifiedAt("2025-01-20");
			assertThat(feature.getModifiedDate()).isEqualTo("2025-01-20");
		}
	}

	@Nested
	@DisplayName("Expanded Entity Names")
	class ExpandedEntityNames {

		@Test
		@DisplayName("should return workstream name when set")
		void shouldReturnWorkstreamName() {
			FeatureElement.ExpandedEntity ws = new FeatureElement.ExpandedEntity();
			ws.setName("Cafeteria Services");
			feature.setToWorkstream(ws);
			assertThat(feature.getWorkstreamName()).isEqualTo("Cafeteria Services");
		}

		@Test
		@DisplayName("should return empty string when workstream is null")
		void shouldReturnEmptyWhenWorkstreamNull() {
			assertThat(feature.getWorkstreamName()).isEmpty();
		}

		@Test
		@DisplayName("should return scope name when set")
		void shouldReturnScopeName() {
			FeatureElement.ExpandedEntity scope = new FeatureElement.ExpandedEntity();
			scope.setName("Backend Fixes");
			feature.setToScope(scope);
			assertThat(feature.getScopeName()).isEqualTo("Backend Fixes");
		}

		@Test
		@DisplayName("should return empty string when scope is null")
		void shouldReturnEmptyWhenScopeNull() {
			assertThat(feature.getScopeName()).isEmpty();
		}

		@Test
		@DisplayName("should return release name when set")
		void shouldReturnReleaseName() {
			FeatureElement.ExpandedEntity release = new FeatureElement.ExpandedEntity();
			release.setName("2025-Q3");
			feature.setToRelease(release);
			assertThat(feature.getReleaseName()).isEqualTo("2025-Q3");
		}

		@Test
		@DisplayName("should return empty string when release is null")
		void shouldReturnEmptyWhenReleaseNull() {
			assertThat(feature.getReleaseName()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Requirement Title")
	class RequirementTitle {

		@Test
		@DisplayName("should return title when CALMREQU assignment exists")
		void shouldReturnTitleWhenCalmRequExists() {
			FeatureElement.TaskAssignment ta = new FeatureElement.TaskAssignment();
			ta.setType("CALMREQU");
			ta.setTitle("Must support decaf");
			feature.setToTaskAssignments(List.of(ta));

			assertThat(feature.getRequirementTitle()).isEqualTo("Must support decaf");
		}

		@Test
		@DisplayName("should return empty string when no CALMREQU exists")
		void shouldReturnEmptyWhenNoCalmRequ() {
			FeatureElement.TaskAssignment ta = new FeatureElement.TaskAssignment();
			ta.setType("CALMTASK");
			ta.setTitle("Some task");
			feature.setToTaskAssignments(List.of(ta));

			assertThat(feature.getRequirementTitle()).isEmpty();
		}

		@Test
		@DisplayName("should return empty string when toTaskAssignments is null")
		void shouldReturnEmptyWhenNull() {
			assertThat(feature.getRequirementTitle()).isEmpty();
		}

		@Test
		@DisplayName("should return first CALMREQU if multiple exist")
		void shouldReturnFirstCalmRequ() {
			FeatureElement.TaskAssignment ta1 = new FeatureElement.TaskAssignment();
			ta1.setType("CALMREQU");
			ta1.setTitle("First requirement");
			FeatureElement.TaskAssignment ta2 = new FeatureElement.TaskAssignment();
			ta2.setType("CALMREQU");
			ta2.setTitle("Second requirement");
			feature.setToTaskAssignments(List.of(ta1, ta2));

			assertThat(feature.getRequirementTitle()).isEqualTo("First requirement");
		}

		@Test
		@DisplayName("should skip non-CALMREQU and find CALMREQU")
		void shouldSkipNonCalmRequAndFindCalmRequ() {
			FeatureElement.TaskAssignment task = new FeatureElement.TaskAssignment();
			task.setType("CALMTASK");
			task.setTitle("A task");
			FeatureElement.TaskAssignment req = new FeatureElement.TaskAssignment();
			req.setType("CALMREQU");
			req.setTitle("The requirement");
			feature.setToTaskAssignments(List.of(task, req));

			assertThat(feature.getRequirementTitle()).isEqualTo("The requirement");
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
					"statusCode": "IN_REALIZATION",
					"projectId": "PRJ001",
					"responsibleId": "DEBUG_DUCK",
					"priorityCode": 2
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getUuid()).isEqualTo("abc123-def456");
			assertThat(deserializedFeature.getDisplayId()).isEqualTo("6-1337");
			assertThat(deserializedFeature.getTitle()).isEqualTo("Implement Rubber Duck Debugging");
			assertThat(deserializedFeature.getStatus()).isEqualTo("In Implementation");
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
					"statusCode": "CONFIRMED",
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
			assertThat(deserializedFeature.getStatus()).isEqualTo("Deployed");
			assertThat(deserializedFeature.getPriorityCode()).isEqualTo(1);
			assertThat(deserializedFeature.getType()).isEqualTo("USER_STORY");
			assertThat(deserializedFeature.getResponsibleId()).isEqualTo("BUG_WHISPERER");
			assertThat(deserializedFeature.getReleaseId()).isEqualTo("2024-Q1");
			assertThat(deserializedFeature.getWorkstreamId()).isEqualTo("WS-DEV");
		}

		@Test
		@DisplayName("should deserialize expanded entities from $expand response")
		void shouldDeserializeExpandedEntities() {
			String json = """
				{
					"uuid": "feature-uuid-002",
					"displayId": "6-5678",
					"title": "Feature with expanded entities",
					"statusCode": "IN_REALIZATION",
					"priorityCode": 20,
					"modifiedAt": "2025-06-15T09:30:00Z",
					"toWorkstream": {
						"uuid": "ws-uuid-001",
						"name": "Developer Productivity"
					},
					"toScope": {
						"uuid": "scope-uuid-001",
						"name": "New Features"
					},
					"toRelease": {
						"uuid": "rel-uuid-001",
						"name": "2025-Q3"
					}
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getDisplayId()).isEqualTo("6-5678");
			assertThat(deserializedFeature.getPriority()).isEqualTo("High");
			assertThat(deserializedFeature.getModifiedDate()).isEqualTo("2025-06-15");
			assertThat(deserializedFeature.getWorkstreamName()).isEqualTo("Developer Productivity");
			assertThat(deserializedFeature.getScopeName()).isEqualTo("New Features");
			assertThat(deserializedFeature.getReleaseName()).isEqualTo("2025-Q3");
		}

		@Test
		@DisplayName("should deserialize toTaskAssignments from JSON")
		void shouldDeserializeTaskAssignments() {
			String json = """
				{
					"displayId": "6-7777",
					"title": "Feature with requirements",
					"toTaskAssignments": [
						{
							"uuid": "ta-uuid-001",
							"parent_uuid": "feature-uuid-001",
							"title": "Implement login",
							"type": "CALMTASK"
						},
						{
							"uuid": "ta-uuid-002",
							"parent_uuid": "feature-uuid-001",
							"title": "Must support SSO",
							"type": "CALMREQU"
						}
					]
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getRequirementTitle()).isEqualTo("Must support SSO");
		}

		@Test
		@DisplayName("should handle JSON without expanded entities")
		void shouldHandleJsonWithoutExpandedEntities() {
			String json = """
				{
					"displayId": "6-9999",
					"title": "No expansions",
					"priorityCode": 30
				}
				""";

			FeatureElement deserializedFeature = gson.fromJson(json, FeatureElement.class);

			assertThat(deserializedFeature.getWorkstreamName()).isEmpty();
			assertThat(deserializedFeature.getScopeName()).isEmpty();
			assertThat(deserializedFeature.getReleaseName()).isEmpty();
			assertThat(deserializedFeature.getPriority()).isEqualTo("Medium");
		}
	}
}
