package br.edu.robotcontroller

import org.junit.Assert.*
import org.junit.Test

class ControlSessionTest {
    @Test fun heldTurnRepeatsEveryTickWithoutNewTouchEvents() {
        for ((steering, expected) in listOf(0.8f to 0xF4, -0.8f to 0xBC)) {
            val link = Link()
            link.session.connected(); link.finish()
            // One input event, followed by five seconds with the finger stationary.
            link.session.input(1f, steering)
            repeat(25) {
                link.time += 200
                link.session.tick()
                assertEquals(expected, link.finish())
            }
            assertEquals(listOf(0) + List(25) { expected }, link.writes)
            // Releasing steering alone resumes straight movement, also periodically.
            link.session.input(1f, 0f)
            repeat(3) {
                link.time += 200
                link.session.tick()
                assertEquals(0xFC, link.finish())
            }
            link.session.input(0f, 0f)
            assertEquals(0, link.finish())
            link.time += 200
            link.session.tick()
            assertEquals(0, link.finish())
        }
    }

    @Test fun releasingMovementStopsHeldTurnEvenDuringPendingWrite() {
        for ((steering, expected) in listOf(1f to 0xF4, -1f to 0xBC)) {
            val link = Link()
            link.session.connected(); link.finish()
            link.session.input(1f, steering)
            repeat(25) {
                link.time += 200; link.session.tick()
                assertEquals(expected, link.finish())
            }
            link.time += 200; link.session.tick()
            link.session.input(0f, steering)
            assertEquals(expected, link.finish())
            assertEquals(0, link.finish())
            repeat(3) {
                link.time += 200; link.session.tick()
                assertEquals(0, link.finish())
            }
        }
    }

    @Test fun releasingMovementStopsAndReleasingSteeringKeepsMovement() {
        for (steering in listOf(1f, -1f)) {
            val link = Link()
            link.connectAndMove(1f)
            link.session.input(1f, steering); link.session.tick(); link.finish()
            link.session.input(0f, steering); link.session.tick()
            assertEquals(0, link.finish())
            link.time += 200
            link.session.input(1f, steering); link.session.tick(); link.finish()
            link.session.input(1f, 0f); link.session.tick()
            assertEquals(0xFC, link.finish())
            link.session.input(0f, 0f)
            assertEquals(0, link.finish())
        }
    }

    @Test fun releasingReverseWhileHoldingSteeringRemainsStopped() {
        val link = Link()
        link.connectAndMove(-1f)
        link.session.input(0f, 1f); link.session.tick()
        assertEquals(0, link.finish())
        link.time = 199; link.session.tick(); assertEquals(0, link.finish())
        link.time = 200; link.session.tick(); assertEquals(0, link.finish())
    }

    @Test fun turnStopAndReconnectDoNotRestoreHeldInput() {
        val link = Link()
        link.session.connected(); link.finish()
        link.session.input(1f, -1f); link.session.tick(); link.finish()
        link.session.stop(); assertEquals(0, link.finish())
        link.time += 200; link.session.tick(); assertEquals(0, link.finish())
        link.session.input(1f, 1f); link.session.tick(); link.finish()
        link.session.disconnected(); link.session.connected()
        assertEquals(0, link.finish())
        link.session.tick(); assertEquals(0, link.finish())
    }

    @Test fun latestSteeringIsSentImmediatelyAfterFiveSecondsForward() {
        val link = Link(); link.connectAndMove(1f)
        repeat(25) { link.time += 200; link.session.tick(); link.finish() }
        link.time += 1
        link.session.input(1f, 1f)
        assertEquals("Turn must not wait for heartbeat", 1, link.pending.size)
        assertEquals(0xF4, link.finish())
        link.session.input(0f, 1f)
        assertEquals(0, link.finish())
    }

    @Test fun busyWriterKeepsOnlyLatestSteeringAndSendsItOnCompletion() {
        val link = Link(); link.connectAndMove(1f)
        link.session.tick() // Forward heartbeat in flight.
        repeat(1000) { link.session.input(1f, if (it % 2 == 0) -1f else 1f) }
        assertEquals(1, link.pending.size)
        assertEquals(0xFC, link.finish())
        assertEquals(1, link.pending.size)
        assertEquals(0xF4, link.finish())
        assertTrue(link.pending.isEmpty())
    }

    @Test fun releaseDuringSlowTransportDoesNotQueueOldTurns() {
        val link = Link(); link.connectAndMove(1f)
        link.session.tick() // Simula escrita que demora 1,5 s.
        repeat(1000) { link.session.input(1f, if (it % 2 == 0) -1f else 1f) }
        link.session.input(0f, 1f)
        link.time += 1500
        assertEquals(1, link.pending.size)
        assertEquals(0xFC, link.finish())
        assertEquals(0, link.finish())
        assertTrue(link.pending.isEmpty())
        assertEquals(listOf(0, 0xFC, 0xFC, 0), link.writes)
    }

    private class Link {
        var time = 0L
        val writes = mutableListOf<Int>()
        val pending = ArrayDeque<Pair<Int, (Boolean) -> Unit>>()
        val session = ControlSession(
            send = { byte, done -> pending.addLast(byte to done) },
            nowMillis = { time }
        )
        fun finish(success: Boolean = true): Int {
            val (byte, done) = pending.removeFirst()
            if (success) writes.add(byte)
            done(success)
            return byte
        }
        fun connectAndMove(movement: Float) {
            session.connected()
            assertEquals(0, finish())
            session.input(movement, 0f)
            session.tick()
            finish()
        }
    }

    @Test fun reversingBothWaysWritesNeutralAndWaitsOnePeriod() {
        for (start in listOf(1f, -1f)) {
            val link = Link()
            link.connectAndMove(start)
            link.session.input(-start, 0f) // No center touch event at all.
            link.session.tick()
            assertEquals(0, link.pending.first().first)
            link.session.tick()
            assertEquals(1, link.pending.size) // Cannot move before zero completes.
            link.finish()
            link.time = 199
            link.session.tick()
            assertTrue(link.pending.isEmpty())
            link.time = 200
            link.session.tick()
            link.finish()
            assertEquals(if (start > 0) listOf(0, 0xFC, 0, 0x6C) else listOf(0, 0x6C, 0, 0xFC), link.writes)
        }
    }

    @Test fun passingCenterWhileWriteIsBusyDoesNotLoseStop() {
        val link = Link()
        link.session.connected(); link.finish()
        link.session.input(1f, 0f); link.session.tick()
        link.session.input(0f, 0f) // Release while forward write is still pending.
        link.session.input(-1f, 0f)
        assertEquals(1, link.pending.size)
        assertEquals(0xFC, link.finish())
        assertEquals(0, link.pending.first().first)
        link.finish()
        link.time = 200
        link.session.tick(); link.finish()
        assertEquals(listOf(0, 0xFC, 0, 0x6C), link.writes)
    }

    @Test fun failedNeutralMustBeRetriedBeforeReverse() {
        val link = Link()
        link.connectAndMove(1f)
        link.session.input(-1f, 0f); link.session.tick()
        assertEquals(0, link.finish(false))
        link.time = 1000
        link.session.tick()
        assertEquals(0, link.finish())
        link.time = 1199; link.session.tick(); assertTrue(link.pending.isEmpty())
        link.time = 1200; link.session.tick(); link.finish()
        assertEquals(listOf(0, 0xFC, 0, 0x6C), link.writes)
    }

    @Test fun changingMindDuringNeutralUsesLatestInput() {
        val link = Link()
        link.connectAndMove(1f)
        link.session.input(-1f, 0f); link.session.tick()
        link.session.input(1f, 1f)
        link.finish()
        link.time = 200; link.session.tick(); link.finish()
        assertEquals(listOf(0, 0xFC, 0, 0xF4), link.writes)
    }

    @Test fun stopDuringMovingWriteAndLateCallbackAfterReconnect() {
        val link = Link()
        link.connectAndMove(1f)
        link.session.input(-1f, 0f); link.session.tick()
        val oldCallback = link.pending.removeFirst().second
        link.session.disconnected(); link.session.connected()
        oldCallback(true)
        link.session.input(-1f, 0f); link.session.tick()
        assertEquals(1, link.pending.size)
        assertEquals(0, link.finish())
        // A entrada nova da conexão atual começa assim que o zero termina.
        assertEquals(0x6C, link.pending.first().first)
        link.session.stop(); assertEquals(0x6C, link.finish())
        assertEquals(0, link.finish()) // Parada pendente tem prioridade.
        link.session.tick(); assertEquals(0, link.finish())
        assertEquals(listOf(0x6C, 0, 0), link.writes.takeLast(3))
    }

    @Test fun changingSpeedOrSteeringDoesNotInsertNeutral() {
        val link = Link()
        link.connectAndMove(1f)
        link.session.input(1f, 1f); link.session.tick(); link.finish()
        link.session.input(1f, -1f); link.session.tick(); link.finish()
        assertEquals(listOf(0, 0xFC, 0xF4, 0xBC), link.writes)
    }
}
