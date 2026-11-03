package sa.factory.turbineindustriali

import sa.factory.turbineindustriali.hardware.ModbusRtuParser
import sa.factory.turbineindustriali.maintenance.Iso10816VibrationEngine
import sa.factory.turbineindustriali.model.MachineClass
import sa.factory.turbineindustriali.model.VibrationSeverityZone
import org.junit.Assert.*
import org.junit.Test

/**
 * Feature Test Suite for Turbine Industrial IoT Modbus RTU & ISO 10816 Vibration Engine.
 * Zero-Emoji Invariant strictly maintained.
 */
class TurbineIndustrialFeatureTest {

    @Test
    fun testModbusCrc16CalculationAndValidation() {
        // Standard Modbus query: Slave 1, Function 3, Start 0, Count 10
        val queryData = byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x0A)
        val crc = ModbusRtuParser.calculateCrc16(queryData, queryData.size)

        // CRC for 01 03 00 00 00 0A is 0xCDC5 -> Low: C5, High: CD
        assertEquals(0xC5, crc and 0xFF)
        assertEquals(0xCD, (crc ushr 8) and 0xFF)

        val fullFrame = ByteArray(8)
        System.arraycopy(queryData, 0, fullFrame, 0, 6)
        fullFrame[6] = (crc and 0xFF).toByte()
        fullFrame[7] = ((crc ushr 8) and 0xFF).toByte()

        assertTrue("Valid Modbus CRC must be accepted", ModbusRtuParser.validateCrc(fullFrame))

        // Negative test: Tamper byte
        fullFrame[3] = 0x05.toByte()
        assertFalse("Tampered Modbus frame must fail CRC validation", ModbusRtuParser.validateCrc(fullFrame))
    }

    @Test
    fun testModbusQueryEncoding() {
        val query = ModbusRtuParser.encodeReadQuery(slaveAddress = 2, startRegister = 0x0064, numRegisters = 6)
        assertEquals(8, query.size)
        assertEquals(2.toByte(), query[0])
        assertEquals(0x03.toByte(), query[1])
        assertEquals(0x00.toByte(), query[2])
        assertEquals(0x64.toByte(), query[3])
        assertEquals(0x00.toByte(), query[4])
        assertEquals(0x06.toByte(), query[5])
        assertTrue("Generated query must contain valid CRC", ModbusRtuParser.validateCrc(query))
    }

    @Test
    fun testModbusResponseDecodingToVibrationTelemetry() {
        // Construct frame with:
        // Slave 1, Func 3, Count 12 bytes
        // X = 250 (0.25g), Y = 180 (0.18g), Z = 1010 (1.01g)
        // Vel = 210 (2.10 mm/s), Temp = 520 (52.0 C), Acoustic = 450 (45.0 dB)
        val frame = ByteArray(3 + 12 + 2)
        frame[0] = 0x01.toByte()
        frame[1] = 0x03.toByte()
        frame[2] = 0x0C.toByte()

        // X: 250 -> 0x00FA
        frame[3] = 0x00.toByte(); frame[4] = 0xFA.toByte()
        // Y: 180 -> 0x00B4
        frame[5] = 0x00.toByte(); frame[6] = 0xB4.toByte()
        // Z: 1010 -> 0x03F2
        frame[7] = 0x03.toByte(); frame[8] = 0xF2.toByte()
        // Vel: 210 -> 0x00D2
        frame[9] = 0x00.toByte(); frame[10] = 0xD2.toByte()
        // Temp: 520 -> 0x0208
        frame[11] = 0x02.toByte(); frame[12] = 0x08.toByte()
        // Acoustic: 450 -> 0x01C2
        frame[13] = 0x01.toByte(); frame[14] = 0xC2.toByte()

        val crc = ModbusRtuParser.calculateCrc16(frame, 15)
        frame[15] = (crc and 0xFF).toByte()
        frame[16] = ((crc ushr 8) and 0xFF).toByte()

        val packet = ModbusRtuParser.decodeResponseFrame(frame)

        assertTrue("CRC must be valid", packet.crcValid)
        assertEquals("Slave address must be 1", 1, packet.slaveAddress)
        assertEquals("X-axis accel must be 0.25g", 0.25, packet.xAxisAccelG, 0.001)
        assertEquals("Y-axis accel must be 0.18g", 0.18, packet.yAxisAccelG, 0.001)
        assertEquals("Z-axis accel must be 1.01g", 1.01, packet.zAxisAccelG, 0.001)
        assertEquals("Vibration velocity RMS must be 2.10 mm/s", 2.10, packet.velocityRmsMmSec, 0.01)
        assertEquals("Bearing temp must be 52.0 C", 52.0, packet.bearingTempCelsius, 0.1)
        assertEquals("Acoustic noise must be 45.0 dB", 45.0, packet.acousticProbeDb, 0.1)
    }

    @Test
    fun testIso10816SeverityZonesEvaluation() {
        // For Class III Large Rigid Foundation Turbines:
        // <= 2.3 mm/s: Zone A
        // 2.3 - 4.5 mm/s: Zone B
        // 4.5 - 11.2 mm/s: Zone C
        // > 11.2 mm/s: Zone D
        assertEquals(VibrationSeverityZone.ZONE_A_GOOD, Iso10816VibrationEngine.evaluateSeverityZone(1.80, MachineClass.CLASS_III_LARGE_RIGID))
        assertEquals(VibrationSeverityZone.ZONE_B_ACCEPTABLE, Iso10816VibrationEngine.evaluateSeverityZone(3.50, MachineClass.CLASS_III_LARGE_RIGID))
        assertEquals(VibrationSeverityZone.ZONE_C_UNSATISFACTORY, Iso10816VibrationEngine.evaluateSeverityZone(7.20, MachineClass.CLASS_III_LARGE_RIGID))
        assertEquals(VibrationSeverityZone.ZONE_D_UNACCEPTABLE, Iso10816VibrationEngine.evaluateSeverityZone(14.50, MachineClass.CLASS_III_LARGE_RIGID))
    }

    @Test
    fun testAcousticCavitationAlarmDetection() {
        val frame = ByteArray(17)
        frame[0] = 0x01.toByte()
        frame[1] = 0x03.toByte()
        frame[2] = 0x0C.toByte()
        // Vel = 1.50 mm/s (150 -> 0x0096)
        frame[9] = 0x00.toByte(); frame[10] = 0x96.toByte()
        // Acoustic = 74.0 dB (740 -> 0x02E4)
        frame[13] = 0x02.toByte(); frame[14] = 0xE4.toByte()

        val crc = ModbusRtuParser.calculateCrc16(frame, 15)
        frame[15] = (crc and 0xFF).toByte()
        frame[16] = ((crc ushr 8) and 0xFF).toByte()

        val packet = ModbusRtuParser.decodeResponseFrame(frame)
        val report = Iso10816VibrationEngine.evaluateVibrationTelemetry(packet, MachineClass.CLASS_III_LARGE_RIGID)

        assertEquals("Severity zone must be Zone A", VibrationSeverityZone.ZONE_A_GOOD, report.severityZone)
        assertTrue("Acoustic anomaly flag must be triggered for 74 dB", report.acousticAnomaly)
        assertTrue("Recommendation must mention acoustic noise", report.recommendation.contains("acoustic"))
    }

    @Test
    fun testCorruptedModbusFrameRejection() {
        val corruptedFrame = byteArrayOf(0x01, 0x03, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x12, 0x34)
        val packet = ModbusRtuParser.decodeResponseFrame(corruptedFrame)
        assertFalse("Corrupted frame must report crcValid=false", packet.crcValid)
        assertEquals(0.0, packet.velocityRmsMmSec, 0.001)
    }
}
