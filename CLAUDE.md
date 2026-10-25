# CLAUDE.md

## Language

Write everything in this repository in English, regardless of the language the request is written in
(the maintainer often writes in Russian). This covers code, identifiers, comments, log messages,
commit messages, branch names, pull request titles and descriptions, review replies, and documentation.

The only exception is user-facing plugin messages in the language files under
`AreaShop/src/main/resources/lang/`: when adding a message, add it to `EN.yml` and `EN-MM.yml` and
also add a Russian translation to `RU.yml`, since the production server runs in Russian.

## What this is

A maintained fork of AreaShop (the original by NLthijs48 is unmaintained) for the PointForCraft
Minecraft server. AreaShop lets players rent and buy WorldGuard regions through signs and commands.

- Target platform: Paper 1.20.6. That is the minimum: commands use cloud-paper's Brigadier support,
  which needs Paper 1.20.6. Compile against 1.20.6 (`gradle/libs.versions.toml`) so newer API can't
  slip in.
- The main module still compiles against `spigot-api`, not `paper-api`, so Paper-only API is not
  available there without a dependency change.
- Requires WorldGuard and WorldEdit (or FastAsyncWorldEdit); Vault for the economy; Essentials is optional.
- The default branch is `dev/bleeding`.

## Layout

- `AreaShop/` (Gradle project `:areashop`): the plugin itself, package `me.wiefferink.areashop`.
  - `AreaShop.java`: plugin entry point (enable, reload, config and language setup).
  - `commands/`: one class per `/areashop` subcommand, built on Incendo Cloud 2;
    `commands/util/` holds the base class `AreashopCommandBean`, registration and argument parsers.
  - `regions/`: `GeneralRegion`, `RentRegion`, `BuyRegion`, `RegionGroup`; region data is stored as YAML.
  - `features/`: per-region features (signs, friends, teleport, WorldGuard flags, ...). New region
    behaviour belongs here rather than in `GeneralRegion`.
  - `managers/`: file storage (`FileManager`), caches, feature and sign managers.
  - `modules/`: Guice modules; services are wired through dependency injection.
  - `ASLanguageManager.java`: message loading on top of the InteractiveMessenger library.
  - `src/main/resources/`: `config.yml`, `default.yml`, `hiddenConfig.yml`, `plugin.yml`, `lang/`.
- `areashop-interface/`: API interfaces shared with the adapters.
- `adapters/platform/`: platform abstraction (`platform-interface`, `paper`).
- `adapters/plugins/`: version-specific integrations (`worldedit`, `worldguard`, `fastasyncworldedit`,
  `essentials`), shaded into the final jar.

## Messages and translations

- Messages are referenced by key and resolved through the language files; never hard-code
  player-facing text in Java.
- `ASLanguageManager` saves the bundled `lang/*.yml` files without losing edits: `lang/.bundled/` keeps
  the bundled version each file was based on, and on a plugin update only messages that were not edited
  on disk get the new text. `lang/custom/<language>.yml` is an optional overlay with only changed keys.
  Changing an existing message's text in the bundled files therefore reaches servers only where it was
  not customised; use a new key when the old text must not survive.
- `EN.yml` uses the legacy InteractiveMessenger format; `EN-MM.yml` is the MiniMessage variant used
  when `useMiniMessage: true`. Keep both in sync when adding or changing a message.

## Building

- JDK 21, Gradle wrapper: `./gradlew build`. The plugin jar is
  `AreaShop/build/libs/AreaShop-<version>.jar` (the one without a suffix).
- The version lives in `gradle.properties` (`version=`); CI overrides it with `-Pversion=`.
- Dependencies come from jitpack, enginehub, papermc and other repositories that may be unreachable
  from a sandbox. If a local build can't resolve dependencies, push and rely on CI rather than
  changing build files to work around it.
- There is no test suite yet; at minimum make sure CI's `build` passes.

## CI and releases

- `.github/workflows/ci.yml`: builds every push to a branch other than `dev/bleeding` and uploads the
  jar as an artifact. It deliberately has no `pull_request` trigger; avoid job-level `if:` conditions
  that leave "skipped" checks on pull requests.
- `.github/workflows/dev-build.yml`: pushes to `dev/bleeding` build and refresh the rolling `dev-build`
  pre-release.
- `.github/workflows/release.yml`: a `vX.Y.Z` tag publishes a GitHub Release (a suffix such as `-rc.1`
  makes it a pre-release). After a release, bump `gradle.properties` to the next patch `-SNAPSHOT`.
  Details in `documentation/releasing.md`.

## Conventions

- Match the style of the file you are editing; indentation varies between files (tabs in most older
  code, spaces in some newer files).
- `plugin.yml` has CRLF line endings; keep them.
- New permissions go in `plugin.yml`; new config options get a commented default in `config.yml`.
- Keep changes backwards compatible with existing region files and configs on a live server; if a
  format change is unavoidable, migrate old data on load.
- Work happens on feature branches with a pull request into `dev/bleeding`.
