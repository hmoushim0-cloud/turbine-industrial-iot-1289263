package sa.factory.turbineindustriali.hardware

import sa.factory.turbineindustriali.model.TriAxialVibrationPacket
import java.util.Locale

/**
 * High-Precision Modbus RTU / RS485 Industrial Binary Protocol Parser.
 * Compliant with Modbus-IDA and IEC 61158 industrial automation standards.
 * Zero-Emoji Invariant strictly maintained.
 */
object ModbusRtuParser {

    const val MODBUS_READ_HOLDING_REGISTERS: Byte = 0x03
    const val MODBUS_READ_INPUT_REGISTERS: Byte = 0x04

    /**
     * Calculates Modbus CRC-16 (polynomial 0xA001, initial value 0xFFFF).
     */
    fun calculateCrc16(bytes: ByteArray, length: Int = bytes.size): Int {
        var crc = 0xFFFF
        for (i in 0 until length) {
            crc = crc xor (bytes[i].toInt() and 0xFF)
            for (j in 0 until 8) {
                crc = if ((crc and 0x0001) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
            }
        }
        return crc and 0xFFFF
    }

    /**
     * Validates CRC-16 of an incoming Modbus RTU binary frame.
     * The last two bytes are expected to be CRC-16 (Low byte first, then High byte).
     */
    fun validateCrc(frame: ByteArray): Boolean {
        if (frame.size < 4) return false
        val dataLen = frame.size - 2
        val computedCrc = calculateCrc16(frame, dataLen)

        val receivedLow = frame[dataLen].toInt() and 0xFF
        val receivedHigh = frame[dataLen + 1].toInt() and 0xFF
        val receivedCrc = (receivedHigh shl 8) or receivedLow

        return computedCrc == receivedCrc
    }

    /**
     * Encodes a Modbus RTU Read Holding Registers query frame with trailing CRC-16.
     */
    fun encodeReadQuery(slaveAddress: Int, startRegister: Int, numRegisters: Int): ByteArray {
        val frame = ByteArray(8)
        frame[0] = (slaveAddress and 0xFF).toByte()
        frame[1] = MODBUS_READ_HOLDING_REGISTERS
        frame[2] = ((startRegister ushr 8) and 0xFF).toByte()
        frame[3] = (startRegister and 0xFF).toByte()
        frame[4] = ((numRegisters ushr 8) and 0xFF).toByte()
        frame[5] = (numRegisters and 0xFF).toByte()

        val crc = calculateCrc16(frame, 6)
        frame[6] = (crc and 0xFF).toByte() // CRC Low
        frame[7] = ((crc ushr 8) and 0xFF).toByte() // CRC High

        return frame
    }

    /**
     * Decodes a binary Modbus RTU response packet into TriAxialVibrationPacket telemetry.
     */
    fun decodeResponseFrame(frame: ByteArray): TriAxialVibrationPacket {
        val isValidCrc = validateCrc(frame)
        val hexPayload = bytesToHex(frame)

        if (!isValidCrc || frame.size < 7) {
            return TriAxialVibrationPacket(
                slaveAddress = if (frame.isNotEmpty()) frame[0].toInt() and 0xFF else 0,
                xAxisAccelG = 0.0,
                yAxisAccelG = 0.0,
                zAxisAccelG = 0.0,
                velocityRmsMmSec = 0.0,
                bearingTempCelsius = 0.0,
                acousticProbeDb = 0.0,
                crcValid = false,
                rawFrameHex = hexPayload
            )
        }

        val slaveAddr = frame[0].toInt() and 0xFF
        val byteCount = frame[2].toInt() and 0xFF

        // We expect at least 6 registers (12 bytes): X, Y, Z Accel, Velocity, Temp, Acoustic
        var xG = 0.0
        var yG = 0.0
        var zG = 0.0
        var velRms = 0.0
        var tempC = 25.0
        var acousticDb = 30.0

        if (byteCount >= 12 && frame.size >= 3 + 12 + 2) {
            val rawX = ((frame[3].toInt() shl 8) or (frame[4].toInt() and 0xFF)).toShort()
            val rawY = ((frame[5].toInt() shl 8) or (frame[6].toInt() and 0xFF)).toShort()
            val rawZ = ((frame[7].toInt() shl 8) or (frame[8].toInt() and 0xFF)).toShort()
            val rawVel = ((frame[9].toInt() shl 8) or (frame[10].toInt() and 0xFF)).toShort()
            val rawTemp = ((frame[11].toInt() shl 8) or (frame[12].toInt() and 0xFF)).toShort()
            val rawAcoustic = ((frame[13].toInt() shl 8) or (frame[14].toInt() and 0xFF)).toShort()

            xG = rawX.toDouble() * 0.001
            yG = rawY.toDouble() * 0.001
            zG = rawZ.toDouble() * 0.001
            velRms = rawVel.toDouble() * 0.01
            tempC = rawTemp.toDouble() * 0.1
            acousticDb = rawAcoustic.toDouble() * 0.1
        }

        return TriAxialVibrationPacket(
            slaveAddress = slaveAddr,
            xAxisAccelG = xG,
            yAxisAccelG = yG,
            zAxisAccelG = zG,
            velocityRmsMmSec = velRms,
            bearingTempCelsius = tempC,
            acousticProbeDb = acousticDb,
            crcValid = true,
            rawFrameHex = hexPayload
        )
    }

    /**
     * Decodes a hex string payload into TriAxialVibrationPacket.
     */
    fun decodeHexPayload(hexString: String): TriAxialVibrationPacket {
        val clean = hexString.replace(" ", "").trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val h = Character.digit(clean[i], 16)
            val l = Character.digit(clean[i + 1], 16)
            data[i / 2] = ((h shl 4) + l).toByte()
            i += 2
        }
        return decodeResponseFrame(data)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format(Locale.US, "%02X", b))
        }
        return sb.toString()
    }
}
