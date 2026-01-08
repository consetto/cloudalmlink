package com.consetto.adt.cloudalmlink.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Preference page for configuring Cloud ALM connection settings.
 * Allows users to enter tenant, region, and OAuth client credentials.
 */
public class CloudAlmPeferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public CloudAlmPeferencePage() {
		super(GRID);
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"com.consetto.adt.cloudalmlink.preferences.CloudAlmPeferencePage");
		setPreferenceStore(scopedPreferenceStore);
		setDescription("Please enter tenant and region from Cloud ALM: https://tenant.region.alm.cloud.sap");
	}

	@Override
	public void createFieldEditors() {
		addField(new StringFieldEditor(PreferenceConstants.P_TEN, "Tenant:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_REG, "Region:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_CID, "Client ID:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_KEY, "Client Secret:", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		// No initialization required
	}
}
