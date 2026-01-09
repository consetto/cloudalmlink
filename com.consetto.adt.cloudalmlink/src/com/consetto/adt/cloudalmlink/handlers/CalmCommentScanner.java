package com.consetto.adt.cloudalmlink.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

/**
 * Scans source code for Cloud ALM IDs.
 * Supports features (6-NNNN) and tasks/requirements (3-NNNN).
 * Creates clickable links that open the item in Cloud ALM.
 */
public class CalmCommentScanner implements IHyperlinkDetector {

	// Matches both features (6-NNNN) and tasks/requirements (3-NNNN)
	private static final Pattern CALM_ID_PATTERN = Pattern.compile("[36]-\\d+");

	@Override
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (textViewer == null || region == null) {
			return null;
		}

		IDocument document = textViewer.getDocument();
		if (document == null) {
			return null;
		}

		try {
			// Get the current line
			int lineNumber = document.getLineOfOffset(region.getOffset());
			int lineOffset = document.getLineOffset(lineNumber);
			int lineLength = document.getLineLength(lineNumber);
			String lineText = document.get(lineOffset, lineLength);

			// Find all Cloud ALM ID patterns in the line
			List<IHyperlink> links = new ArrayList<>();
			Matcher matcher = CALM_ID_PATTERN.matcher(lineText);

			while (matcher.find()) {
				int start = lineOffset + matcher.start();
				int end = lineOffset + matcher.end();

				// Check if cursor is within this match
				if (region.getOffset() >= start && region.getOffset() <= end) {
					// Only detect if ID is in a comment (after " or on a line starting with *)
					String textBeforeMatch = lineText.substring(0, matcher.start());
					String trimmedLine = lineText.trim();
					boolean isInComment = trimmedLine.startsWith("*") || textBeforeMatch.contains("\"");

					if (isInComment) {
						String itemId = matcher.group();
						IRegion linkRegion = new Region(start, end - start);
						links.add(new CalmComment(linkRegion, itemId));
					}
				}
			}

			return links.isEmpty() ? null : links.toArray(new IHyperlink[0]);
		} catch (Exception e) {
			return null;
		}
	}
}
