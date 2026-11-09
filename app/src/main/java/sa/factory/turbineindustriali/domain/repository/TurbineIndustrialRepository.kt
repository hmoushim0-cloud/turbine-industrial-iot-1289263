package sa.factory.turbineindustriali.domain.repository

import sa.factory.turbineindustriali.model.TurbineIndustrialItem
import sa.factory.turbineindustriali.model.TurbineIndustrialOrder
import sa.factory.turbineindustriali.model.TurbineIndustrialTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TurbineIndustrialRepository {
    val catalogFlow: Flow<List<TurbineIndustrialItem>>
    val activeCart: StateFlow<List<TurbineIndustrialItem>>
    val telemetryFlow: Flow<TurbineIndustrialTelemetry>
    
    fun addToCart(item: TurbineIndustrialItem)
    fun removeFromCart(item: TurbineIndustrialItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): TurbineIndustrialOrder
    suspend fun refreshCatalog()
}
