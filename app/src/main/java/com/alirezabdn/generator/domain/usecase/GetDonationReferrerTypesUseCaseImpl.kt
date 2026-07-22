package com.alirezabdn.generator.domain.usecase

import com.alirezabdn.generator.data.mapper.toUIModel
import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import ir.ayantech.networking.repository.DonationRepository
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.api.onChangeState
import ir.ayantech.networking.v2.api.onFailure
import ir.ayantech.networking.v2.api.onSuccess
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


/** Retrieves donation referrer types through the domain repository boundary. */
class GetDonationReferrerTypesUseCaseImpl(
    private val repository: DonationRepository,
) : GetDonationReferrerTypesUseCase {
    override suspend fun invoke(id: String): Flow<AyanAPIResult<List<ReferrerType>, ApiCallStatus, Exception>> {
        return flow {
            val donationRequest = DonationServiceDTO.GetDonationRequestBody(id = id)
            repository.getDonationReferrerTypes(donationRequest).collect { result ->
                result.onSuccess { success ->
                    if (success.referrerTypeList != null) {
                        emit(AyanAPIResult.success(success.toUIModel()))
                    } else {
                        val fakeException = Exception("Response is empty, try another time...")
                        emit(AyanAPIResult.error(fakeException))
                    }

                }
                result.onFailure { ayanFailure ->
                    val failureMessage = (ayanFailure as? Failure)?.failureMessage ?: ""
                    emit(AyanAPIResult.Error(Exception(failureMessage)))
                }

                result.onChangeState { state ->
                    emit(AyanAPIResult.ChangeState(state))
                }

            }
        }
    }

}
