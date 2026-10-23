#!/usr/bin/env bash
set -euo pipefail
# Secrets are read from caller-owned files, never passed as command-line values.
: "${APKSIGNER:?Path to Android SDK apksigner}"
: "${MOMENTUM_KEYSTORE:?Private release keystore outside the checkout}"
: "${MOMENTUM_PASSWORD_FILE:?Private keystore password file}"
input=${1:?Unsigned APK path}
output=${2:?Signed output APK path}
if [[ -n "${MOMENTUM_LINEAGE:-}" ]]; then
  : "${MOMENTUM_LEGACY_KEYSTORE:?Original signing keystore}"
  : "${MOMENTUM_LEGACY_PASSWORD_FILE:?Original keystore password file}"
  "$APKSIGNER" sign --lineage "$MOMENTUM_LINEAGE" --rotation-min-sdk-version 28 \
    --ks "$MOMENTUM_LEGACY_KEYSTORE" --ks-pass "file:$MOMENTUM_LEGACY_PASSWORD_FILE" \
    --next-signer --ks "$MOMENTUM_KEYSTORE" --ks-pass "file:$MOMENTUM_PASSWORD_FILE" \
    --out "$output" "$input"
else
  "$APKSIGNER" sign --ks "$MOMENTUM_KEYSTORE" --ks-pass "file:$MOMENTUM_PASSWORD_FILE" --out "$output" "$input"
fi
"$APKSIGNER" verify --verbose "$output"
