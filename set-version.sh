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

# Files to update
FILES=(
    "src/main/java/com/cashier/constant/AppConstants.java"
    "pom.xml"
    "install.sh"
    "start.bat"
    "package.bat"
    "jpackage.bat"
)

echo "[1/5] Updating version in Java files..."
sed -i '' "s/APP_VERSION = \"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION = \"$NEW_VERSION\"/g" \
    src/main/java/com/cashier/constant/AppConstants.java

echo "[2/5] Updating version in pom.xml..."
sed -i '' "s/<version>[0-9]\+\.[0-9]\+\.[0-9]\+<\/version>/<version>$NEW_VERSION<\/version>/g" pom.xml

echo "[3/5] Updating version in shell scripts..."
sed -i '' "s/APP_VERSION=-\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION=\"$NEW_VERSION\"/g" install.sh
sed -i '' "s/APP_VERSION=-\"[0-9]\+\.[0-9]\+\.[0-9]\+\"/APP_VERSION=\"$NEW_VERSION\"/g" install.sh

echo "[4/5] Updating version in batch scripts..."
sed -i '' "s/VERSION\\" 2\.[0-9]\+\.[0-9]\+/VERSION\\" $NEW_VERSION/g" start.bat
sed -i '' "s/VERSION\\" 2\.[0-9]\+\.[0-9]\+/VERSION\\" $NEW_VERSION/g" package.bat
sed -i '' "s/set \"APP_VERSION=2\.[0-9]\+\.[0-9]\+\"/set \"APP_VERSION=$NEW_VERSION\"/g" start.bat
sed -i '' "s/set \"APP_VERSION=2\.[0-9]\+\.[0-9]\+\"/set \"APP_VERSION=$NEW_VERSION\"/g" package.bat
sed -i '' "s/REM Version 2\.[0-9]\+\.[0-9]\+/REM Version $NEW_VERSION/g" jpackage.bat
sed -i '' "s/set \"APP_VERSION=2\.[0-9]\+\.[0-9]\+\"/set \"APP_VERSION=$NEW_VERSION\"/g" jpackage.bat

echo "[5/5] Verifying updates..."
echo ""

# Count updated files
UPDATED=0
for file in "${FILES[@]}"; do
    if git diff --quiet "$file" 2>/dev/null; then
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
