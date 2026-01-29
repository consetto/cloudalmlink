package com.consetto.adt.cloudalmlink.handlers;

import java.nio.charset.Charset;

import com.consetto.adt.cloudalmlink.model.VersionData;
import com.sap.adt.communication.content.AdtMediaType;
import com.sap.adt.communication.content.IContentHandler;
import com.sap.adt.communication.message.IMessageBody;

/**
 * Content handler for ADT REST resource responses containing version data.
 * Handles deserialization of ATOM+XML feed responses into VersionData objects.
 */
public class VersionDataContentHandler implements IContentHandler<VersionData> {

	@Override
	public VersionData deserialize(IMessageBody body, Class<? extends VersionData> dataType) {
		// Create new VersionData instance using factory method
		VersionData versionData = VersionData.fromMessageBody(body);
		// Update shared instance for backward compatibility
		VersionData.setInstance(versionData);
		return versionData;
	}

	@Override
	public IMessageBody serialize(VersionData dataObject, Charset charset) {
		// Serialization is not supported for VersionData
		return null;
	}

	@Override
	public String getSupportedContentType() {
		return AdtMediaType.ATOM_XML;
	}

	@Override
	public Class<VersionData> getSupportedDataType() {
		return VersionData.class;
	}
}
