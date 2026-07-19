package com.alirezabdn.generator.data

import ir.ayantech.networking.v2.api.AyanAPIResult
import ir.ayantech.networking.v2.model.ApiCallStatus
import kotlinx.coroutines.flow.Flow

interface SampleRepository {
    suspend fun fetchDonation(data: DonationServiceDTO.GetDonationRequestBody):
            Flow<AyanAPIResult<DonationServiceDTO.DonationResponseModel, ApiCallStatus, Exception>>
}