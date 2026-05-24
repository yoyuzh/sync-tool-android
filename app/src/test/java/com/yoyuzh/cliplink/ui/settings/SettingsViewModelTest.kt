package com.yoyuzh.cliplink.ui.settings

import com.yoyuzh.cliplink.data.settings.AppSettingsStore
import com.yoyuzh.cliplink.domain.usecase.RegisterDeviceUseCase
import com.yoyuzh.cliplink.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var registerDeviceUseCase: RegisterDeviceUseCase
    private lateinit var workScheduler: WorkScheduler

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settingsStore = mock()
        registerDeviceUseCase = mock()
        workScheduler = mock()
        whenever(settingsStore.settings).thenReturn(flowOf())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveAndRegister persists drafts before registration starts`() = runTest(dispatcher) {
        whenever(registerDeviceUseCase()).thenReturn(Result.success(Unit))
        val viewModel = SettingsViewModel(settingsStore, registerDeviceUseCase, workScheduler)

        viewModel.saveAndRegisterDevice(" http://127.0.0.1:8787 ", " Android Phone ")
        advanceUntilIdle()

        inOrder(settingsStore, registerDeviceUseCase) {
            verify(settingsStore).updateServerUrl("http://127.0.0.1:8787")
            verify(settingsStore).updateDeviceName("Android Phone")
            verify(registerDeviceUseCase).invoke()
        }
        verify(workScheduler).triggerImmediateSync()
    }
}
