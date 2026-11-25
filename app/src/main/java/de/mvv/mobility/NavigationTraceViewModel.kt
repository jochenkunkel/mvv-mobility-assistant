package de.mvv.mobility

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import de.mvv.mobility.data.TraceFix
import de.mvv.mobility.data.WalkedTrace

/**
 * Activity-scoped owner for the recorded walking history.
 *
 * ViewModels survive configuration changes; this state deliberately does not
 * survive process death and is not serialized into Android saved-state Bundles.
 * Route changes must not call [clearSession]; only an explicit session end does.
 */
class NavigationTraceViewModel : ViewModel() {
    var walkedTrace by mutableStateOf(WalkedTrace())
        private set

    fun acceptFix(fix: TraceFix) {
        walkedTrace = walkedTrace.accept(fix).retainLast(1500)
    }

    fun clearSession() {
        walkedTrace = WalkedTrace()
    }
}
