package com.alirezabdn.generator.domain.usecase

import com.alirezabdn.generator.domain.model.ReferrerType

interface GetDonationReferrerTypesUseCase {
    suspend operator fun invoke(id: String): Result<List<ReferrerType>>
}
