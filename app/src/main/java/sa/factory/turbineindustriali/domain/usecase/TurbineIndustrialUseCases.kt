package sa.factory.turbineindustriali.domain.usecase

import sa.factory.turbineindustriali.domain.repository.TurbineIndustrialRepository
import sa.factory.turbineindustriali.model.TurbineIndustrialItem
import sa.factory.turbineindustriali.model.TurbineIndustrialOrder
import sa.factory.turbineindustriali.model.TurbineIndustrialTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: TurbineIndustrialRepository) {
    operator fun invoke(): Flow<List<TurbineIndustrialItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: TurbineIndustrialRepository) {
    val cart = repository.activeCart
    fun add(item: TurbineIndustrialItem) = repository.addToCart(item)
    fun remove(item: TurbineIndustrialItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: TurbineIndustrialRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): TurbineIndustrialOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: TurbineIndustrialRepository) {
    operator fun invoke(): Flow<TurbineIndustrialTelemetry> = repository.telemetryFlow
}
