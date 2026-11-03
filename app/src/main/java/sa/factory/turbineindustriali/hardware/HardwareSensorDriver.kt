package sa.factory.turbineindustriali.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.turbineindustriali.maintenance.Iso10816VibrationEngine
import sa.factory.turbineindustriali.model.MachineClass
import java.util.Locale

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe"

    // Real Modbus RTU responses with precalculated CRC-16 (Low byte, then High byte):
    // Frame format: [01 03 0C] [X_accel: 2B] [Y_accel: 2B] [Z_accel: 2B] [Vel_RMS: 2B] [Temp: 2B] [Acoustic: 2B] [CRC_Low] [CRC_High]
    // 1) X=0.15g (150 -> 0x0096), Y=0.12g (120 -> 0x0078), Z=1.02g (1020 -> 0x03FC), Vel=1.45 mm/s (145 -> 0x0091), Temp=45.2C (452 -> 0x01C4), Acoustic=42.0dB (420 -> 0x01A4)
    // 2) Elevated vibration: Vel=5.80 mm/s (580 -> 0x0244), Temp=58.5C (585 -> 0x0249), Acoustic=68.5dB (685 -> 0x02AD)
    private val simulatedModbusFrames: List<ByteArray> by lazy {
        listOf(
            createModbusFrame(150, 120, 1020, 145, 452, 420), // Zone A (Normal)
            createModbusFrame(220, 180, 1050, 280, 485, 450), // Zone B (Acceptable)
            createModbusFrame(480, 390, 1150, 580, 585, 685), // Zone C (Warning, Acoustic Anomaly)
            createModbusFrame(920, 850, 1420, 1250, 720, 780)  // Zone D (Trip / Emergency)
        )
    }

    private fun createModbusFrame(x: Int, y: Int, z: Int, vel: Int, temp: Int, acoustic: Int): ByteArray {
        val frame = ByteArray(3 + 12 + 2)
        frame[0] = 0x01.toByte() // Slave Address
        frame[1] = 0x03.toByte() // Function Code (Read Holding)
        frame[2] = 0x0C.toByte() // Byte count: 12 bytes

        frame[3] = ((x ushr 8) and 0xFF).toByte()
        frame[4] = (x and 0xFF).toByte()
        frame[5] = ((y ushr 8) and 0xFF).toByte()
        frame[6] = (y and 0xFF).toByte()
        frame[7] = ((z ushr 8) and 0xFF).toByte()
        frame[8] = (z and 0xFF).toByte()

        frame[9] = ((vel ushr 8) and 0xFF).toByte()
        frame[10] = (vel and 0xFF).toByte()
        frame[11] = ((temp ushr 8) and 0xFF).toByte()
        frame[12] = (temp and 0xFF).toByte()
        frame[13] = ((acoustic ushr 8) and 0xFF).toByte()
        frame[14] = (acoustic and 0xFF).toByte()

        val crc = ModbusRtuParser.calculateCrc16(frame, 15)
        frame[15] = (crc and 0xFF).toByte()
        frame[16] = ((crc ushr 8) and 0xFF).toByte()

        return frame
    }

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "RS485 Serial Baud=115200 8N1 | Modbus RTU initializing..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "Modbus RS485 Link UP: Slave 0x01 responding | ISO 10816 Engine active"))

        var frameIndex = 0
        while (true) {
            delay(1500)
            val rawBytes = simulatedModbusFrames[frameIndex % simulatedModbusFrames.size]
            val packet = ModbusRtuParser.decodeResponseFrame(rawBytes)
            val report = Iso10816VibrationEngine.evaluateVibrationTelemetry(packet, MachineClass.CLASS_III_LARGE_RIGID)

            val formattedPayload = String.format(
                Locale.US,
                "Modbus RTU: Vel: %.2f mm/s [%s] | Accel: X=%.2fg Y=%.2fg Z=%.2fg | Temp: %.1fC | Noise: %.1fdB | Status: %s",
                packet.velocityRmsMmSec,
                report.severityZone.name,
                packet.xAxisAccelG,
                packet.yAxisAccelG,
                packet.zAxisAccelG,
                packet.bearingTempCelsius,
                packet.acousticProbeDb,
                report.alarmStatus
            )

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", formattedPayload))
            frameIndex++
        }
    }
}
