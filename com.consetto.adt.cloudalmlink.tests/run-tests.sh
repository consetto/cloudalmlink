#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIB_DIR="lib"
BUILD_DIR="build/classes"
MARKER="build/.compiled"

# --- 1. Ensure dependencies exist ---
ensure_jar() {
    local jar="$1" url="$2"
    if [ ! -f "$LIB_DIR/$jar" ]; then
        echo "Downloading $jar..."
        mkdir -p "$LIB_DIR"
        curl -sL -o "$LIB_DIR/$jar" "$url"
    fi
}

ensure_jar "junit-platform-console-standalone-1.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar"
ensure_jar "assertj-core-3.25.3.jar" \
    "https://repo1.maven.org/maven2/org/assertj/assertj-core/3.25.3/assertj-core-3.25.3.jar"
ensure_jar "gson-2.12.1.jar" \
    "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.12.1/gson-2.12.1.jar"
ensure_jar "byte-buddy-1.14.12.jar" \
    "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.14.12/byte-buddy-1.14.12.jar"
ensure_jar "opentest4j-1.3.0.jar" \
    "https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"
ensure_jar "junit-jupiter-api-5.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar"
ensure_jar "junit-jupiter-engine-5.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.2/junit-jupiter-engine-5.10.2.jar"
ensure_jar "junit-platform-engine-1.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-engine/1.10.2/junit-platform-engine-1.10.2.jar"
ensure_jar "junit-platform-commons-1.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar"
ensure_jar "junit-platform-launcher-1.10.2.jar" \
    "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.2/junit-platform-launcher-1.10.2.jar"

# --- 2. Build classpath ---
CP=$(echo "$LIB_DIR"/*.jar | tr ' ' ':')

# --- 3. Compile if sources changed ---
MAIN_SRC="../com.consetto.adt.cloudalmlink/src"
NEEDS_COMPILE=false
if [ ! -f "$MARKER" ]; then
    NEEDS_COMPILE=true
else
    # Check if any .java file in test project or main plugin is newer than the marker
    if find src "$MAIN_SRC" -name '*.java' -newer "$MARKER" 2>/dev/null | grep -q .; then
        NEEDS_COMPILE=true
    fi
fi

if [ "$NEEDS_COMPILE" = true ]; then
    echo "Compiling..."
    rm -rf "$BUILD_DIR"
    mkdir -p "$BUILD_DIR"
    javac -cp "$CP" -d "$BUILD_DIR" \
        $(find src/main/java -name '*.java') \
        $(find src/test/java -name '*.java')
    touch "$MARKER"
    echo "Compilation complete."
else
    echo "Sources unchanged — skipping compilation."
fi

# --- 4. Run tests ---
echo ""
java -jar "$LIB_DIR/junit-platform-console-standalone-1.10.2.jar" \
    --class-path "$BUILD_DIR:$LIB_DIR/assertj-core-3.25.3.jar:$LIB_DIR/gson-2.12.1.jar:$LIB_DIR/byte-buddy-1.14.12.jar" \
    --scan-class-path "$BUILD_DIR"
