package com.consetto.adt.cloudalmlink.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.consetto.adt.cloudalmlink.model.CloudAlmConfig;
import com.consetto.adt.cloudalmlink.services.PreferenceService;

/**
 * Preference page for configuring Cloud ALM connection settings.
 * Allows users to enter tenant, region, and OAuth client credentials.
 */
public class CloudAlmPeferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public CloudAlmPeferencePage() {
		super(GRID);
		setPreferenceStore(PreferenceService.getInstance().getPreferenceStore());
		setDescription("Please enter tenant and region from Cloud ALM: https://tenant.region.alm.cloud.sap");
	}

	@Override
	public void createFieldEditors() {
		addField(new HostLabelFieldEditor(PreferenceConstants.P_TEN, "Tenant:", getFieldEditorParent()));
		addField(new HostLabelFieldEditor(PreferenceConstants.P_REG, "Region:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_CID, "Client ID:", getFieldEditorParent()));
		StringFieldEditor secretEditor = new StringFieldEditor(PreferenceConstants.P_KEY, "Client Secret:", getFieldEditorParent());
		// Do not show the secret on screen (screen sharing, screenshots)
		secretEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(secretEditor);
		addField(new BooleanFieldEditor(PreferenceConstants.P_DEMO, "Enable Demo Mode", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
		// No initialization required
	}

	/**
	 * Accepts only what can be one label of the Cloud ALM host name (e.g. "mycompany", "eu10"),
	 * because tenant and region decide where the client credentials are sent.
	 */
	private static class HostLabelFieldEditor extends StringFieldEditor {

		HostLabelFieldEditor(String name, String labelText, Composite parent) {
			super(name, labelText, parent);
			setErrorMessage(labelText.replace(":", "") + " may only contain letters, digits and hyphens");
		}

		@Override
		protected boolean doCheckState() {
			String value = getStringValue();
			return value.isEmpty() || CloudAlmConfig.isHostLabel(value);
		}
	}
}
