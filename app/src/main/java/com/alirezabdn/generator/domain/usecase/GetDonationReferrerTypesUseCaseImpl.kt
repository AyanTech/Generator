package com.alirezabdn.generator.domain.usecase

import com.alirezabdn.generator.data.mapper.toUIModel
import com.alirezabdn.generator.data.remote.dto.DonationServiceDTO
import com.alirezabdn.generator.domain.model.ReferrerType
import ir.ayantech.networking.repository.DonationRepository

/** Retrieves donation referrer types through the domain repository boundary. */
class GetDonationReferrerTypesUseCaseImpl(
    private val repository: DonationRepository,
) : GetDonationReferrerTypesUseCase {
    override suspend fun invoke(id: String): Result<List<ReferrerType>> {
        val request = DonationServiceDTO.GetDonationRequestBody(id = id)
        return repository.getDonationReferrerTypes(request).fold(
            onSuccess = { response ->
                if (response.referrerTypeList == null) {
                    Result.failure(IllegalStateException("Response is empty, try another time..."))
                } else {
                    Result.success(response.toUIModel())
                }
            },
            onFailure = { Result.failure(it) },
        )
    }
}
