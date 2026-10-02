# Cloud ALM Link: guide for contributors and coding agents

Cloud ALM Link is an Eclipse plugin for ABAP Development Tools (ADT). The README describes what it
does; this file describes how to change it.

## Principles

- **Credentials stay local.** The Cloud ALM client secret is sent only to the SAP token endpoint
  of the configured tenant. Never log it, never put it in a URL, never add it to test fixtures.
- **Data from ADT and Cloud ALM is untrusted.** Transport IDs, titles and feature IDs end up in
  URLs and in the browser. Validate or encode them before they become part of a URL.
- **Never block the UI thread** with network calls in new code; use a `Job`.

## Layout

| Path | What lives there |
| --- | --- |
| `com.consetto.adt.cloudalmlink/` | The plugin: `plugin.xml`, `META-INF/MANIFEST.MF`, sources in `src/` |
| `…/handlers/` | Commands (show transports, open in Cloud ALM), Cloud ALM API client, comment hyperlinks |
| `…/model/` | Config, token, version and feature models, demo data |
| `…/views/` | The "Cloud ALM Transports" view |
| `com.consetto.adt.cloudalmlink.feature/` | Eclipse feature; its id must not change (existing installs update by it) |
| `com.consetto.adt.cloudalmlink.site/` | p2 update site (`category.xml`) |
| `com.consetto.adt.cloudalmlink.tests/` | Plain JUnit 5 tests, run in the Maven reactor |

## Checks

```bash
mvn verify                              # oldest supported Eclipse (eclipse.release in pom.xml)
mvn verify -Declipse.release=latest     # newest Eclipse; CI runs both
```

## Pull requests

- Every change is a pull request; nothing is pushed to master directly. One coherent change per
  PR, opened as a **draft** until finished and tested. Fill the template.
- Commit messages: a short plain subject, no conventional-commit prefix.
- Never commit secrets, tenant names, SAP system IDs or customer data.
- Version: change `Bundle-Version` in the MANIFEST, `feature.xml` and every `pom.xml` together
  (`mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=x.y.z-SNAPSHOT`).
