package com.alirezabdn.generator.presentation

import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import com.alirezabdn.generator.domain.usecase.GetDonationReferrerTypesUseCaseImpl
import ir.ayantech.networking.repository.mock.DonationRepositoryMock
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

private typealias RepositoryResult =
    AyanAPIResult<DonationServiceDTO.DonationResponseModel, ApiCallStatus, Exception>

@OptIn(ExperimentalCoroutinesApi::class)
class DonationViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = DonationRepositoryMock()
    private val useCase = GetDonationReferrerTypesUseCaseImpl(repository)
    private val viewModel = DonationViewModel(useCase)

    @Test
    fun `initial state is idle`() {
        assertSame(DonationUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `loadReferrerTypes uses default id and exposes mapped content`() = runTest {
        val repositoryResults = MutableSharedFlow<RepositoryResult>()
        repository.getDonationReferrerTypesResult = repositoryResults

        viewModel.loadReferrerTypes()
        advanceUntilIdle()
        assertSame(DonationUiState.Loading, viewModel.uiState.value)
        assertEquals("abc", repository.lastGetDonationReferrerTypesRequestBody?.id)

        val response = DonationServiceDTO.DonationResponseModel(
            referrerTypeList = listOf(
                DonationServiceDTO.ReferrerType(name = "web", showName = "Website"),
            ),
        )
        repositoryResults.emit(AyanAPIResult.success(response))
        advanceUntilIdle()

        assertEquals(
            DonationUiState.Content(
                listOf(ReferrerType(name = "web", displayName = "Website")),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `loadReferrerTypes exposes use case errors`() = runTest {
        val repositoryResults = MutableSharedFlow<RepositoryResult>()
        repository.getDonationReferrerTypesResult = repositoryResults

        viewModel.loadReferrerTypes("broken-donation")
        advanceUntilIdle()
        repositoryResults.emit(
            AyanAPIResult.error<DonationServiceDTO.DonationResponseModel>(
                Exception("Request failed"),
            ),
        )
        advanceUntilIdle()

        // The use case intentionally exposes messages only from networking Failure values.
        assertEquals(DonationUiState.Error(""), viewModel.uiState.value)
    }

    @Test
    fun `state changes are translated to loading and idle`() = runTest {
        val repositoryResults = MutableSharedFlow<RepositoryResult>()
        repository.getDonationReferrerTypesResult = repositoryResults
        viewModel.loadReferrerTypes("donation-id")
        advanceUntilIdle()

        repositoryResults.emit(
            AyanAPIResult.changeState<DonationServiceDTO.DonationResponseModel>(
                ApiCallStatus.LOADING,
            ),
        )
        advanceUntilIdle()
        assertSame(DonationUiState.Loading, viewModel.uiState.value)

        repositoryResults.emit(
            AyanAPIResult.changeState<DonationServiceDTO.DonationResponseModel>(
                ApiCallStatus.SUCCESSFUL,
            ),
        )
        advanceUntilIdle()
        assertSame(DonationUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `starting a new load cancels the previous collection`() = runTest {
        val repositoryResults = MutableSharedFlow<RepositoryResult>()
        repository.getDonationReferrerTypesResult = repositoryResults

        viewModel.loadReferrerTypes("first")
        advanceUntilIdle()
        viewModel.loadReferrerTypes("second")
        advanceUntilIdle()

        assertEquals(2, repository.getDonationReferrerTypesCallCount)
        assertEquals("second", repository.lastGetDonationReferrerTypesRequestBody?.id)
        assertEquals(1, repositoryResults.subscriptionCount.value)
    }
}
