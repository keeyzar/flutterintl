<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# gpt-helper Changelog

## [Unreleased]

## [2.0.6] - 2025-09-04

### Added

- fix parallelism config not applied - again

## [2.0.5] - 2025-09-04

### Added

- fix parallelism config not applied

## [2.0.4] - 2025-09-03

### Added

- fix single quote issue in arb files
- highlight text in translation dialog context field

## [2.0.3] - 2025-09-03

### Added

- fix single quote issue in arb files
- highlight text in translation dialog context field

## [2.0.2] - 2025-31-08

### Fixed

- Translation progress was not reported. It is fixed now
- When many best guesses are done, we now show a progress bar

## [2.0.1] - 2024-05-24

### Fixed

- minor plugin description issues

## [2.0.0] - 2024-05-24

### Changed

- Use Gemini instead of openAI
- Better error handling, better UI, better everything :)

### Added

- Allow complex arb entries - and create arb entry key suggestions based on AI and context (e.g. filename)
- internationalize whole files and directories in a single go
- Ignore unknown properties in intl settings file instead of failing

### Fixed

- Known bug - translation tasks are not closed after translation is done... (this is not fixed, but a known issue)

## [1.18.2]

### Fixed

- cancellation of task finally works
- change translation now uses correct key

## [1.18.0]

### Added

- FASTER translations - because of queue
- CHANGE translations from within an arb entry, i.e. if you're in e.g. app_en.arb then press alt+enter on any key
- allow lowerCamelCase key for translations

## [1.17.0]

### Fixed

- With new Open AI API compatible again :)
- Can use gpt-4 turbo - fixed instructions for it to work

### Added

- allow lowerCamelCase key for translations

## [1.16.0]

### Fixed

- Plugin works again, after fixing class loader issues.. :)

### Added

- Use gpt 3.5 with nice speed for complex translations.
- allow selecting gpt model in settings
- allow usage of the old translation prompting without complex keys
- allow configuration of translation history length

## [1.11.0]

### Added

- allow creation of complex arb entries, i.e. with placeholder, with plural, with numbers! YAY!
- translation history in translation input dialog, so that one can easily look up old key structure
- allow selecting gpt model in settings
- allow usage of the old translation prompting without complex keys
- allow configuration of translation history length

## [1.10.0]

### Added

- configure parallel translations - because openAI blocks new account parallelism

### Fixed

- fix errors in context of formatting after removing const modifier
- allow undoing all localizations with two UNDO commands - wow was that annoying...

## [1.9.0]

### Added

- parallel translations
- use old translation data

### Fixed

- fix nullable getter generated code

## [1.8.0]

### Added

- Ask user to rate plugin, if he uses it for a while

## [1.7.0]

### Changed

- Remove const modifier if it exists in parent

## [1.6.0]

### Added

- Allow for auto localization of file

## [1.5.0]

### Changed

- Use project level persistence

## [1.4.0]

### Added

- Added Logo

## [1.0.0]

### Added

- initial release

[Unreleased]: https://github.com/keeyzar/gpt-helper/compare/v2.0.5...HEAD
[2.0.5]: https://github.com/keeyzar/gpt-helper/compare/v2.0.4...v2.0.5
[2.0.4]: https://github.com/keeyzar/gpt-helper/compare/v2.0.3...v2.0.4
[2.0.3]: https://github.com/keeyzar/gpt-helper/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/keeyzar/gpt-helper/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/keeyzar/gpt-helper/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/keeyzar/gpt-helper/compare/v1.18.2...v2.0.0
[1.18.2]: https://github.com/keeyzar/gpt-helper/compare/v1.18.0...v1.18.2
[1.18.0]: https://github.com/keeyzar/gpt-helper/compare/v1.17.0...v1.18.0
[1.17.0]: https://github.com/keeyzar/gpt-helper/compare/v1.16.0...v1.17.0
[1.16.0]: https://github.com/keeyzar/gpt-helper/compare/v1.11.0...v1.16.0
[1.11.0]: https://github.com/keeyzar/gpt-helper/compare/v1.10.0...v1.11.0
[1.10.0]: https://github.com/keeyzar/gpt-helper/compare/v1.9.0...v1.10.0
[1.9.0]: https://github.com/keeyzar/gpt-helper/compare/v1.8.0...v1.9.0
[1.8.0]: https://github.com/keeyzar/gpt-helper/compare/v1.7.0...v1.8.0
[1.7.0]: https://github.com/keeyzar/gpt-helper/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/keeyzar/gpt-helper/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/keeyzar/gpt-helper/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/keeyzar/gpt-helper/compare/v1.0.0...v1.4.0
[1.0.0]: https://github.com/keeyzar/gpt-helper/commits/v1.0.0
