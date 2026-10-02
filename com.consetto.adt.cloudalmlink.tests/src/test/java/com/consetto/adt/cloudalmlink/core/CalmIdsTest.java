package com.consetto.adt.cloudalmlink.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CalmIds}: IDs must stand on their own and be inside a comment.
 */
@DisplayName("CalmIds")
class CalmIdsTest {

	@ParameterizedTest
	@DisplayName("should find no ID inside dates or longer numbers")
	@ValueSource(strings = {
			"\" changed 2023-06-15",
			"\" see ticket 13-45",
			"\" version v1-6-12",
			"\" 6-12a",
			"\" 6-12-1" })
	void shouldNotMatchInsideLongerTokens(String line) {
		assertThat(CalmIds.find(line)).isEmpty();
	}

	@Test
	@DisplayName("should find IDs next to punctuation")
	void shouldMatchNextToPunctuation() {
		assertThat(CalmIds.find("* see (7-5678), 15-9012 and 3-144444."))
				.extracting(CalmIds.Match::id)
				.containsExactly("7-5678", "15-9012", "3-144444");
	}

	@Test
	@DisplayName("should report the position of each ID")
	void shouldReportPositions() {
		var match = CalmIds.find("\" fix 6-1234").get(0);

		assertThat(match.start()).isEqualTo(6);
		assertThat(match.end()).isEqualTo(12);
	}

	@Test
	@DisplayName("should keep only IDs after the comment quote")
	void shouldKeepOnlyIdsInComments() {
		String line = "lv_id = '6-111'. \" belongs to 6-222";

		assertThat(CalmIds.findInComments(line)).extracting(CalmIds.Match::id).containsExactly("6-222");
	}

	@Test
	@DisplayName("should treat the whole line as comment when it starts with *")
	void shouldTreatStarLineAsComment() {
		assertThat(CalmIds.findInComments("  * 6-1 and 3-2")).hasSize(2);
	}

	@Test
	@DisplayName("should not fail on positions outside the line")
	void shouldHandleOutOfRangePositions() {
		assertThat(CalmIds.isInComment("\" x", 99)).isFalse();
		assertThat(CalmIds.isInComment("\" x", -1)).isFalse();
	}
}
