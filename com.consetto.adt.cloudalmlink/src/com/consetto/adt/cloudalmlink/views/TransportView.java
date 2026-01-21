package com.consetto.adt.cloudalmlink.views;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.consetto.adt.cloudalmlink.model.DemoDataProvider;
import com.consetto.adt.cloudalmlink.model.VersionData;
import com.consetto.adt.cloudalmlink.model.VersionElement;
import com.consetto.adt.cloudalmlink.preferences.PreferenceConstants;

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
	private boolean isDemoMode = false;

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(2, false);
		parent.setLayout(layout);

		Label searchLabel = new Label(parent, SWT.NONE);
		searchLabel.setText("Search: ");

		final Text searchText = new Text(parent, SWT.BORDER | SWT.SEARCH);
		searchText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		createViewer(parent);
	}

	/**
	 * Creates and configures the table viewer for displaying version data.
	 */
	private void createViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns(parent, viewer);

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(VersionData.INSTANCE.getVersions());

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
	 * Creates table columns for version/feature display.
	 */
	private void createColumns(final Composite parent, final TableViewer viewer) {
		String[] titles = { "ID", "Transport", "Title", "Feature", "Status", "Responsible" };
		int[] bounds = { 100, 100, 180, 100, 100, 100 };

		// ID column
		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VersionElement v = (VersionElement) element;
				return v.getID();
			}
		});

		// Transport column
		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VersionElement v = (VersionElement) element;
				return v.getTransportId();
			}
		});

		// Title column
		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VersionElement v = (VersionElement) element;
				return v.getTitle();
			}
		});

		// Feature column
		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VersionElement v = (VersionElement) element;
				return v.getFeature() != null ? v.getFeature().getDisplayId() : "No Feature";
			}
		});

		// Status column
		col = createTableViewerColumn(titles[4], bounds[4], 4);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VersionElement v = (VersionElement) element;
				return v.getFeature() != null ? v.getFeature().getStatus() : "No Feature";
			}
		});

		// Responsible column
		col = createTableViewerColumn(titles[5], bounds[5], 5);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				VersionElement v = (VersionElement) element;
				return v.getFeature() != null ? v.getFeature().getResponsibleId() : "No Feature";
			}
		});
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	/**
	 * Label provider for table cells with image support.
	 */
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		@Override
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		@Override
		public Image getImage(Object obj) {
			return workbench.getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TransportView.this.fillContextMenu(manager);
			}
		});
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
			public void run() {
				IStructuredSelection selection = viewer.getStructuredSelection();
				Object obj = selection.getFirstElement();

				if (obj instanceof VersionElement) {
					VersionElement version = (VersionElement) obj;
					String featureId = version.getFeature() != null ? version.getFeature().getDisplayId() : null;

					if (featureId != null) {
						String calmURL;
						if (isDemoMode) {
							calmURL = DemoDataProvider.DEMO_CLOUD_ALM_URL;
						} else {
							ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
									"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
							String region = scopedPreferenceStore.getString(PreferenceConstants.P_REG);
							String tenant = scopedPreferenceStore.getString(PreferenceConstants.P_TEN);
							String baseUrl = "https://" + tenant + "." + region + ".alm.cloud.sap";
							calmURL = baseUrl + "/launchpad#feature-display?sap-ui-app-id-hint=com.sap.calm.imp.cdm.features.ui&/details/"
									+ featureId;
						}
						try {
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(calmURL));
						} catch (PartInitException | MalformedURLException e) {
							// Browser could not be opened - fail silently
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
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				showInBrowserAction.run();
			}
		});
	}

	private void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), "Cloud ALM Transports", message);
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
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
