package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "FlutterIntlSettings", storages =[Storage(value="plugin_gpt_intl_settings.xml")])
@Service
class UserSettingsPersistentStateComponent : PersistentStateComponent<UserSettingsPersistentStateComponent.IdeaUserSettingsModel> {

    companion object {
        fun getInstance(): UserSettingsPersistentStateComponent {
            return ApplicationManager.getApplication().getService(UserSettingsPersistentStateComponent::class.java);
        }
    }



    private var model = IdeaUserSettingsModel(
        "",
        "",
        true,
        "",
        "",
        true,
        ""
    )

    override fun getState(): IdeaUserSettingsModel {
        return model
    }



    override fun loadState(state: IdeaUserSettingsModel) {
        XmlSerializerUtil.copyBean(state, model)
    }

    fun setNewState(newState: IdeaUserSettingsModel) {
        model = newState;
    }

    data class IdeaUserSettingsModel(
        /**
         * where is the directory containing the arb files
         */
        val arbDir: String,
        /**
         * how is the key names (e.g. S, AppLocalizations)
         */
        val outputClass: String,
        /**
         * whether we're working with a nullable getter
         */
        val nullableGetter: Boolean,

        /**
         * base file for the translations, i.e. here you can find the prefix
         */
        val templateArbFile: String,
        val intlConfigFile: String,
        val watchIntlConfigFile: Boolean,
        /**
         * here you can find the file, where the outputClass is located in
         */
        val outputLocalizationFile: String
    ) {
        constructor() : this("", "", false, "", "", true, "")
    }

}
