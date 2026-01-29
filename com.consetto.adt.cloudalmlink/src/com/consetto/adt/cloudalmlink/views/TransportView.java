package com.consetto.adt.cloudalmlink.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.model.DemoDataProvider;
import com.consetto.adt.cloudalmlink.model.VersionData;
import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.consetto.adt.cloudalmlink.services.PreferenceService;
import com.consetto.adt.cloudalmlink.util.CloudAlmLinkLogger;

import jakarta.inject.Inject;

/**
 * Eclipse View displaying transport versions and their associated Cloud ALM features.
 * Provides table view with columns for ID, Transport, Title, Feature, Status, and Responsible.
 */
public class TransportView extends ViewPart {

	public static final String ID = "com.consetto.adt.cloudalmlink.views.TransportView";

	@Inject
	IWorkbench workbench;

	private TableViewer viewer;
	private Action showInBrowserAction;
	private TransportFilter searchFilter;
	private boolean isDemoMode = false;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);

		Label searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText("Search: ");

		final Text searchText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		searchText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		searchText.setMessage("Filter by any column...");

		// Initialize filter and connect to search field
		searchFilter = new TransportFilter();
		searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				searchFilter.setSearchText(searchText.getText());
				viewer.refresh();
			}
		});

		createViewer(parent);
	}

	/**
	 * Creates and configures the table viewer for displaying version data.
	 */
	private void createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setUseHashlookup(true);
		viewer.addFilter(searchFilter);
		viewer.setInput(VersionData.getInstance().getVersions());

		// Make selection available to other views
		getSite().setSelectionProvider(viewer);

		// Configure table layout
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(gridData);

		makeActions();
		contributeToActionBars();
		hookContextMenu();
		hookDoubleClickAction();
	}

	public TableViewer getViewer() {
		return viewer;
	}

	/**
	 * Creates table columns using the declarative column definitions.
	 */
	private void createColumns() {
		List<TableColumnDefinition<VersionElement>> columns = TransportViewColumns.getColumns();

		for (TableColumnDefinition<VersionElement> colDef : columns) {
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			TableColumn column = viewerColumn.getColumn();
			column.setText(colDef.title());
			column.setWidth(colDef.width());
			column.setResizable(true);
			column.setMoveable(true);

			viewerColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof VersionElement version) {
						return colDef.getText(version);
					}
					return "";
				}
			});
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(showInBrowserAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(showInBrowserAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(showInBrowserAction);
	}

	/**
	 * Creates the "Open in Cloud ALM" action for viewing features in browser.
	 */
	private void makeActions() {
		showInBrowserAction = new Action() {
			@Override
			public void run() {
				IStructuredSelection selection = viewer.getStructuredSelection();
				Object obj = selection.getFirstElement();

				// Use pattern matching for instanceof (Java 21)
				if (obj instanceof VersionElement version) {
					String featureId = version.getFeature() != null ? version.getFeature().getDisplayId() : null;

					if (featureId != null) {
						String calmURL;
						if (isDemoMode) {
							calmURL = DemoDataProvider.DEMO_CLOUD_ALM_URL;
						} else {
							// Use service layer for URL building
							CloudAlmConfig config = PreferenceService.getInstance().getCloudAlmConfig();
							calmURL = config.featureUrl(featureId);
						}
						try {
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(calmURL));
						} catch (PartInitException | MalformedURLException e) {
							CloudAlmLinkLogger.logError("Failed to open browser for URL: " + calmURL, e);
							showMessage("Could not open browser. Please check the Error Log for details.");
						}
					} else {
						showMessage("No Feature ID available for this transport.");
					}
				} else {
					showMessage("Please select a valid Element.");
				}
			}
		};
		showInBrowserAction.setText("Open in Cloud ALM");
		showInBrowserAction.setToolTipText("Show in Browser");
	}

	/**
	 * Registers double-click listener to trigger the browser action.
	 */
	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(event -> showInBrowserAction.run());
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Cloud ALM Transports", message);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		// Clean up resources
		searchFilter = null;
		viewer = null;
		showInBrowserAction = null;
		super.dispose();
	}

	/**
	 * Updates the view with new version data.
	 *
	 * @param versions The version data to display
	 */
	public void setVersionData(VersionData versions) {
		isDemoMode = false;
		viewer.setInput(versions.getVersions());
	}

	/**
	 * Updates the view with demo data and sets demo mode flag.
	 *
	 * @param demoVersions The demo version elements to display
	 */
	public void setDemoData(List<VersionElement> demoVersions) {
		isDemoMode = true;
		viewer.setInput(demoVersions);
	}
}
