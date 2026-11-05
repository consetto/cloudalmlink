# Roadmap

What is planned for Cloud ALM Link for ADT. Each item lands as its own pull request; the order
within a section is the intended order, not a promise of dates.

## Next release (0.9.7)

- **Version feed parsing in core.** Read the ADT versions feed with a plain StAX parser in
  `core`, so it is unit-tested without Eclipse and refuses DTDs and external entities.
- **Quieter Error Log.** Stop logging expected answers as warnings: a Cloud ALM transport without
  a feature, and object types the backend does not serve as plain XML.
- **New shortcut.** *Show Transports and Features* moves to Ctrl+Alt+6 (Cmd+Option+6 on macOS),
  because Ctrl+6 is ADT's *Open SAP GUI*.
- **Client secret in Eclipse Secure Storage.** Store the Cloud ALM client secret encrypted
  instead of in the workspace preferences, and move an existing secret over on first start.

## Later

- **One Cloud ALM tenant per ABAP project.** Let each ABAP project use its own Cloud ALM tenant
  instead of one tenant for the whole workspace. Needs research first: where to store the
  assignment and how it fits the project properties ADT already has.
