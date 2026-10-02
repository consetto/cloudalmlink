package com.consetto.adt.cloudalmlink.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds Cloud ALM IDs in ABAP source lines: features (6-NNNN), tasks/requirements (3-NNNN),
 * documents (7-NNNN) and libraries (15-NNNN).
 */
public final class CalmIds {

	/**
	 * An ID must stand on its own: the date 2023-06-15 must not yield "3-06", and 13-45 not "3-45".
	 */
	private static final Pattern CALM_ID = Pattern.compile("(?<![\\w-])(?:3|6|7|15)-\\d+(?![\\w-])");

	/**
	 * A Cloud ALM ID found in a line.
	 *
	 * @param id    The ID, e.g. "6-1234"
	 * @param start Offset of the first character within the line
	 * @param end   Offset after the last character within the line
	 */
	public record Match(String id, int start, int end) {
	}

	private CalmIds() {
	}

	/**
	 * Checks whether a string is exactly one Cloud ALM ID.
	 *
	 * @param id The string to check
	 * @return true for e.g. "6-1234", false for "6-", "16-1" or null
	 */
	public static boolean isCalmId(String id) {
		return id != null && CALM_ID.matcher(id).matches();
	}

	/**
	 * Finds all Cloud ALM IDs in a line, inside or outside comments.
	 *
	 * @param lineText The line
	 * @return The IDs in order of appearance, never null
	 */
	public static List<Match> find(String lineText) {
		List<Match> matches = new ArrayList<>();
		if (lineText == null) {
			return matches;
		}
		Matcher matcher = CALM_ID.matcher(lineText);
		while (matcher.find()) {
			matches.add(new Match(matcher.group(), matcher.start(), matcher.end()));
		}
		return matches;
	}

	/**
	 * Finds the Cloud ALM IDs that are inside an ABAP comment.
	 *
	 * @param lineText The line
	 * @return The IDs within comments in order of appearance, never null
	 */
	public static List<Match> findInComments(String lineText) {
		return find(lineText).stream()
				.filter(match -> isInComment(lineText, match.start()))
				.toList();
	}

	/**
	 * Checks whether a position in an ABAP line is inside a comment: a full-line comment starting
	 * with "*" or anything after a double quote.
	 *
	 * @param lineText   The line
	 * @param matchStart The position to check
	 * @return true if the position is within a comment
	 */
	public static boolean isInComment(String lineText, int matchStart) {
		if (lineText == null || matchStart < 0 || matchStart > lineText.length()) {
			return false;
		}
		return lineText.trim().startsWith("*") || lineText.substring(0, matchStart).contains("\"");
	}
}
