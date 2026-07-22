package com.alirezabdn.generator.domain.usecase

import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.repository.mock.DonationRepositoryMock
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetDonationReferrerTypesUseCaseImplTest {
    private val repository = DonationRepositoryMock()
    private val useCase = GetDonationReferrerTypesUseCaseImpl(repository)

    @Test
    fun `invoke passes the donation id to repository and maps a successful response`() = runTest {
        repository.getDonationReferrerTypesResult = flowOf(
            AyanAPIResult.success(
                DonationServiceDTO.DonationResponseModel(
                    referrerTypeList = listOf(
                        DonationServiceDTO.ReferrerType(
                            name = "campaign",
                            showName = "Campaign",
                        ),
                    ),
                ),
            ),
        )

        val emissions = useCase("donation-42").toList()

        assertEquals(1, repository.getDonationReferrerTypesCallCount)
        assertEquals("donation-42", repository.lastGetDonationReferrerTypesRequestBody?.id)
        assertEquals(
            listOf(ReferrerType(name = "campaign", displayName = "Campaign")),
            (emissions.single() as AyanAPIResult.Success).value,
        )
    }

    @Test
    fun `invoke converts a response without a list to an error`() = runTest {
        repository.getDonationReferrerTypesResult = flowOf(
            AyanAPIResult.success(
                DonationServiceDTO.DonationResponseModel(referrerTypeList = null),
            ),
        )

        val result = useCase("donation-42").toList().single()

        assertTrue(result is AyanAPIResult.Error)
        assertEquals(
            "Response is empty, try another time...",
            (result as AyanAPIResult.Error).ayanFailure.message,
        )
    }

    @Test
    fun `invoke exposes the networking failure message`() = runTest {
        val failure = Failure(
            failureRepository = FailureRepository.REMOTE,
            failureType = FailureType.UNKNOWN,
            failureCode = "server_error",
            language = Language.ENGLISH,
            failureStatus = null,
            failureMessage = "Service unavailable",
        )
        repository.getDonationReferrerTypesResult = flowOf(
            AyanAPIResult.error<DonationServiceDTO.DonationResponseModel>(failure),
        )

        val result = useCase("donation-42").toList().single()

        assertEquals(
            "Service unavailable",
            (result as AyanAPIResult.Error).ayanFailure.message,
        )
    }

    @Test
    fun `invoke forwards repository state changes`() = runTest {
        repository.getDonationReferrerTypesResult = flowOf(
            AyanAPIResult.changeState<DonationServiceDTO.DonationResponseModel>(
                ApiCallStatus.LOADING,
            ),
            AyanAPIResult.changeState<DonationServiceDTO.DonationResponseModel>(
                ApiCallStatus.SUCCESSFUL,
            ),
        )

        val results = useCase("donation-42").toList()

        assertEquals(
            listOf(ApiCallStatus.LOADING, ApiCallStatus.SUCCESSFUL),
            results.map { (it as AyanAPIResult.ChangeState).state },
        )
    }
}
