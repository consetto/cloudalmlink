package com.consetto.adt.cloudalmlink.handlers;

import java.net.URL;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;

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
		ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		String calmRegion = store.getString(PreferenceConstants.P_REG);
		String tenant = store.getString(PreferenceConstants.P_TEN);

		String baseUrl = "https://" + tenant + "." + calmRegion + ".alm.cloud.sap";
		String url;

		if (itemId.startsWith("6-")) {
			// Feature
			url = baseUrl + "/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/" + itemId;
		} else if (itemId.startsWith("3-")) {
			// Task/Requirement
			url = baseUrl + "/launchpad#task-management?sap-app-origin-hint=&/taskDetail/" + itemId;
		} else if (itemId.startsWith("7-")) {
			// Document
			url = baseUrl + "/launchpad#DocumentationObject-manage?sap-ui-app-id-hint=com.sap.calm.imp.sd.docu.ui&/Documents('" + itemId + "')";
		} else if (itemId.startsWith("15-")) {
			// Library
			url = baseUrl + "/launchpad#library-management?sap-ui-app-id-hint=com.sap.calm.imp.lib.ui&/LibraryElement('" + itemId + "')";
		} else {
			return;
		}

		try {
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
		} catch (Exception e) {
			// Browser could not be opened - fail silently
		}
	}
}
