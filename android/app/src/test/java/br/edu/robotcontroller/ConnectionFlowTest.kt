package br.edu.robotcontroller

import org.junit.Assert.*
import org.junit.Test

class ConnectionFlowTest {
    @Test fun freshLaunchAlwaysStartsAtStepOne() {
        val flow = ConnectionFlow()
        assertFalse(flow.selectionReached)
        assertFalse(flow.takeRefreshOnResume())
    }
    @Test fun settingsButtonAdvancesAndRequestsOneAutomaticRefresh() {
        val flow = ConnectionFlow()
        flow.openSettings()
        assertTrue(flow.selectionReached)
        assertTrue(flow.takeRefreshOnResume())
        assertFalse(flow.takeRefreshOnResume()) // Permission dialog returning cannot ask again forever.
        assertTrue(flow.selectionReached)
    }
    @Test fun recreationWhileSettingsAreOpenPreservesPendingRefresh() {
        val flow = ConnectionFlow().apply { openSettings() }
        val restored = ConnectionFlow(flow.selectionReached, flow.hasPendingRefresh())
        assertTrue(restored.selectionReached)
        assertTrue(restored.takeRefreshOnResume())
        restored.restart()
        assertFalse(restored.selectionReached)
        assertFalse(restored.takeRefreshOnResume())
        restored.openSettings()
        assertTrue(restored.takeRefreshOnResume())
    }
}
