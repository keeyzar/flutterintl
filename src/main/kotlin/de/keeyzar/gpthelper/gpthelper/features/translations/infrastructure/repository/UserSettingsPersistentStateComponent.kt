package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


@State(name = "FlutterIntlSettings", storages =[Storage(value="plugin_gpt_intl_settings.xml")])
@Service
class UserSettingsPersistentStateComponent : PersistentStateComponent<UserSettingsPersistentStateComponent.UserSettingsModel> {

    companion object {
        fun getInstance(): UserSettingsPersistentStateComponent {
            return ApplicationManager.getApplication().getService(UserSettingsPersistentStateComponent::class.java);
        }
    }

    private var model = UserSettingsModel()

    override fun getState(): UserSettingsModel {
        return model
    }

    override fun loadState(state: UserSettingsModel) {
        XmlSerializerUtil.copyBean(state, model)
    }

    fun setNewState(newState: UserSettingsModel) {
        model = newState;
    }

    class UserSettingsModel {
        /**
         * where is the directory containing the arb files
         */
        var arbDir: String by object: RWProperty<UserSettingsModel, String>("") {}
        /**
         * how is the key names (e.g. S, AppLocalizations)
         */
        var outputClass: String by object: RWProperty<UserSettingsModel, String>("") {}
        /**
         * whether we're working with a nullable getter
         */
        var nullableGetter: Boolean by object: RWProperty<UserSettingsModel, Boolean>(true) {}

        /**
         * base file for the translations, i.e. here you can find the prefix
         */
        var templateArbFile: String by object: RWProperty<UserSettingsModel, String>("") {}
        var intlConfigFile: String by object: RWProperty<UserSettingsModel, String>("") {}
        var watchIntlConfigFile: Boolean by object: RWProperty<UserSettingsModel, Boolean>(true) {}
        /**
         * here you can find the file, where the outputClass is located in
         */
        var outputLocalizationFile: String by object: RWProperty<UserSettingsModel, String>("") {}
        var parallelism: Int by object: RWProperty<UserSettingsModel, Int>(3) {}
        var tonality: String by object: RWProperty<UserSettingsModel, String>("informal") {}
        var gptModel: String? by object: RWProperty<UserSettingsModel, String?>("gpt-3.5-turbo-0613") {}
        var translateAdvancedArbKeys: Boolean by object: RWProperty<UserSettingsModel, Boolean>(true) {}

        open class RWProperty<R, T>(initValue: T) : ReadWriteProperty<R, T> {
            private var backingField: T = initValue
            override fun getValue(thisRef: R, property: KProperty<*>): T {
                println("State::${property.name}.getValue(), value: '$backingField'")
                return backingField
            }

            override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
                backingField = value
                println("State::${property.name}.setValue(), value: '$backingField'")
            }
        }
    }


}
