:toc:

= Flutter l18n Flutter Gen-l18n IntelliJ IDEA Plugin


This plugin provides convenient features for generating translations in your Flutter project using the l18n (localization) package. It integrates with IntelliJ IDEA and offers the following features:



<!-- Plugin description -->

## Features

1. **Internationalize file/directory**: Right click file or directory, select translate file/directory, choose strings to translate (e.g. print statements you may ignore). Get a best guess for the intl keys (i.e. based on context of the file, and also filename) by AI. Accept and translate to all languages configured in your project.

2. **Translate to new Language**: You can right-click on an `.arb` (Application Resource Bundle) file in the project view and choose the "Translate this to new Arb file" option. This action translates the entire file.

3. **Translate missing keys**: go to untranslated-messages.txt and right-click anywhere, choose "Translate missing keys"  This will translate all missing keys in your project.

4. **Custom instructions**: You can customize the instructions for the AI model to suit your translation needs. This allows you to provide specific guidelines or context for the translations.

5. **Select model**: You can choose between different AI models (e.g., gemini-flash, ...). I choose Gemini, because it's affordable, ridiculously fast and works well for this use case. Initially I had used openAI, but you all know what happened to them. They got steamrolled.

## Requirements

To use this plugin, you need an Gemini API key. Please make sure you have the API key before proceeding.

## Possible Improvements
Here are some ideas, if you want to improve (sorry for the code base)

### Performance Speedup by translating multiple keys per request instead of one

this is a bigger refactor
starting with the request to the LLM, we need to request multi line json, we need to identify them back again and then we need to map them back correctly. we need chunking and upstream we need all the changes, too. e.g. translation task amount calculation. 

But it's also valid to just fire 100 parallel tasks... it's just resource heavy and one needs an api key...

### Check for existing keys
e.g. when we translate a file there are many common buttons, like e.g. Cancel, Save, .... we should provide all existing common keys to AI and let AI choose, if we can use them. If yes, use them, if not proceed with the translation (of the rest)


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
