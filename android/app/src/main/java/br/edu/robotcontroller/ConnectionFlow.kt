package br.edu.robotcontroller

/** The setup step is navigation state, independent of whether Bluetooth is already enabled. */
class ConnectionFlow(var selectionReached: Boolean = false, private var refreshPending: Boolean = false) {
    fun openSettings() { selectionReached = true; refreshPending = true }
    fun restart() { selectionReached = false; refreshPending = false }
    fun takeRefreshOnResume(): Boolean = refreshPending.also { refreshPending = false }
    fun hasPendingRefresh() = refreshPending
}
