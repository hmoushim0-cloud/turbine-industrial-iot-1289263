package sa.factory.turbineindustriali.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.turbineindustriali.domain.usecase.*
import sa.factory.turbineindustriali.model.TurbineIndustrialItem
import sa.factory.turbineindustriali.model.TurbineIndustrialOrder
import sa.factory.turbineindustriali.model.TurbineIndustrialTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TurbineIndustrialUiState(
    val catalog: List<TurbineIndustrialItem> = emptyList(),
    val filteredCatalog: List<TurbineIndustrialItem> = emptyList(),
    val selectedCategory: String = "الكل",
    val cart: List<TurbineIndustrialItem> = emptyList(),
    val telemetry: TurbineIndustrialTelemetry? = null,
    val lastOrder: TurbineIndustrialOrder? = null,
    val isCheckingOut: Boolean = false,
    val activeTab: String = "home"
)

class TurbineIndustrialViewModel(
    private val getCatalogUseCase: GetCatalogUseCase,
    private val manageCartUseCase: ManageCartUseCase,
    private val processCheckoutUseCase: ProcessCheckoutUseCase,
    private val observeHardwareTelemetryUseCase: ObserveHardwareTelemetryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TurbineIndustrialUiState())
    val uiState: StateFlow<TurbineIndustrialUiState> = _uiState.asStateFlow()

    init {
        // Observe Catalog
        getCatalogUseCase().onEach { list ->
            _uiState.update { it.copy(catalog = list, filteredCatalog = list) }
        }.launchIn(viewModelScope)

        // Observe Cart
        manageCartUseCase.cart.onEach { cartItems ->
            _uiState.update { it.copy(cart = cartItems) }
        }.launchIn(viewModelScope)

        // Observe Telemetry
        observeHardwareTelemetryUseCase().onEach { telem ->
            _uiState.update { it.copy(telemetry = telem) }
        }.launchIn(viewModelScope)
    }

    fun selectCategory(category: String) {
        _uiState.update { state ->
            val filtered = if (category == "الكل") state.catalog else state.catalog.filter { it.category == category }
            state.copy(selectedCategory = category, filteredCatalog = filtered)
        }
    }

    fun switchTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun addToCart(item: TurbineIndustrialItem) = manageCartUseCase.add(item)
    fun removeFromCart(item: TurbineIndustrialItem) = manageCartUseCase.remove(item)

    fun checkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }
            val order = processCheckoutUseCase()
            _uiState.update { it.copy(isCheckingOut = false, lastOrder = order, activeTab = "checkout") }
        }
    }
}
