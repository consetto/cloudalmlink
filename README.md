# Cloud ALM Link for ADT

Cloud ALM Link integrates SAP Cloud ALM with ABAP Development Tools (ADT) in Eclipse, allowing you to navigate seamlessly between your ABAP development environment and Cloud ALM.

Detailed information can be found at https://cloudalmlink.consetto.com/

## Prerequisites

- Eclipse IDE 2025-03 or newer
- ABAP Development Tools (ADT) installed
- Enabled Cloud ALM API with Scope calm-api.features.read
https://help.sap.com/docs/cloud-alm/apis/enabling-sap-cloud-alm-apis?locale=en-US

## Installation

The plugin can be installed directly into your Eclipse IDE using the standard "Install New Software" feature.

1. Open Eclipse and navigate to **Help > Install New Software...**
2. In the "Work with" field, add the following update site URL:
   ```
   https://cloudalmlink.consetto.com/latest
   ```
3. Select **Cloud ALM Link** from the list of available software.
4. Click **Next**, accept the license agreement, and click **Finish**.
5. Restart Eclipse when prompted.

## Configuration

Before using the plugin, you need to configure your Cloud ALM tenant settings:

1. Go to **Window > Preferences** (or **Eclipse > Preferences** on macOS)
2. Navigate to **ABAP > ADT Cloud ALM Link**
3. Enter your Cloud ALM settings:
   - **Tenant**: Your Cloud ALM tenant name (e.g., `mycompany`)
   - **Region**: Your Cloud ALM region (e.g., `eu10`)
   - **Client ID**: Your Client ID
   - **Client Secret**: Your Client Secret

## Features

### Show Transports and Features

From any ABAP source editor, you can view the transports and Cloud ALM features associated with the current object:

- Right-click in the editor and select **Show Transports and Features**
- Or use the keyboard shortcut **Ctrl+6** (Cmd+6 on macOS)

### Open Feature from Transport Organizer

Right-click on a transport request in the Transport Organizer or Transport Editor and select **Open in Cloud ALM** to open the corresponding transport in Cloud ALM.



### Cloud ALM Links in Comments

The plugin automatically detects Cloud ALM IDs in your ABAP source code comments and makes them clickable:

- **Features**: IDs starting with `6-` (e.g., `6-1234`)
- **Tasks/Requirements**: IDs starting with `3-` (e.g., `3-144444`)

To use this feature:
1. Add a Cloud ALM ID in a comment, for example:
   ```abap
   * Implementation for feature 6-1234
   " Fix for task 3-144444
   ```
2. Hold **Ctrl** (Cmd on macOS) and hover over the ID
3. Click **Open in Cloud ALM** to open the feature or task in your browser


## License

This project is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Copyright (c) Consetto GmbH.

## Contributing

Contributions are welcome! Feel free to submit issues and pull requests.
