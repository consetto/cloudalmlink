# Cloud ALM Link Plugin - Unit Tests

This document describes the unit test suite for the Cloud ALM Link Eclipse plugin and provides instructions on how to run them.

## Overview

The test suite is designed to validate the core business logic of the Cloud ALM Link plugin without requiring the Eclipse runtime environment. Tests are organized into two phases:

- **Phase 1**: Model layer tests (POJOs, data classes, filtering logic)
- **Phase 2**: Handler tests (pattern matching, URL construction, XML parsing)

## Test Project Structure

```
com.consetto.adt.cloudalmlink.tests/
├── run-tests.sh                     # Test runner script (compile + run)
├── lib/                             # JARs (committed, no internet needed)
│   ├── junit-platform-console-standalone-1.10.2.jar
│   ├── junit-jupiter-api-5.10.2.jar
│   ├── assertj-core-3.25.3.jar
│   ├── gson-2.12.1.jar
│   └── ...                          # (10 JARs total)
├── build/                           # Compiled output (gitignored)
├── src/
│   ├── main/java/                   # Testable source copies (Eclipse-free)
│   │   └── com/consetto/adt/cloudalmlink/
│   │       ├── model/
│   │       │   ├── BearerToken.java
│   │       │   ├── FeatureElement.java
│   │       │   └── VersionElement.java
│   │       ├── views/
│   │       │   └── TransportFilter.java
│   │       └── handlers/
│   │           ├── PatternUtils.java
│   │           └── AtomLinkParser.java
│   └── test/java/                   # Unit tests
│       └── com/consetto/adt/cloudalmlink/
│           ├── model/
│           │   ├── BearerTokenTest.java
│           │   ├── FeatureElementTest.java
│           │   └── VersionElementTest.java
│           ├── views/
│           │   └── TransportFilterTest.java
│           └── handlers/
│               ├── PatternUtilsTest.java
│               └── AtomLinkParserTest.java
```

## Prerequisites

- **Java 21** or higher
- No build tool (Maven/Gradle) required — the included shell script handles everything

## Running the Tests

### Using the test runner script

```bash
# From the repo root:
./com.consetto.adt.cloudalmlink.tests/run-tests.sh

# Or from the test project directory:
cd com.consetto.adt.cloudalmlink.tests
./run-tests.sh
```

The script handles compilation and test execution automatically:

- **First run**: downloads any missing JARs to `lib/`, compiles all sources, runs tests
- **Subsequent runs** (no source changes): skips compilation, runs tests directly
- **After editing a `.java` file**: detects the change, recompiles, then runs tests

Incremental compilation uses a marker file (`build/.compiled`) — if any `.java` source is newer than the marker, the script recompiles. All JARs are committed to `lib/`, so no internet access is needed for normal use.

### Using an IDE

#### IntelliJ IDEA
1. Import the `com.consetto.adt.cloudalmlink.tests` directory as a project
2. Add all JARs from `lib/` to the module classpath
3. Right-click on `src/test/java` → Run 'All Tests'

#### Eclipse
1. Import the test project
2. Add all JARs from `lib/` to the build path
3. Right-click on a test class → Run As → JUnit Test

#### VS Code
1. Install the "Java Test Runner" extension
2. Open the test folder
3. Click the play button next to test classes or methods

## Test Coverage

### Phase 1: Model Layer Tests

| Test Class | Source Class | Description |
|------------|--------------|-------------|
| `BearerTokenTest` | `BearerToken` | OAuth token management, expiration logic with 5-second buffer |
| `VersionElementTest` | `VersionElement` | Transport/version data model |
| `FeatureElementTest` | `FeatureElement` | Cloud ALM feature entity, JSON deserialization |
| `TransportFilterTest` | `TransportFilter` | Case-insensitive filtering across all fields |

### Phase 2: Handler Tests

| Test Class | Source Class | Description |
|------------|--------------|-------------|
| `PatternUtilsTest` | `PatternUtils` | Transport ID extraction, URI parsing, Cloud ALM URL construction |
| `AtomLinkParserTest` | `AtomLinkParser` | ADT XML response parsing, atom link extraction |

## Test Categories

### BearerTokenTest
- Token properties (access token, token type)
- Token validity (expiration logic, 5-second safety buffer)
- Expiration time calculation
- JSON deserialization via Gson

### VersionElementTest
- Basic properties (ID, transport, author, title)
- Feature association
- Null handling
- Common SAP transport ID formats (NPL, DEV, S4D)

### FeatureElementTest
- All feature properties (UUID, displayId, status, project, etc.)
- Status codes (IN_PROGRESS, RELEASED, COMPLETED, IN_REVIEW)
- Display ID formats for all Cloud ALM item types
- JSON deserialization

### TransportFilterTest
- Empty/null search handling
- Version field matching (ID, transport, author, title)
- Feature field matching (displayId, status, responsible)
- Case-insensitivity
- Null field handling

### PatternUtilsTest
- Transport ID extraction from XML responses (3 patterns)
- URI parameter extraction and decoding
- Feature ID extraction from descriptions
- Cloud ALM ID validation (6-, 3-, 7-, 15- prefixes)
- Comment context detection
- Cloud ALM URL construction for all item types
- Version URI resolution

### AtomLinkParserTest
- Atom link parsing (rel before href, href before rel)
- Namespace handling (atom:link, link)
- Self-closing tags
- Complex URIs with query parameters
- Path extraction from ADT URIs
- Real-world ADT XML responses

## Test Dependencies

The test project uses the following dependencies:

| Dependency | Version | Purpose |
|------------|---------|---------|
| JUnit Jupiter | 5.10.2 | Test framework |
| JUnit Platform Console Standalone | 1.10.2 | CLI test runner |
| AssertJ | 3.25.3 | Fluent assertions |
| Gson | 2.12.1 | JSON deserialization tests |
| ByteBuddy | 1.14.12 | Runtime code generation (AssertJ dependency) |
| OpenTest4J | 1.3.0 | Test exception types |

All JARs are committed to `lib/` for reproducibility without internet access.

## Test Reports

Test results are printed directly to the console by the JUnit Platform Console runner, including a tree view of all tests and a summary with pass/fail counts.

---

## Future Considerations: Eclipse-Specific Tests

The current test suite covers standalone business logic. For comprehensive testing of Eclipse integration, the following approaches should be considered.

### 1. Eclipse PDE Test Fragment

Create a test fragment that runs within the Eclipse runtime:

```
com.consetto.adt.cloudalmlink.tests.pde/
├── META-INF/
│   └── MANIFEST.MF
├── fragment.xml
├── build.properties
└── src/
    └── com/consetto/adt/cloudalmlink/
        └── pde/
            ├── CalmSourceHandlerIntegrationTest.java
            ├── TransportViewIntegrationTest.java
            └── CalmCommentScannerIntegrationTest.java
```

**MANIFEST.MF for Test Fragment:**
```manifest
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: Cloud ALM Link Tests (PDE)
Bundle-SymbolicName: com.consetto.adt.cloudalmlink.tests.pde
Bundle-Version: 0.9.4.qualifier
Fragment-Host: com.consetto.adt.cloudalmlink;bundle-version="0.9.4"
Bundle-RequiredExecutionEnvironment: JavaSE-21
Require-Bundle: org.junit;bundle-version="4.13",
 org.eclipse.ui,
 org.eclipse.core.runtime,
 org.mockito;bundle-version="5.0"
```

### 2. SWTBot for UI Tests

For testing UI components like `TransportView`:

```java
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

public class TransportViewSWTBotTest {
    private SWTWorkbenchBot bot;

    @Before
    public void setUp() {
        bot = new SWTWorkbenchBot();
    }

    @Test
    public void shouldDisplayTransportsInTable() {
        // Open view
        bot.menu("Window").menu("Show View").menu("Other...").click();
        bot.tree().expandNode("Cloud ALM").select("Transports");
        bot.button("Open").click();

        // Verify table
        SWTBotTable table = bot.table();
        assertThat(table.columnCount()).isGreaterThan(0);
    }
}
```

**Required Dependencies:**
```xml
<dependency>
    <groupId>org.eclipse.swtbot</groupId>
    <artifactId>org.eclipse.swtbot.eclipse.finder</artifactId>
    <version>4.1.0</version>
    <scope>test</scope>
</dependency>
```

### 3. Mock Objects for Eclipse APIs

For testing handlers that use Eclipse services:

```java
import static org.mockito.Mockito.*;

public class CalmSourceHandlerTest {

    @Mock
    private IPreferenceStore preferenceStore;

    @Mock
    private IWorkbenchWindow workbenchWindow;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock preference store
        when(preferenceStore.getString(PreferenceConstants.P_TEN))
            .thenReturn("mytenant");
        when(preferenceStore.getString(PreferenceConstants.P_REG))
            .thenReturn("eu10");
    }

    @Test
    public void shouldHandleExecutionEvent() {
        // Test with mocked Eclipse services
    }
}
```

### 4. Tycho for Maven-Based PDE Testing

Add Tycho plugins to run PDE tests from Maven:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.eclipse.tycho</groupId>
            <artifactId>tycho-surefire-plugin</artifactId>
            <version>4.0.4</version>
            <configuration>
                <useUIHarness>true</useUIHarness>
                <useUIThread>true</useUIThread>
                <product>org.eclipse.platform.ide</product>
                <application>org.eclipse.ui.ide.workbench</application>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 5. What Needs to Be Tested with Eclipse Runtime

| Component | Test Type | Dependencies |
|-----------|-----------|--------------|
| `CalmSourceHandler.execute()` | Integration | ADT REST API mocks, Eclipse command framework |
| `CalmApiHandler` HTTP calls | Integration | WireMock for HTTP mocking |
| `TransportView` rendering | SWTBot UI | Eclipse workbench, SWT widgets |
| `CalmCommentScanner` detection | Integration | Eclipse text editor, hyperlink framework |
| `AdtObjectContext.fromEditor()` | Integration | ADT editor model, IFile adapters |
| Preference page | SWTBot UI | Preference store, field editors |

### 6. Setting Up Eclipse PDE Test Infrastructure

1. **Create Target Platform**
   - Define target platform with Eclipse SDK + ADT plugins
   - Export as `.target` file

2. **Configure Test Launch**
   - Create JUnit Plug-in Test launch configuration
   - Select test fragment
   - Configure required bundles

3. **CI/CD Integration**
   - Use Tycho for headless test execution
   - Configure Xvfb for UI tests on Linux CI

### 7. Recommended Next Steps

1. **Immediate**: Use the current standalone tests for model/handler logic
2. **Short-term**: Create mock wrappers for `IPreferenceStore` to test more handler code
3. **Medium-term**: Set up Tycho build with PDE test fragment
4. **Long-term**: Implement SWTBot tests for critical UI workflows

### 8. Resources

- [Eclipse PDE Testing Documentation](https://www.eclipse.org/pde/pde-build/)
- [SWTBot Documentation](https://www.eclipse.org/swtbot/)
- [Tycho Testing Guide](https://tycho.eclipseprojects.io/doc/latest/Testing.html)
- [Mockito with Eclipse](https://www.vogella.com/tutorials/Mockito/article.html)

---

## Summary

| Phase | Coverage | Eclipse Required | Status |
|-------|----------|------------------|--------|
| Phase 1 | Model layer | No | Implemented |
| Phase 2 | Handlers (utilities) | No | Implemented |
| Future | Eclipse integration | Yes | Documented |
| Future | UI components | Yes | Documented |

The current test suite provides solid coverage for the core business logic (~60-70% of testable code). Eclipse-specific tests require additional infrastructure setup but the groundwork is documented above for future implementation.
