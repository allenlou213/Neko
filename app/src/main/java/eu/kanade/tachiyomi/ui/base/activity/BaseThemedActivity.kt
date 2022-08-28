package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.setThemeByPref
import uy.kohesive.injekt.injectLazy

abstract class BaseThemedActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()
    private var updatedTheme: Resources.Theme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        updatedTheme = null
        setThemeByPref(preferences)
        super.onCreate(savedInstanceState)
    }
}
