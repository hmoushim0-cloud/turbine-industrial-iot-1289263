package sa.factory.turbineindustriali.data.remote

import sa.factory.turbineindustriali.model.TurbineIndustrialItem
import sa.factory.turbineindustriali.model.TurbineIndustrialOrder

interface TurbineIndustrialApiService {
    suspend fun fetchCatalog(): List<TurbineIndustrialItem>
    suspend fun submitOrder(order: TurbineIndustrialOrder): Boolean
}
