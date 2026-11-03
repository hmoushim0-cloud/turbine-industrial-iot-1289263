# تطبيق مجس التوربينات — برمجه ESP32 ليعمل كجهاز Blue (`turbine-industrial-iot-1289263`)
> منظومة industrial_iot المتكاملة مع ربط 3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe

- **Project ID**: `#1289263`
- **Commercial Archetype**: `industrial_iot`
- **Bound Hardware Peripheral**: `3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe`
- **Package Name**: `sa.factory.turbineindustriali`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)


## [Sidekick Autonomous Update]

### Overview of Completed Features
Sidekick has enhanced the Turbine Industrial IoT platform with an industrial-grade Modbus RTU / RS485 serial binary protocol parser (including polynomial 0xA001 CRC-16 checksum validation) and an ISO 10816-3 mechanical vibration severity and predictive maintenance engine.

### Implemented Modules & Features
1. **Modbus RTU / RS485 Binary Protocol Engine (`ModbusRtuParser.kt`)**:
   - Calculates and validates Modbus CRC-16 checksums (polynomial `0xA001`, initial `0xFFFF`).
   - Encodes Read Holding Registers (`0x03`) and Read Input Registers (`0x04`) query frames.
   - Unpacks binary multi-register response packets into 3-axis acceleration (X, Y, Z in milli-g), combined vibration velocity RMS ($mm/s$), bearing surface temperature ($^\circ C$), and ultrasonic acoustic emission probe levels ($dB$).
   - Automatically drops and flags corrupted frames failing CRC validation.

2. **ISO 10816-3 Vibration Severity & Predictive Maintenance Engine (`Iso10816VibrationEngine.kt`)**:
   - Implements ISO 10816-3 mechanical vibration velocity RMS severity classification across industrial machine categories (Classes I, II, III, IV).
   - Maps vibration velocity into standardized severity zones:
     - Zone A (Good / Optimal baseline commissioning)
     - Zone B (Acceptable for unrestricted continuous operation)
     - Zone C (Unsatisfactory / Warning, maintenance overhaul required within 72 hours)
     - Zone D (Unacceptable / Critical trip threshold, immediate turbine emergency shutdown)
   - Evaluates ultrasonic acoustic noise thresholds ($> 65$ dB) to identify early bearing degradation and hydrodynamic cavitation.

3. **Autonomous Sensor Driver Telemetry Stream (`HardwareSensorDriver.kt`)**:
   - Upgraded to simulate realistic multi-register Modbus RTU response packets with valid CRC-16 bytes.
   - Streams live turbine status frames displaying velocity RMS, ISO severity zone, bearing temp, and automated alarm states.

4. **JUnit 4 Feature Test Suite (`TurbineIndustrialFeatureTest.kt`)**:
   - 6 comprehensive tests covering CRC-16 calculation/validation, Modbus query serialization, binary telemetry decoding, ISO 10816-3 severity classification, acoustic bearing fault detection, and corrupted frame rejection.

### Verification and Test Execution
Run the standalone Kotlin feature tests:
```bash
kotlinc -cp "/usr/share/java/junit4-4.13.2.jar:/usr/share/java/hamcrest-2.2.jar" \
  app/src/main/java/sa/factory/turbineindustriali/model/TurbineIndustrialModels.kt \
  app/src/main/java/sa/factory/turbineindustriali/hardware/ModbusRtuParser.kt \
  app/src/main/java/sa/factory/turbineindustriali/maintenance/Iso10816VibrationEngine.kt \
  app/src/main/java/test/java/sa/factory/turbineindustriali/TurbineIndustrialFeatureTest.kt \
  -d /tmp/turbine_test_bin

java -cp "/tmp/turbine_test_bin:/usr/share/java/junit4-4.13.2.jar:/usr/share/java/hamcrest-2.2.jar:/usr/share/java/kotlin-stdlib.jar" \
  org.junit.runner.JUnitCore sa.factory.turbineindustriali.TurbineIndustrialFeatureTest
```
Result: `OK (6 tests)` passed.
