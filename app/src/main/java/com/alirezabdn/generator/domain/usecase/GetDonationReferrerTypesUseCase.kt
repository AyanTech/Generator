package com.alirezabdn.generator.domain.usecase

import com.alirezabdn.generator.domain.model.ReferrerType
import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.flow.Flow

interface GetDonationReferrerTypesUseCase {
    suspend operator fun invoke(id: String): Flow<AyanAPIResult<List<ReferrerType>, ApiCallStatus, Exception>>
}