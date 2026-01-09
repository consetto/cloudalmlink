package com.consetto.adt.cloudalmlink.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;
import com.sap.adt.tm.impl.Request;

/**
 * Command handler for opening a transport request's associated feature in Cloud ALM.
 * Fetches the feature via Cloud ALM API or falls back to description parsing.
 */
public class CalmTransportHandler extends AbstractHandler {

	private CalmApiHandler calmApiHandler = new CalmApiHandler();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		String region = scopedPreferenceStore.getString(PreferenceConstants.P_REG);
		String tenant = scopedPreferenceStore.getString(PreferenceConstants.P_TEN);

		// Build Cloud ALM base URL from tenant and region preferences
		String baseUrl = "https://" + tenant + "." + region + ".alm.cloud.sap";

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ITreeSelection selection = (ITreeSelection) window.getSelectionService().getSelection();

		if (selection.size() == 1) {
			Object selObject = selection.getFirstElement();

			if (selObject instanceof com.sap.adt.tm.impl.Request) {
				Request req = (Request) selObject;
				String transportId = req.getNumber();
				String feature = getFeatureFromApi(transportId);

				// Fall back to description parsing if API lookup fails
				if (feature == null) {
					feature = getFeatureFromDescription(req.getDesc());
				}

				if (feature != null) {
					// Construct Cloud ALM feature URL and open in external browser
					String calmURL = baseUrl + "/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/"
							+ feature;
					try {
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(calmURL));
					} catch (PartInitException | MalformedURLException e) {
						// Browser could not be opened - fail silently
					}
				} else {
					MessageDialog.openInformation(window.getShell(), "Cloud ALM Link",
							"No feature found for transport " + transportId);
				}
			}
		}

		return null;
	}

	/**
	 * Fetches the Cloud ALM feature ID via API for the given transport.
	 *
	 * @param transportId The transport request ID (e.g., "S4DK911940")
	 * @return The feature displayId if found, null otherwise
	 */
	private String getFeatureFromApi(String transportId) {
		if (transportId == null || transportId.isEmpty()) {
			return null;
		}

		VersionElement version = new VersionElement();
		calmApiHandler.getFeature(transportId, version);

		if (version.getFeature() != null) {
			return version.getFeature().getDisplayId();
		}
		return null;
	}

	/**
	 * Extracts the Cloud ALM feature ID from a transport description.
	 * Feature IDs follow the pattern "6-NNNN" (e.g., "6-123").
	 * (Fallback method if API lookup fails.)
	 * @param descr The transport request description
	 * @return The feature ID if found and valid, null otherwise
	 */
	private String getFeatureFromDescription(String descr) {
		if (descr == null || descr.isEmpty()) {
			return null;
		}

		String[] descrSplit = descr.split(":");
		String feature = descrSplit[0];

		if (feature.matches("^6-\\d+$")) {
			return feature;
		}
		return null;
	}
}
