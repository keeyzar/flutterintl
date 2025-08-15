<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# gpt-helper Changelog

## [Unreleased]

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

## [1.4]
### Added
- Added Logo

## [1.0]
### Added
- initial release
