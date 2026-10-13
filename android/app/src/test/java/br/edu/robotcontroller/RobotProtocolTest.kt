package br.edu.robotcontroller

import org.junit.Assert.*
import org.junit.Test

class RobotProtocolTest {
    @Test fun rightTurnPutsLowSpeedInRightBitsAndHighSpeedInLeftBits() {
        val rightTurn = RobotProtocol.command(1f, 0.8f)
        assertEquals(1, (rightTurn shr 2) and 3)
        assertEquals(3, (rightTurn shr 5) and 3)
        val leftTurn = RobotProtocol.command(1f, -0.8f)
        assertEquals(3, (leftTurn shr 2) and 3)
        assertEquals(1, (leftTurn shr 5) and 3)
    }

    @Test fun correctedSpecificationExamples() {
        listOf(Triple(RobotProtocol.Wheels(true, 3, 3), 0xFC, "frente"),
            Triple(RobotProtocol.Wheels(false, 3, 3), 0x6C, "trás"),
            Triple(RobotProtocol.Wheels(true, 3, 1), 0xBC, "curva"),
            Triple(RobotProtocol.Wheels(false, 1, 2), 0x44, "ré curva")).forEach {
            assertEquals(it.third, it.second, RobotProtocol.encode(it.first))
            assertEquals(it.first, RobotProtocol.decode(it.second))
        }
    }
    @Test fun allWheelStatesRespectReservedBitsAndPolarity() {
        for (forward in listOf(false, true)) for (right in 0..3) for (left in 0..3) {
            val wheels = RobotProtocol.Wheels(forward, right, left)
            val byte = RobotProtocol.encode(wheels)
            assertEquals(0, byte and 3)
            assertEquals(byte shr 7 and 1, byte shr 4 and 1)
            assertEquals(right, RobotProtocol.decode(byte).right)
            assertEquals(left, RobotProtocol.decode(byte).left)
            if (right + left == 0) assertEquals(0, byte)
        }
    }
    @Test fun fullSteeringAndDeadZone() {
        assertEquals(0xF4, RobotProtocol.command(1f, 1f))
        assertEquals(0xBC, RobotProtocol.command(1f, -1f))
        assertEquals(0x64, RobotProtocol.command(-1f, 1f))
        assertEquals(0x2C, RobotProtocol.command(-1f, -1f))
        for (direction in -100..100) {
            assertEquals(0, RobotProtocol.command(0.08f, direction / 100f))
            assertEquals(0, RobotProtocol.command(-0.08f, direction / 100f))
        }
        assertEquals(1, RobotProtocol.level(1f/3f))
        assertEquals(2, RobotProtocol.level(2f/3f))
    }
    @Test fun steeringAloneNeverMovesEitherWheel() {
        for (movement in listOf(-0.08f, 0f, 0.08f)) {
            for (steering in -100..100) {
                assertEquals(0, RobotProtocol.command(movement, steering / 100f))
            }
        }
        assertTrue(RobotProtocol.describe(0f, 1f).contains("parado"))
    }

    @Test fun innerWheelStaysAtLeastLowInBothDirectionsAndAtEveryThrottleLevel() {
        for (m in -100..100) for (d in -100..100) {
            val wheels = RobotProtocol.mix(m / 100f, d / 100f)
            if (kotlin.math.abs(m) > 8) {
                assertTrue(wheels.right in 1..3)
                assertTrue(wheels.left in 1..3)
                assertEquals(m > 0, wheels.forward)
            }
        }
        assertEquals(RobotProtocol.Wheels(true, 1, 2), RobotProtocol.mix(0.3f, 1f))
        assertEquals(RobotProtocol.Wheels(true, 2, 1), RobotProtocol.mix(0.3f, -1f))
        assertEquals(RobotProtocol.Wheels(false, 1, 2), RobotProtocol.mix(-0.3f, 1f))
        assertEquals(RobotProtocol.Wheels(false, 2, 1), RobotProtocol.mix(-0.3f, -1f))
    }

    @Test fun mirroredSteeringSwapsWheelMagnitudes() {
        for (m in -100..100) for (d in -100..100) {
            val a = RobotProtocol.mix(m/100f, d/100f)
            val b = RobotProtocol.mix(m/100f, -d/100f)
            assertEquals(a.right, b.left)
            assertEquals(a.left, b.right)
        }
    }
    @Test fun reconnectAndReleaseDoNotReplayMovement() {
        val sent = mutableListOf<Int>()
        val session = ControlSession(send = { value, complete -> sent.add(value); complete(true) })
        session.input(1f, 0f); session.tick(); assertTrue(sent.isEmpty())
        session.connected(); assertEquals(listOf(0), sent)
        session.input(1f, 0f); session.tick(); assertEquals(0xFC, sent.last())
        session.input(0f, 0f); assertEquals(0, sent.last())
        session.input(-1f, 0f); session.disconnected()
        val count = sent.size; session.tick(); assertEquals(count, sent.size)
        session.connected(); session.tick(); assertEquals(listOf(0, 0), sent.takeLast(2))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectMixedFlags() { RobotProtocol.decode(0x80) }
    @Test(expected = IllegalArgumentException::class) fun rejectReservedBits() { RobotProtocol.decode(0x01) }
    @Test(expected = IllegalArgumentException::class) fun rejectNan() { RobotProtocol.command(Float.NaN, 0f) }
}
