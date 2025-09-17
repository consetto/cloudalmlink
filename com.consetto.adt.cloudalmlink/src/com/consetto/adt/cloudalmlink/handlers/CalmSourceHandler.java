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
import com.sap.adt.tools.core.project.AdtProjectServiceFactory;
import com.sap.adt.tools.core.project.IAbapProject;
import com.sap.adt.tools.core.ui.editors.IAdtFormEditor;
import com.sap.adt.util.satomparser.model.IDocument;

public class CalmSourceHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// get active adt editor
		IAdtFormEditor editor = (IAdtFormEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		// get the document from the editor
		org.eclipse.jface.text.IDocument document = editor.getAdapter(AbapSourcePage.class).getDocument();

		// 3) Get the source code from the document
		String code = document.get();

		// get ADT URL
		String adtUrl = editor.getModelFile().getRawLocationURI().toString();
		// print ADT URL

		System.out.println("ADT URL: " + adtUrl);

		// extract the substring from the ADT URL to get the path to the file
		String fileName = adtUrl.substring(adtUrl.indexOf(".") + 1, adtUrl.lastIndexOf("/") + 1);
		System.out.println("File name: " + fileName);

//		IFile file = editor.getModelFile();
		EList<IAtomLink> links = editor.getModel().getLinks();
		
		String type = editor.getModel().getType();

		// loop through the links. If the href contain source/main/versions or
		// implementations/versions, save it to a new String versionsURL
		String versionsURL = null;
		for (IAtomLink link : links) {
			// print the link href
			System.out.println("Link href: " + link.getHref());
			// check if the link href contains source/main/versions or
			// implementations/versions
			if (link.getHref().contains("source/main/versions")
					|| link.getHref().contains("implementations/versions")) {
				versionsURL = link.getHref();
				System.out.println("Versions URL: " + versionsURL);
				break;
			}

		}
		
		// version null and type = "CLAS/OC" set versionsURL to "includes/implementations/versions"
		if (versionsURL == null && type.equals("CLAS/OC")) {
			versionsURL = "includes/implementations/versions";
			System.out.println("Versions URL set to: " + versionsURL);
		}
		
		// if versionsURL is still null, exit the method
		if (versionsURL == null) {
			return null;
		}

		String versionURIString = "/sap/bc/" + fileName + versionsURL;
		
		// if type is "CLAS/OC" and versionsURLString does not contain /adt/oo, add replace /adt/classlib with /adt/oo
		if (type.equals("CLAS/OC") && !versionURIString.contains("/adt/oo")) {
			versionURIString = versionURIString.replace("/adt/classlib", "/adt/oo");
			System.out.println("Version URI String set to: " + versionURIString);
		} 

		// Coding for the REST call to get the versions
		IRestResourceFactory restResourceFactory = AdtRestResourceFactory.createRestResourceFactory();
		// Get available projects in the workspace
		IProject[] abapProjects = AdtProjectServiceFactory.createProjectService().getAvailableAbapProjects();
		// Use the first project in the workspace for the demo
		// to keep the example simple
		IAbapProject abapProject = (IAbapProject) abapProjects[0].getAdapter(IAbapProject.class);
		// Trigger logon dialog if necessary
		AdtLogonServiceUIFactory.createLogonServiceUI().ensureLoggedOn(abapProject.getDestinationData(),
				PlatformUI.getWorkbench().getProgressService());
		// Create REST resource for given destination and URI

		String destination = abapProject.getDestinationId();
		URI versionUri = URI.create(versionURIString);
		IRestResource versionResource = restResourceFactory.createResourceWithStatelessSession(versionUri, destination);

		// Create and add the content handler:
		VersionDataContentHandler versionHandler = new VersionDataContentHandler();

		// versionResource.setAcceptContentType(versionHandler.getSupportedContentType());
		versionResource.addContentHandler(versionHandler);

		// add accept header
		IHeaders requestHeader = HeadersFactory.newHeaders();
		IField acceptFiled = HeadersFactory.newField("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
		requestHeader.setField(acceptFiled);

		VersionData versions = null;
		try {
			versions = (VersionData) versionResource.get(null, requestHeader, VersionData.class);

		} catch (RuntimeException e) {
			MessageDialog.openError(window.getShell(), "ADT Cloud ALM Link Error",
					"An exception occured reading the versions " + e.getMessage());
		}
		
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();

		try {
			workbenchPage.showView("com.consetto.adt.cloudalmlink.views.TransportView");
			TransportView transportView = (TransportView) workbenchPage
					.findView("com.consetto.adt.cloudalmlink.views.TransportView");
			transportView.setVersionData(versions);
			
			
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


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
