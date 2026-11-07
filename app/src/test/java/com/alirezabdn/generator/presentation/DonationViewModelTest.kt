package com.alirezabdn.generator.presentation

import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import com.alirezabdn.generator.domain.usecase.GetDonationReferrerTypesUseCaseImpl
import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.repository.DonationRepository
import ir.ayantech.networking.repository.mock.DonationRepositoryMock
import ir.ayantech.networking.v2.helpers.Failure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
        repository.getDonationReferrerTypesResult = Result.success(
            DonationServiceDTO.DonationResponseModel(
                referrerTypeList = listOf(
                    DonationServiceDTO.ReferrerType(name = "web", showName = "Website"),
                ),
            ),
        )

        viewModel.loadReferrerTypes()
        advanceUntilIdle()

        assertEquals("abc", repository.lastGetDonationReferrerTypesRequestBody?.id)
        assertEquals(
            DonationUiState.Content(
                listOf(ReferrerType(name = "web", displayName = "Website")),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `loadReferrerTypes exposes use case errors`() = runTest {
        repository.getDonationReferrerTypesResult = Result.failure(Exception("Request failed"))

        viewModel.loadReferrerTypes("broken-donation")
        advanceUntilIdle()

        assertEquals(DonationUiState.Error("Request failed"), viewModel.uiState.value)
    }

    @Test
    fun `loadReferrerTypes displays the networking failure message`() = runTest {
        repository.getDonationReferrerTypesResult = Result.failure(
            Failure(
                failureRepository = FailureRepository.REMOTE,
                failureType = FailureType.UNKNOWN,
                failureCode = "server_error",
                language = Language.ENGLISH,
                failureStatus = null,
                failureMessage = "Service unavailable",
            ),
        )

        viewModel.loadReferrerTypes("donation-id")
        advanceUntilIdle()

        assertEquals(DonationUiState.Error("Service unavailable"), viewModel.uiState.value)
    }

    @Test
    fun `starting a new load cancels the previous request`() = runTest {
        val requestIds = mutableListOf<String>()
        var firstRequestCancelled = false
        val suspendingRepository = object : DonationRepository by DonationRepositoryMock() {
            override suspend fun getDonationReferrerTypes(
                requestBody: DonationServiceDTO.GetDonationRequestBody,
                baseUrl: String?,
            ): Result<DonationServiceDTO.DonationResponseModel> {
                requestIds += requestBody.id
                if (requestBody.id == "first") {
                    try {
                        awaitCancellation()
                    } finally {
                        firstRequestCancelled = true
                    }
                }
                return Result.success(DonationServiceDTO.DonationResponseModel(emptyList()))
            }
        }
        val viewModel = DonationViewModel(GetDonationReferrerTypesUseCaseImpl(suspendingRepository))

        viewModel.loadReferrerTypes("first")
        advanceUntilIdle()
        assertSame(DonationUiState.Loading, viewModel.uiState.value)

        viewModel.loadReferrerTypes("second")
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), requestIds)
        assertTrue(firstRequestCancelled)
        assertEquals(DonationUiState.Content(emptyList()), viewModel.uiState.value)
    }
}
