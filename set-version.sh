#!/bin/bash

# LiSuan Version Update Script
# Updates version across all project files

set -e

if [ -z "$1" ]; then
    echo "Usage: ./set-version.sh x.y.z"
    echo "Example: ./set-version.sh 2.5.7"
    exit 1
fi

NEW_VERSION="$1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================"
echo "  LiSuan Version Update"
echo "========================================"
echo ""
echo "Setting version to: $NEW_VERSION"
echo ""

# Validate version format
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "[Error] Invalid version format: $NEW_VERSION"
    echo "Expected format: x.y.z (e.g., 2.5.7)"
    exit 1
fi

# Detect sed -i syntax (macOS vs Linux)
if [[ "$OSTYPE" == "darwin"* ]]; then
    SED_INPLACE="sed -i ''"
else
    SED_INPLACE="sed -i"
fi

# Helper: sed with compatible -i flag
sed_i() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

echo "[1/7] Updating version in Java files..."
sed_i "s/APP_VERSION = \"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION = \"$NEW_VERSION\"/g" \
    src/main/java/com/cashier/constant/AppConstants.java

echo "[2/7] Updating version in pom.xml..."
sed_i "s/<version>[0-9]\+\.[0-9]\+\.[0-9]\+<\/version>/<version>$NEW_VERSION<\/version>/g" pom.xml

echo "[3/7] Updating version in batch scripts..."
for f in start.bat quick-start.bat install.bat; do
    if [ -f "$f" ]; then
        sed_i "s/set \"APP_VERSION=[0-9]\+\.[0-9]\+\.[0-9]\+\"/set \"APP_VERSION=$NEW_VERSION\"/g" "$f"
        sed_i "s/Version [0-9]\+\.[0-9]\+\.[0-9]\+/Version $NEW_VERSION/g" "$f"
    fi
done

echo "[4/7] Updating version in diagnose.bat and create-shortcut.bat..."
if [ -f "diagnose.bat" ]; then
    sed_i "s/set \"APP_VERSION=[0-9]\+\.[0-9]\+\.[0-9]\+\"/set \"APP_VERSION=$NEW_VERSION\"/g" diagnose.bat
    sed_i "s/APP_VERSION=\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION=\"$NEW_VERSION\"/g" diagnose.bat
fi
if [ -f "create-shortcut.bat" ]; then
    sed_i "s/APP_VERSION=v[0-9]\+\.[0-9]\+\.[0-9]\+/APP_VERSION=v$NEW_VERSION/g" create-shortcut.bat
fi

echo "[5/7] Updating version in PowerShell scripts..."
for f in run-app.ps1 package-simple.ps1; do
    if [ -f "$f" ]; then
        sed_i "s/\\\$APP_VERSION = \"[0-9]\+\.[0-9]\+\.[0-9]\+\"/\$APP_VERSION = \"$NEW_VERSION\"/g" "$f"
    fi
done

echo "[6/8] Updating version in shell scripts and .env..."
if [ -f "start.sh" ]; then
    sed_i "s/APP_VERSION=\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION=\"$NEW_VERSION\"/g" start.sh
fi
if [ -f "install.sh" ]; then
    sed_i "s/APP_VERSION:-\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION:-\"$NEW_VERSION\"/g" install.sh
fi
if [ -f ".env.example" ]; then
    sed_i "s/APP_VERSION=[0-9]\+\.[0-9]\+\.[0-9]\+/APP_VERSION=$NEW_VERSION/g" .env.example
fi

echo "[7/8] Updating version in i18n files..."
# Extract version components for i18n pattern (e.g., 2.5.7 -> 2\.5\.6 -> 2\.5\.7)
OLD_VERSION_PATTERN="v[0-9]\+\.[0-9]\+\.[0-9]\+"
NEW_VERSION_PATTERN="v$NEW_VERSION"
for f in src/main/resources/com/cashier/i18n/messages*.properties; do
    if [ -f "$f" ]; then
        sed_i "s/$OLD_VERSION_PATTERN/$NEW_VERSION_PATTERN/g" "$f"
    fi
done

echo "[8/8] Verifying updates..."
echo ""

# Files that should have been updated
FILES=(
    "src/main/java/com/cashier/constant/AppConstants.java"
    "pom.xml"
    "start.bat"
    "quick-start.bat"
    "install.bat"
    "diagnose.bat"
    "create-shortcut.bat"
    "run-app.ps1"
    "package-simple.ps1"
    "start.sh"
    "install.sh"
    ".env.example"
    "src/main/resources/com/cashier/i18n/messages.properties"
    "src/main/resources/com/cashier/i18n/messages_en.properties"
    "src/main/resources/com/cashier/i18n/messages_zh_CN.properties"
    "src/main/resources/com/cashier/i18n/messages_zh_TW.properties"
    "src/main/resources/com/cashier/i18n/messages_ja.properties"
    "src/main/resources/com/cashier/i18n/messages_ko.properties"
)

# Count updated files
UPDATED=0
for file in "${FILES[@]}"; do
    if [ ! -f "$file" ]; then
        echo "  [ ] $file (not found)"
    elif git diff --quiet "$file" 2>/dev/null; then
        echo "  [ ] $file (no change)"
    else
        echo "  [X] $file (updated)"
        ((UPDATED++))
    fi
done

echo ""
echo "========================================"
echo "  Version update complete!"
echo "========================================"
echo ""
echo "Files updated: $UPDATED"
echo "New version: $NEW_VERSION"
echo ""
echo "Next steps:"
echo "  1. Review changes: git diff"
echo "  2. Commit: git add -A && git commit -m \"chore: 发布版本 v$NEW_VERSION\""
echo "  3. Create tag: git tag -a v$NEW_VERSION -m \"LiSuan v$NEW_VERSION\""
echo "  4. Push: git push origin main && git push origin v$NEW_VERSION"
echo ""
