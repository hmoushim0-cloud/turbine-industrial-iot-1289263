package sa.factory.turbineindustriali.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe"

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "Handshake initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "VIB 3-Axis: X=0.8g Y=0.5g Z=1.2g | Acoustic RS-485: 68dB @ 400Hz | HFP Latency: 42ms"))

        while (true) {
            delay(1500)
            val jitter = Random.nextInt(1, 10)
            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", "VIB 3-Axis: X=0.8g Y=0.5g Z=1.2g | Acoustic RS-485: 68dB @ 400Hz | HFP Latency: 42ms | Frame #${System.currentTimeMillis() % 10000} (Jitter: +${jitter}ms)"))
        }
    }
}
