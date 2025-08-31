:toc:

= Flutter l18n Flutter Gen-l18n IntelliJ IDEA Plugin


This plugin provides convenient features for generating translations in your Flutter project using the l18n (localization) package. It integrates with IntelliJ IDEA and offers the following features:



<!-- Plugin description -->

## Features

1. **Intention Action**: While editing a Dart String literal, you can press `Alt+Enter` and select "Generate Translations" from the menu. This action generates the translation for the selected string.

2. **Translate File Action**: You can right-click on an `.arb` (Application Resource Bundle) file in the project view and choose the "Translate this to new Arb file" option. This action translates the entire file.

## Requirements

To use this plugin, you need an OpenAI API key. Please make sure you have the API key before proceeding.

## Possible Improvements
Here are some ideas, if you want to improve (sorry for the code base)

### background task issues
calculation and handling of the translation task is kinda not working.

### Check for existing keys
e.g. when we translate a file there are many common buttons, like e.g. Cancel, Save, .... we should provide all existing common keys to AI and let AI choose, if we can use them. If yes, use them, if not proceed with the translation (of the rest)

### Single editor action for translation
There are multiple actions registered for translation... But I don't feel to bad about that, as e.g. Agent Mode from gemini is not really working well with that either - and they're the brains (and have better AI and more resources)


### ask for a review after some translations


### untranslated keys (e.g. because of an error)
1. read untranslated keys (from flutter l10n file)
2. get keys (key + @key) from base
3. translate keys
4. append to file
5. unscramble file
6. reformat file

### show history of translations in dialog for desired key
often times you have identical structure, and you want to see the latest one

### error on existing keys
currently you can add existing keys, which is an error

### make error dialog clickable (and allow showing stacktrace)
there is most definitely some kind of existing function for that (showError or something like that)

## Disclaimer

This plugin was developed for the purpose of learning Kotlin and IntelliJ IDEA plugin development. The code quality may not be optimal as it was developed while experimenting with Domain-Driven Design (DDD) and faced time constraints.

Please note that this plugin is provided as-is, and I do not guarantee its compatibility with future versions of Flutter or IntelliJ IDEA. Use it at your own risk.

If you encounter any issues or have suggestions for improvement, feel free to report them on the project's GitHub repository.

**Important**: Keep in mind that the use of the Gemini API for translation may be subject to terms and conditions, including limitations on the number of requests or usage restrictions. Ensure you comply with OpenAI's guidelines and any applicable usage restrictions when using this plugin.

We hope you find this plugin helpful for your Flutter localization needs!
<!-- Plugin description end -->

## Installation

1. https://plugins.jetbrains.com/plugin/21732-gpt-flutter-intl[Flutter l18n GPT here]
