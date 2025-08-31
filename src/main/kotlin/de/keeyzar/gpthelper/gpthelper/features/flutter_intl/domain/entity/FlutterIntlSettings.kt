package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlutterIntlSettings(
    @JsonProperty("arb-dir")
    var arbDir: String = "lib/l10n",
    @JsonProperty("template-arb-file")
    var templateArbFile: String = "app_en.arb",
    @JsonProperty("output-localization-file")
    var outputLocalizationFile: String = "lib/l10n/app_localizations.dart",
    @JsonProperty("output-class")
    var outputClass: String = "AppLocalizations",
    @JsonProperty("nullable-getter")
    var nullableGetter: Boolean = true,
    @JsonProperty("untranslated-messages-file")
    var untranslatedMessagesFile: String = "untranslated_messages.txt",
)
