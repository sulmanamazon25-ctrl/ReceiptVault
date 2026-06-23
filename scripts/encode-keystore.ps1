# Run from repo root after release.keystore exists.
# Copy the base64 output into GitHub secret ANDROID_KEYSTORE_BASE64.

[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PSScriptRoot\..\release.keystore"))
