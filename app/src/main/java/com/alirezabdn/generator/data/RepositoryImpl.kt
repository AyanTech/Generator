package com.alirezabdn.generator.data

import ir.ayantech.ayannetworking.v2.api.AyanAPIResult
import ir.ayantech.ayannetworking.v2.model.ApiCallStatus
import ir.ayantech.networking.datasource.DonationServiceDTORemoteDataSource
import kotlinx.coroutines.flow.Flow

class RepositoryImpl(val donationServiceDTORemoteDataSource: DonationServiceDTORemoteDataSource) :
    SampleRepository {
    override suspend fun fetchDonation(data: DonationServiceDTO.GetDonationRequestBody)
    : Flow<AyanAPIResult<DonationServiceDTO.DonationResponseModel, ApiCallStatus, Exception>> {
        return donationServiceDTORemoteDataSource.invoke(requestBody = data)
    }
}