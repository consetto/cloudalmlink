# Testing

## Unit tests

`com.consetto.adt.cloudalmlink.tests` holds plain JUnit 5 tests that run without Eclipse. They
compile the plugin's **own sources** (no copies) and test them directly:

| Plugin classes | Tests |
| --- | --- |
| `core/AdtResponseParser`: atom links, transport IDs, parent requests, ToC titles, feature IDs in descriptions | `core/AdtResponseParserTest`, `core/CorePatternsTest` |
| `core/VersionUris`: versions and transports endpoints of an object | `core/VersionUrisTest`, `core/CorePatternsTest` |
| `core/CalmIds`: Cloud ALM IDs in ABAP comments | `core/CalmIdsTest`, `core/CorePatternsTest` |
| `core/VersionSearch`: the search field of the Cloud ALM Transports view | `core/VersionSearchTest` |
| `model/BearerToken`, `CloudAlmConfig`, `CloudAlmItemType`, `FeatureElement`, `VersionElement` | `model/*Test` |

The tests module's `pom.xml` lists which plugin sources it compiles. Those classes must not use
Eclipse or ADT APIs. If one starts to, the tests module no longer compiles: move the new logic into
`core/` and keep only the Eclipse/ADT calls in the handler or view.

```bash
mvn verify                                  # whole build including the tests (what CI runs)
mvn test -f com.consetto.adt.cloudalmlink.tests/pom.xml   # only the unit tests, no Eclipse download
```

In Eclipse or IntelliJ, import `com.consetto.adt.cloudalmlink.tests` as a Maven project and run the
tests from the IDE.

**Test the refusal, not only the success.** For every check that refuses something (an invalid
tenant, an ID inside a date), add a test that fails when the check is removed.

## What needs a real Eclipse with ADT

Not covered by the unit tests; check by hand before merging changes in these areas and describe
the run in the pull request:

| Area | Check |
| --- | --- |
| `CalmSourceHandler` (Show Transports and Features) | From the editor and from the Project Explorer, for a class, a program and a CDS view |
| `CalmApiHandler` (Cloud ALM API) | Features appear for a transport linked to a feature; wrong credentials do not freeze Eclipse |
| `CalmTransportHandler` (Open in Cloud ALM) | From the Transport Organizer view and the transport editor |
| `CalmCommentScanner` | Ctrl+hover on IDs in `"` and `*` comments; no link on dates |
| `TransportView` | Search, double-click, Open in Transport Organizer |
| Preference page | Invalid tenant/region is rejected, secret is masked |
