# CLAUDE.md

## Language

Write everything in this repository in English, regardless of the language the request is written in
(the maintainer often writes in Russian). This covers code, identifiers, comments, log messages,
commit messages, branch names, pull request titles and descriptions, review replies, and documentation.

The only exception is user-facing plugin messages in the language files under
`AreaShop/src/main/resources/lang/`: when adding a message, add it to `EN.yml` (and `EN-MM.yml`) and
also add a Russian translation to `RU.yml`, since the production server runs in Russian.

## Project

- AreaShop fork targeting Paper 1.20.6, built with Gradle (`./gradlew build`, JDK 21).
- The plugin version lives in `gradle.properties`; CI is defined in `.github/workflows/`.
