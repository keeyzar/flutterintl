package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.entity.FlutterIntlSettings
import java.nio.file.Path

/**
 * provides flutter intl settings, if there are any
 * caution, the user might have settings, which he uses in the console, therefore this might not be the ground truth
 */
interface FlutterIntlSettingsRepository {
    fun getFlutterIntlSettings(): FlutterIntlSettings
    fun loadFlutterIntlSettingsByPath(path: Path): FlutterIntlSettings
}
