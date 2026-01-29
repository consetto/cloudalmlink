package com.consetto.adt.cloudalmlink.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.model.FeatureElement;
import com.consetto.adt.cloudalmlink.services.ICloudAlmApiService;
import com.consetto.adt.cloudalmlink.services.PreferenceService;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;
import com.sap.adt.tm.impl.Request;

/**
 * Command handler for opening a transport request's associated feature in Cloud ALM.
 * Fetches the feature via Cloud ALM API or falls back to description parsing.
 */
public class CalmTransportHandler extends AbstractHandler {

	private ICloudAlmApiService apiService;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get configuration using service layer
		CloudAlmConfig config = PreferenceService.getInstance().getCloudAlmConfig();

		if (!config.hasConnectionSettings()) {
			IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
			MessageDialog.openWarning(window.getShell(), "Cloud ALM Link",
					"Cloud ALM connection is not configured. Please configure tenant and region in preferences.");
			return null;
		}

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ITreeSelection selection = (ITreeSelection) window.getSelectionService().getSelection();

		if (selection.size() == 1) {
			Object selObject = selection.getFirstElement();

			if (selObject instanceof Request req) {
				String transportId = req.getNumber();
				String featureId = getFeatureFromApi(transportId);

				// Fall back to description parsing if API lookup fails
				if (featureId == null) {
					featureId = getFeatureFromDescription(req.getDesc());
				}

				if (featureId != null) {
					// Use centralized URL building from CloudAlmConfig
					String calmURL = config.featureUrl(featureId);
					try {
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(calmURL));
					} catch (PartInitException | MalformedURLException e) {
						CloudAlmLinkLogger.logError("Failed to open browser for URL: " + calmURL, e);
						MessageDialog.openError(window.getShell(), "Cloud ALM Link",
								"Could not open browser. Please check the Error Log for details.");
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
	 * Gets or creates the API service instance.
	 *
	 * @return The Cloud ALM API service
	 */
	private ICloudAlmApiService getApiService() {
		if (apiService == null) {
			apiService = new CalmApiHandler();
		}
		return apiService;
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

		FeatureElement feature = getApiService().getFeature(transportId);
		if (feature != null) {
			return feature.getDisplayId();
		}
		return null;
	}

	/**
	 * Extracts the Cloud ALM feature ID from a transport description.
	 * Feature IDs follow the pattern "6-NNNN" (e.g., "6-123").
	 * (Fallback method if API lookup fails.)
	 *
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

	@Override
	public void dispose() {
		if (apiService != null) {
			apiService.close();
			apiService = null;
		}
		super.dispose();
	}
}
