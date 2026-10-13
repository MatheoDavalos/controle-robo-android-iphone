package br.edu.robotcontroller

import kotlin.math.abs

/** Platform-independent v1 protocol. Positive movement = forward = direction bit 1. */
object RobotProtocol {
    const val DEAD_ZONE = 0.08f
    data class Wheels(val forward: Boolean, val right: Int, val left: Int) {
        init { require(right in 0..3 && left in 0..3) }
    }
    fun encode(wheels: Wheels): Int {
        if (wheels.right == 0 && wheels.left == 0) return 0
        val direction = if (wheels.forward) 0x90 else 0
        // Firmware contract: LEFT in bits 7–5, RIGHT in bits 4–2.
        return direction or (wheels.left shl 5) or (wheels.right shl 2)
    }
    fun decode(byte: Int): Wheels {
        require(byte in 0..255 && byte and 3 == 0) { "Byte inválido ou bits reservados diferentes de zero" }
        require((byte shr 7 and 1) == (byte shr 4 and 1)) { "Flags opostas não são permitidas" }
        return Wheels(byte and 0x80 != 0, byte shr 2 and 3, byte shr 5 and 3)
    }
    private fun normalized(value: Float): Float {
        require(value.isFinite())
        val magnitude = abs(value.coerceIn(-1f, 1f))
        return if (magnitude <= DEAD_ZONE) 0f else (magnitude - DEAD_ZONE) / (1f - DEAD_ZONE)
    }
    fun level(value: Float): Int {
        require(value.isFinite())
        return when {
            value <= 0f -> 0
            value <= 1f / 3f -> 1
            value <= 2f / 3f -> 2
            else -> 3
        }
    }
    fun mix(movement: Float, steering: Float): Wheels {
        val magnitude = normalized(movement)
        val direction = normalized(steering) * if (steering < 0) -1 else 1
        // Steering requires throttle. During movement, neither wheel drags at level zero.
        if (magnitude == 0f) return Wheels(movement > 0, 0, 0)
        return Wheels(movement > 0, level((magnitude * (1 - direction)).coerceIn(0f, 1f)).coerceAtLeast(1),
            level((magnitude * (1 + direction)).coerceIn(0f, 1f)).coerceAtLeast(1))
    }
    fun command(movement: Float, steering: Float) = encode(mix(movement, steering))
    fun describe(movement: Float, steering: Float): String {
        val wheels = mix(movement, steering)
        val byte = encode(wheels)
        return "Movimento: %.2f · Direção: %.2f\nRD: %d · RE: %d · %s\n0x%02X · %s".format(
            movement, steering, wheels.right, wheels.left, if (byte == 0) "parado" else if (wheels.forward) "frente" else "trás",
            byte, byte.toString(2).padStart(8, '0'))
    }
}

/** Only the latest input is retained; no command queue survives a reconnect. */
class ControlSession(
    private val send: (Int, (Boolean) -> Unit) -> Unit,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val trace: (String) -> Unit = {}
) {
    var connected = false; private set
    var movement = 0f; private set
    var steering = 0f; private set
    private var generation = 0
    private var inFlight = false
    private var lastWritten: Int? = null
    private var stopRequired = true
    private var neutralUntil = 0L

    fun connected() {
        disconnected()
        connected = true
        transmit(0)
    }
    fun disconnected() {
        generation++
        connected = false
        movement = 0f
        steering = 0f
        inFlight = false
        lastWritten = null
        stopRequired = true
        neutralUntil = 0L
    }
    fun input(movement: Float, steering: Float) {
        if (!connected) return
        require(movement.isFinite() && steering.isFinite())
        val previous = RobotProtocol.command(this.movement, this.steering)
        this.movement = movement; this.steering = steering
        val desired = RobotProtocol.command(movement, steering)
        if (desired != previous) trace("INPUT t=${nowMillis()} byte=$desired")
        if (previous != 0 && desired == 0) stop()
        else if (desired != previous) tick() // Mudanças não esperam o heartbeat de 200 ms.
    }
    fun stop() {
        trace("STOP t=${nowMillis()}")
        movement = 0f
        steering = 0f
        stopRequired = true
        if (connected && !inFlight) transmit(0)
    }
    fun tick() {
        if (!connected || inFlight) return
        val desired = RobotProtocol.command(movement, steering)
        val previous = lastWritten
        val reversing = previous != null && previous != 0 && desired != 0 &&
            (previous and 0x80) != (desired and 0x80)
        if (stopRequired || previous == null || reversing) {
            transmit(0)
        } else if (desired == 0 || nowMillis() >= neutralUntil) {
            transmit(desired)
        }
    }
    private fun transmit(value: Int) {
        val token = generation
        inFlight = true
        trace("TX t=${nowMillis()} byte=$value")
        send(value) { success ->
            // Callback runs on the same thread as input/tick. Old sessions cannot re-arm.
            if (token == generation && connected) {
                inFlight = false
                trace("DONE t=${nowMillis()} byte=$value ok=$success")
                if (success) {
                    if (value == 0) {
                        // Hold neutral for at least one transmission period after a moving byte.
                        if (lastWritten != null && lastWritten != 0) neutralUntil = nowMillis() + 200
                        stopRequired = false
                    }
                    lastWritten = value
                    // A release/stop while a write was active must not be lost.
                    if (stopRequired && value != 0) transmit(0)
                    else if (RobotProtocol.command(movement, steering) != value) tick()
                } else {
                    // Retry neutral before any new movement; a rejected write proves nothing.
                    stopRequired = true
                }
            }
        }
    }
}
