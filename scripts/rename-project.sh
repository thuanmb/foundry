#!/usr/bin/env bash
# Rebrand this base project for a new product.
#
#   scripts/rename-project.sh --slug acme --name "Acme" --package com.acme.app
#
#   --slug     lowercase letters/digits only; used for artifacts, images, Helm charts, DB name, URL scheme
#   --name     human-readable product name shown in UIs and docs
#   --package  Java base package; also used as the iOS bundle id / Android package
#
# Run from a clean git working tree so you can review the result with `git diff`.
set -euo pipefail

OLD_SLUG="foundry"
OLD_NAME="Foundry"
OLD_PACKAGE="com.thuanbui.foundry"
OLD_GROUP="com.thuanbui"

SLUG="" NAME="" PACKAGE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --slug) SLUG="$2"; shift 2 ;;
    --name) NAME="$2"; shift 2 ;;
    --package) PACKAGE="$2"; shift 2 ;;
    -h|--help) sed -n '2,11p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "$SLUG" || -z "$NAME" || -z "$PACKAGE" ]]; then
  echo "Usage: $0 --slug <slug> --name <Display Name> --package <java.package>" >&2
  exit 1
fi
[[ "$SLUG" =~ ^[a-z][a-z0-9]*$ ]] || { echo "--slug must match ^[a-z][a-z0-9]*$" >&2; exit 1; }
[[ "$PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || { echo "--package must look like com.example.app" >&2; exit 1; }

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

GROUP="${PACKAGE%.*}"
# PascalCase identifier for Java class names (e.g. "Acme Notes" -> AcmeNotes)
PASCAL="$(printf '%s' "$NAME" | perl -pe 's/[^A-Za-z0-9]+/ /g; s/(\w+)/\u$1/g; s/\s+//g')"
[[ "$PASCAL" =~ ^[A-Z][A-Za-z0-9]*$ ]] || { echo "--name must contain letters to derive a class name" >&2; exit 1; }

OLD_PATH="${OLD_PACKAGE//.//}"
NEW_PATH="${PACKAGE//.//}"

echo "Renaming: slug=$OLD_SLUG->$SLUG name=$OLD_NAME->$NAME package=$OLD_PACKAGE->$PACKAGE class=${PASCAL}Application"

# 1. Move Java package directories
for src in common/src/main api-server/src/main api-server/src/test worker/src/main worker/src/test; do
  dir="$src/java/$OLD_PATH"
  if [[ -d "$dir" ]]; then
    mkdir -p "$src/java/$(dirname "$NEW_PATH")"
    mv "$dir" "$src/java/$NEW_PATH"
    # Remove now-empty parent directories of the old package
    find "$src/java" -type d -empty -delete
  fi
done

# 2. Rename files and directories that carry the old names
for f in $(find api-server worker -name "${OLD_NAME}*Application.java" -not -path '*/target/*'); do
  mv "$f" "$(dirname "$f")/$(basename "$f" | sed "s/^${OLD_NAME}/${PASCAL}/")"
done
[[ -d "api-server/helm/$OLD_SLUG" ]] && mv "api-server/helm/$OLD_SLUG" "api-server/helm/$SLUG"
[[ -d "worker/helm/$OLD_SLUG-worker" ]] && mv "worker/helm/$OLD_SLUG-worker" "worker/helm/$SLUG-worker"

# 3. Replace text in all tracked-type text files
FILES=$(grep -rIl -e "$OLD_SLUG" -e "$OLD_NAME" -e "$OLD_GROUP" . \
  --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target --exclude-dir=build \
  --exclude-dir=dist --exclude-dir=.expo --exclude=rename-project.sh || true)

if [[ -n "$FILES" ]]; then
  # Order matters: most specific patterns first
  OLD_PACKAGE="$OLD_PACKAGE" PACKAGE="$PACKAGE" OLD_PATH="$OLD_PATH" NEW_PATH="$NEW_PATH" \
  OLD_GROUP="$OLD_GROUP" GROUP="$GROUP" OLD_NAME="$OLD_NAME" NAME="$NAME" PASCAL="$PASCAL" \
  OLD_SLUG="$OLD_SLUG" SLUG="$SLUG" \
  perl -pi -e '
    s/\Q$ENV{OLD_PACKAGE}\E/$ENV{PACKAGE}/g;
    s/\Q$ENV{OLD_PATH}\E/$ENV{NEW_PATH}/g;
    s/<groupId>\Q$ENV{OLD_GROUP}\E<\/groupId>/<groupId>$ENV{GROUP}<\/groupId>/g;
    s/\b\Q$ENV{OLD_NAME}\E(Worker)?Application\b/$ENV{PASCAL}$1Application/g;
    s/\Q$ENV{OLD_NAME}\E/$ENV{NAME}/g;
    s/\Q$ENV{OLD_SLUG}\E/$ENV{SLUG}/g;
  ' $FILES
fi

echo "Done. Next steps:"
echo "  1. Review: git status && git diff --stat"
echo "  2. Verify: ./mvnw -q -DskipTests install && (cd web-app && npm test) && (cd mobile-app && npm run typecheck)"
echo "  3. Follow the 'Start a new product' checklist in README.md (secrets, Firebase files, theme, icons)."
