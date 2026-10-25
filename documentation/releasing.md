# Builds and releases

Builds run on GitHub Actions. The version lives in `gradle.properties` (`version=...`); CI passes `-Pversion=...` to override it.

## Dev builds

Every push to a branch other than `dev/bleeding` runs `.github/workflows/ci.yml`. It builds the plugin
with the version `<gradle.properties version>+<run number>.<short commit>` (for example
`2.9.2-SNAPSHOT+42.d790749`) and uploads the jar as a workflow artifact, kept for 30 days. Pull requests
show the check from the push to their branch; pull requests from forks are not built here (push their
branch to this repository to build it).

Pushes to `dev/bleeding` run `.github/workflows/dev-build.yml` instead, which does the same build and
also refreshes the `dev-build` pre-release on the Releases page, so the latest jar can be downloaded
without signing in to GitHub.

## Releases

1. Set `version` in `gradle.properties` to the release version (for example `2.9.2`), update `changelog.md`, and commit.
2. Tag the commit and push the tag:
   ```
   git tag v2.9.2
   git push origin v2.9.2
   ```
3. `.github/workflows/release.yml` builds the jar as `AreaShop-2.9.2.jar` and creates a GitHub Release with generated notes.
   Tags with a suffix, such as `v2.10.0-rc.1`, are published as pre-releases.
4. Bump `version` in `gradle.properties` to the next snapshot (for example `2.9.3-SNAPSHOT`) and commit.

The release version always comes from the tag, so a tag that doesn't match `gradle.properties` still builds with the tag's version.
