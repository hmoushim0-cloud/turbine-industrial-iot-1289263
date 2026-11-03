package sa.factory.turbineindustriali.maintenance

import sa.factory.turbineindustriali.model.MachineClass
import sa.factory.turbineindustriali.model.PredictiveMaintenanceReport
import sa.factory.turbineindustriali.model.TriAxialVibrationPacket
import sa.factory.turbineindustriali.model.VibrationSeverityZone

/**
 * ISO 10816-3 Industrial Vibration Severity & Predictive Maintenance Engine.
 * Evaluates mechanical vibration velocity RMS (mm/s) and acoustic emissions for industrial turbomachinery.
 * Zero-Emoji Invariant strictly maintained.
 */
object Iso10816VibrationEngine {

    const val ACOUSTIC_ULTRASONIC_ALARM_DB = 65.0

    /**
     * Resolves vibration severity zone based on ISO 10816-3 standards.
     */
    fun evaluateSeverityZone(
        velocityRmsMmSec: Double,
        machineClass: MachineClass = MachineClass.CLASS_III_LARGE_RIGID
    ): VibrationSeverityZone {
        return when (machineClass) {
            MachineClass.CLASS_I_SMALL -> {
                when {
                    velocityRmsMmSec <= 0.71 -> VibrationSeverityZone.ZONE_A_GOOD
                    velocityRmsMmSec <= 1.80 -> VibrationSeverityZone.ZONE_B_ACCEPTABLE
                    velocityRmsMmSec <= 4.50 -> VibrationSeverityZone.ZONE_C_UNSATISFACTORY
                    else -> VibrationSeverityZone.ZONE_D_UNACCEPTABLE
                }
            }
            MachineClass.CLASS_II_MEDIUM -> {
                when {
                    velocityRmsMmSec <= 1.12 -> VibrationSeverityZone.ZONE_A_GOOD
                    velocityRmsMmSec <= 2.80 -> VibrationSeverityZone.ZONE_B_ACCEPTABLE
                    velocityRmsMmSec <= 7.10 -> VibrationSeverityZone.ZONE_C_UNSATISFACTORY
                    else -> VibrationSeverityZone.ZONE_D_UNACCEPTABLE
                }
            }
            MachineClass.CLASS_III_LARGE_RIGID -> {
                when {
                    velocityRmsMmSec <= 2.30 -> VibrationSeverityZone.ZONE_A_GOOD
                    velocityRmsMmSec <= 4.50 -> VibrationSeverityZone.ZONE_B_ACCEPTABLE
                    velocityRmsMmSec <= 11.20 -> VibrationSeverityZone.ZONE_C_UNSATISFACTORY
                    else -> VibrationSeverityZone.ZONE_D_UNACCEPTABLE
                }
            }
            MachineClass.CLASS_IV_LARGE_FLEXIBLE -> {
                when {
                    velocityRmsMmSec <= 3.50 -> VibrationSeverityZone.ZONE_A_GOOD
                    velocityRmsMmSec <= 7.10 -> VibrationSeverityZone.ZONE_B_ACCEPTABLE
                    velocityRmsMmSec <= 18.00 -> VibrationSeverityZone.ZONE_C_UNSATISFACTORY
                    else -> VibrationSeverityZone.ZONE_D_UNACCEPTABLE
                }
            }
        }
    }

    /**
     * Generates a comprehensive predictive maintenance evaluation from live telemetry.
     */
    fun evaluateVibrationTelemetry(
        packet: TriAxialVibrationPacket,
        machineClass: MachineClass = MachineClass.CLASS_III_LARGE_RIGID
    ): PredictiveMaintenanceReport {
        val zone = evaluateSeverityZone(packet.velocityRmsMmSec, machineClass)
        val acousticAnomaly = packet.acousticProbeDb > ACOUSTIC_ULTRASONIC_ALARM_DB

        val alarmStatus: String
        val recommendation: String

        when (zone) {
            VibrationSeverityZone.ZONE_A_GOOD -> {
                alarmStatus = "NORMAL_OPTIMAL"
                recommendation = if (acousticAnomaly) {
                    "Vibration normal but acoustic noise elevated. Inspect bearing lubrication."
                } else {
                    "Turbine operating within optimal ISO limits. No maintenance action required."
                }
            }
            VibrationSeverityZone.ZONE_B_ACCEPTABLE -> {
                alarmStatus = "ACCEPTABLE_CONTINUOUS"
                recommendation = if (acousticAnomaly) {
                    "Acoustic threshold exceeded. Schedule ultrasonic probe diagnostic."
                } else {
                    "Vibration acceptable for continuous unrestricted operation."
                }
            }
            VibrationSeverityZone.ZONE_C_UNSATISFACTORY -> {
                alarmStatus = "WARNING_MAINTENANCE_REQUIRED"
                recommendation = "Vibration severity in Warning Zone. Restricted operation permitted; schedule maintenance overhaul within 72 hours."
            }
            VibrationSeverityZone.ZONE_D_UNACCEPTABLE -> {
                alarmStatus = "CRITICAL_EMERGENCY_SHUTDOWN"
                recommendation = "DANGER: Vibration exceeds trip threshold. Initiate immediate turbine trip and emergency shutdown."
            }
        }

        return PredictiveMaintenanceReport(
            machineClass = machineClass,
            severityZone = zone,
            velocityRmsMmSec = packet.velocityRmsMmSec,
            alarmStatus = alarmStatus,
            recommendation = recommendation,
            acousticAnomaly = acousticAnomaly,
            timestampEpochMs = System.currentTimeMillis()
        )
    }
}
