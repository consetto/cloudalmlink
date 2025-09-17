package com.consetto.adt.cloudalmlink.handlers;

import java.nio.charset.Charset;

import javax.xml.stream.XMLStreamReader;

import com.consetto.adt.cloudalmlink.model.VersionData;
import com.sap.adt.communication.content.AdtMediaType;
import com.sap.adt.communication.content.IContentHandler;
import com.sap.adt.communication.message.IMessageBody;
import com.sap.adt.tools.core.content.AdtStaxContentHandlerUtility;

public class VersionDataContentHandler implements IContentHandler<VersionData> {
	
	//constructor
	public VersionDataContentHandler() {
		super();
	//	this.contentType = "application/atom+xml;type=feed"

	}
	
	@Override
	public VersionData deserialize(IMessageBody body, Class<? extends VersionData> dataType) {
		
	
		// new versionData instance
		VersionData versionData = VersionData.INSTANCE;
		// parse the body
		versionData.parseBody(body);
		
		return versionData;
	
	}

	@Override
	public IMessageBody serialize(VersionData dataObject, Charset charset) {
		// serialization is not supported for VersionData
		return null;
	}

	@Override
	public String getSupportedContentType() {
	//  return AdtMediaType.APPLICATION_XML;
	   // return "application/atom+xml;type=feed";
		return AdtMediaType.ATOM_XML;
	//    return "application/atom+xml";
	}

	@Override
	public Class<VersionData> getSupportedDataType() {
		
		return VersionData.class;
	}
	
	
	

}