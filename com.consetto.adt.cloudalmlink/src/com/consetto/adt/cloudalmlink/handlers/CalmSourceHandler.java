package com.consetto.adt.cloudalmlink.handlers;

import java.net.URI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.consetto.adt.cloudalmlink.model.VersionData;
import com.consetto.adt.cloudalmlink.views.TransportView;
import com.sap.adt.communication.message.HeadersFactory;
import com.sap.adt.communication.message.IHeaders;
import com.sap.adt.communication.message.IHeaders.IField;
import com.sap.adt.communication.resources.AdtRestResourceFactory;
import com.sap.adt.communication.resources.IRestResource;
import com.sap.adt.communication.resources.IRestResourceFactory;
import com.sap.adt.destinations.ui.logon.AdtLogonServiceUIFactory;
import com.sap.adt.tools.abapsource.ui.sources.editors.AbapSourcePage;
import com.sap.adt.tools.core.model.atom.IAtomLink;
import com.sap.adt.tools.core.project.IAbapProject;
import com.sap.adt.tools.core.ui.editors.IAdtFormEditor;

/**
 * Command handler for displaying transports and features associated with ABAP source code.
 * Retrieves version data from ADT and displays results in the TransportView.
 */
public class CalmSourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// Get the active ADT editor and extract source information
		IAdtFormEditor editor = (IAdtFormEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		// Get ADT URL to determine the source object path
		String adtUrl = editor.getModelFile().getRawLocationURI().toString();
		IProject project = editor.getModelFile().getProject();
		IAbapProject abapProject = (IAbapProject) project.getAdapter(IAbapProject.class);

		// Extract file path from ADT URL
		String fileName = adtUrl.substring(adtUrl.indexOf(".") + 1, adtUrl.lastIndexOf("/") + 1);

		EList<IAtomLink> links = editor.getModel().getLinks();
		String type = editor.getModel().getType();

		// Find the versions URL from available links
		String versionsURL = null;
		for (IAtomLink link : links) {
			if (link.getHref().contains("source/main/versions")
					|| link.getHref().contains("implementations/versions")) {
				versionsURL = link.getHref();
				break;
			}
		}

		// Handle class implementations that don't have a direct versions link
		if (versionsURL == null && type.equals("CLAS/OC")) {
			versionsURL = "includes/implementations/versions";
		}

		// Exit if no versions URL could be determined
		if (versionsURL == null) {
			return null;
		}

		String versionURIString = "/sap/bc/" + fileName + versionsURL;

		// Adjust URI path for class implementations
		if (type.equals("CLAS/OC") && !versionURIString.contains("/adt/oo")) {
			versionURIString = versionURIString.replace("/adt/classlib", "/adt/oo");
		}

		// Create REST resource to fetch version data
		IRestResourceFactory restResourceFactory = AdtRestResourceFactory.createRestResourceFactory();

		// Ensure user is logged on to the ABAP system
		AdtLogonServiceUIFactory.createLogonServiceUI().ensureLoggedOn(abapProject.getDestinationData(),
				PlatformUI.getWorkbench().getProgressService());

		String destination = abapProject.getDestinationId();
		URI versionUri = URI.create(versionURIString);
		IRestResource versionResource = restResourceFactory.createResourceWithStatelessSession(versionUri, destination);

		// Configure content handler for version data response
		VersionDataContentHandler versionHandler = new VersionDataContentHandler();
		versionResource.addContentHandler(versionHandler);

		// Set Accept header for ATOM+XML response
		IHeaders requestHeader = HeadersFactory.newHeaders();
		IField acceptField = HeadersFactory.newField("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
		requestHeader.setField(acceptField);

		VersionData versions = null;
		try {
			versions = (VersionData) versionResource.get(null, requestHeader, VersionData.class);
		} catch (RuntimeException e) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"An exception occured reading the versions " + e.getMessage());
		}

		// Display results in TransportView
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
		try {
			workbenchPage.showView("com.consetto.adt.cloudalmlink.views.TransportView");
			TransportView transportView = (TransportView) workbenchPage
					.findView("com.consetto.adt.cloudalmlink.views.TransportView");
			transportView.setVersionData(versions);
		} catch (PartInitException e) {
			// View could not be opened - fail silently
		}

		return null;
	}
}
