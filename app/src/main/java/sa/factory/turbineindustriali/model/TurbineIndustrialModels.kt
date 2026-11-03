package sa.factory.turbineindustriali.model

import java.io.Serializable

data class TurbineIndustrialItem(
    val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String = "معتمد",
    val category: String = "عام",
    val status: String = "AVAILABLE"
) : Serializable

data class TurbineIndustrialOrder(
    val orderId: String,
    val items: List<TurbineIndustrialItem>,
    val subtotalSar: Double,
    val vatSar: Double,
    val totalSar: Double,
    val paymentMethod: String = "Mada Contactless EMV",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "CONFIRMED"
) : Serializable

data class TurbineIndustrialTelemetry(
    val peripheralName: String,
    val status: String,
    val liveReading: String,
    val batteryPct: Int = 98,
    val signalDbm: Int = -58,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

enum class MachineClass {
    CLASS_I_SMALL,
    CLASS_II_MEDIUM,
    CLASS_III_LARGE_RIGID,
    CLASS_IV_LARGE_FLEXIBLE
}

enum class VibrationSeverityZone {
    ZONE_A_GOOD,
    ZONE_B_ACCEPTABLE,
    ZONE_C_UNSATISFACTORY,
    ZONE_D_UNACCEPTABLE
}

data class TriAxialVibrationPacket(
    val slaveAddress: Int,
    val xAxisAccelG: Double,
    val yAxisAccelG: Double,
    val zAxisAccelG: Double,
    val velocityRmsMmSec: Double,
    val bearingTempCelsius: Double,
    val acousticProbeDb: Double,
    val crcValid: Boolean,
    val rawFrameHex: String
) : Serializable

data class PredictiveMaintenanceReport(
    val machineClass: MachineClass,
    val severityZone: VibrationSeverityZone,
    val velocityRmsMmSec: Double,
    val alarmStatus: String,
    val recommendation: String,
    val acousticAnomaly: Boolean,
    val timestampEpochMs: Long = System.currentTimeMillis()
) : Serializable
