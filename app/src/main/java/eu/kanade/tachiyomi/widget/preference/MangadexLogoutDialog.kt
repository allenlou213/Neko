package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.PrefAccountLoginBinding
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.launchNow
import eu.kanade.tachiyomi.util.system.loggycat
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import uy.kohesive.injekt.injectLazy

class MangadexLogoutDialog(bundle: Bundle? = null) : DialogController(bundle) {

    val loginHelper: MangaDexLoginHelper by injectLazy()

    private lateinit var binding: PrefAccountLoginBinding
    val preferences: PreferencesHelper by injectLazy()

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return activity!!.materialAlertDialog().apply {
            setTitle(R.string.log_out)
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.log_out) { _, _ ->

                launchNow {
                    runCatching {
                        when (loginHelper.logout()) {
                            true -> activity?.toast(R.string.successfully_logged_out)
                            false -> activity?.toast(R.string.successfully_logged_out)

                        }
                        activity?.toast(R.string.successfully_logged_out)
                        (targetController as? Listener)?.siteLogoutDialogClosed()
                    }.onFailure { e ->
                        loggycat(LogPriority.ERROR, e) { "Error logging out" }
                        activity?.toast(R.string.could_not_log_in)
                    }
                }
            }
        }.create()
    }

    interface Listener {
        fun siteLogoutDialogClosed()
    }
}
