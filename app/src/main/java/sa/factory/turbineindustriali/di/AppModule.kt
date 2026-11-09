package sa.factory.turbineindustriali.di

import sa.factory.turbineindustriali.data.repository.TurbineIndustrialRepositoryImpl
import sa.factory.turbineindustriali.domain.repository.TurbineIndustrialRepository
import sa.factory.turbineindustriali.domain.usecase.*
import sa.factory.turbineindustriali.hardware.HardwareSensorDriver
import sa.factory.turbineindustriali.ui.viewmodel.TurbineIndustrialViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: TurbineIndustrialRepository = TurbineIndustrialRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): TurbineIndustrialViewModel = TurbineIndustrialViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
