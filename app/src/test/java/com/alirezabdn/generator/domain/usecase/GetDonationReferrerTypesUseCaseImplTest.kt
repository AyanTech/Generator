package com.alirezabdn.generator.domain.usecase

import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.repository.mock.DonationRepositoryMock
import ir.ayantech.networking.v2.helpers.Failure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GetDonationReferrerTypesUseCaseImplTest {
    private val repository = DonationRepositoryMock()
    private val useCase = GetDonationReferrerTypesUseCaseImpl(repository)

    @Test
    fun `invoke passes the donation id to repository and maps a successful response`() = runTest {
        repository.getDonationReferrerTypesResult = Result.success(
            DonationServiceDTO.DonationResponseModel(
                referrerTypeList = listOf(
                    DonationServiceDTO.ReferrerType(
                        name = "campaign",
                        showName = "Campaign",
                    ),
                ),
            ),
        )

        val result = useCase("donation-42")

        assertEquals(1, repository.getDonationReferrerTypesCallCount)
        assertEquals("donation-42", repository.lastGetDonationReferrerTypesRequestBody?.id)
        assertEquals(
            listOf(ReferrerType(name = "campaign", displayName = "Campaign")),
            result.getOrThrow(),
        )
    }

    @Test
    fun `invoke converts a response without a list to an error`() = runTest {
        repository.getDonationReferrerTypesResult = Result.success(
            DonationServiceDTO.DonationResponseModel(referrerTypeList = null),
        )

        val result = useCase("donation-42")

        assertEquals("Response is empty, try another time...", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke preserves the networking failure`() = runTest {
        val failure = Failure(
            failureRepository = FailureRepository.REMOTE,
            failureType = FailureType.UNKNOWN,
            failureCode = "server_error",
            language = Language.ENGLISH,
            failureStatus = null,
            failureMessage = "Service unavailable",
        )
        repository.getDonationReferrerTypesResult = Result.failure(failure)

        val result = useCase("donation-42")

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `invoke reports an unconfigured repository mock`() = runTest {
        val result = useCase("donation-42")

        assertEquals(
            "No mock result configured for getDonationReferrerTypes",
            result.exceptionOrNull()?.message,
        )
    }
}
