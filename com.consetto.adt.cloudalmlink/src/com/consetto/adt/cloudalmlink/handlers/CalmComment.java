package com.consetto.adt.cloudalmlink.handlers;

import java.net.URL;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;

import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.model.CloudAlmItemType;
import com.consetto.adt.cloudalmlink.services.PreferenceService;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;

/**
 * Represents a Cloud ALM reference found in source code comments.
 * Supports features (6-NNNN), tasks/requirements (3-NNNN), documents (7-NNNN), and libraries (15-NNNN).
 * Opens the corresponding Cloud ALM page when clicked.
 */
public class CalmComment implements IHyperlink {

	private final IRegion region;
	private final String itemId;

	public CalmComment(IRegion region, String itemId) {
		this.region = region;
		this.itemId = itemId;
	}

	@Override
	public IRegion getHyperlinkRegion() {
		return region;
	}

	@Override
	public String getTypeLabel() {
		return "Open in Cloud ALM";
	}

	@Override
	public String getHyperlinkText() {
		return "Open " + itemId + " in Cloud ALM";
	}

	@Override
	public void open() {
		CloudAlmConfig config = PreferenceService.getInstance().getCloudAlmConfig();
		String url = CloudAlmItemType.getUrlForItem(itemId, config);
		if (url == null) {
			CloudAlmLinkLogger.logWarning("Cannot open " + itemId + ": Cloud ALM tenant and region are not configured");
			return;
		}

		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (Exception e) {
			CloudAlmLinkLogger.logError("Failed to open browser for URL: " + url, e);
		}
	}
}
