package sa.factory.turbineindustriali.data.repository

import sa.factory.turbineindustriali.domain.repository.TurbineIndustrialRepository
import sa.factory.turbineindustriali.hardware.HardwareSensorDriver
import sa.factory.turbineindustriali.model.TurbineIndustrialItem
import sa.factory.turbineindustriali.model.TurbineIndustrialOrder
import sa.factory.turbineindustriali.model.TurbineIndustrialTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TurbineIndustrialRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : TurbineIndustrialRepository {

    private val _catalog = MutableStateFlow<List<TurbineIndustrialItem>>(listOf(
        TurbineIndustrialItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع 3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe", 180.0, "معتمد", "الخدمات الرئيسية"),
        TurbineIndustrialItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        TurbineIndustrialItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<TurbineIndustrialItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<TurbineIndustrialItem>>(emptyList())
    override val activeCart: StateFlow<List<TurbineIndustrialItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<TurbineIndustrialTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            TurbineIndustrialTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: TurbineIndustrialItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: TurbineIndustrialItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): TurbineIndustrialOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = TurbineIndustrialOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
