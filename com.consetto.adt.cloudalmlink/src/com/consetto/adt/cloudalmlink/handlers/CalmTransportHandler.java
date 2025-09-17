package com.consetto.adt.cloudalmlink.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;
import com.sap.adt.tm.impl.Request;

public class CalmTransportHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		String baseUrl = scopedPreferenceStore.getString(PreferenceConstants.P_URL);

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		ITreeSelection selection = (ITreeSelection) window.getSelectionService().getSelection();

		if (selection.size() == 1) {
			Object selObject = selection.getFirstElement();

			if (selObject instanceof com.sap.adt.tm.impl.Request) {

				Request req = (Request) selObject;

			//	MessageDialog.openInformation(window.getShell(), "Cloud ALM Link for ADT", req.getDesc());

				String feature = getFeature(req.getDesc());
				
				String calmURL = baseUrl + "/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/"
					+ feature;
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(calmURL));
				} catch (PartInitException | MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}



//		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
//
//		try {
//			workbenchPage.showView("com.consetto.adt.cloudalmlink.views.TransportView");
//		} catch (PartInitException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		return null;

	}

	public String getFeature(String descr) {

		String[] descrSplit = descr.split(":");
		String feature = descrSplit[0];

		if (feature.matches("^6-\\d+$")) {
			return feature;
		}
		return null;

	}
}
