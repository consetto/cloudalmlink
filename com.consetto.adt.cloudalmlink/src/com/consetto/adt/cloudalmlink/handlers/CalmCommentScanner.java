package com.consetto.adt.cloudalmlink.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import com.consetto.adt.cloudalmlink.core.CalmIds;

/**
 * Scans source code for Cloud ALM IDs.
 * Supports features (6-NNNN), tasks/requirements (3-NNNN), documents (7-NNNN), and libraries (15-NNNN).
 * Creates clickable links that open the item in Cloud ALM.
 */
public class CalmCommentScanner implements IHyperlinkDetector {

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

			// Link the Cloud ALM ID under the cursor if it is inside a comment
			List<IHyperlink> links = new ArrayList<>();
			for (CalmIds.Match match : CalmIds.findInComments(lineText)) {
				int start = lineOffset + match.start();
				int end = lineOffset + match.end();
				if (region.getOffset() >= start && region.getOffset() <= end) {
					links.add(new CalmComment(new Region(start, end - start), match.id()));
				}
			}

			return links.isEmpty() ? null : links.toArray(new IHyperlink[0]);
		} catch (Exception e) {
			return null;
		}
	}
}
