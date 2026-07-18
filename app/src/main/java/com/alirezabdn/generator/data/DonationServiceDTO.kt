package com.alirezabdn.generator.data

import com.alirezabdn.generator.AyanAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@AyanAPI(endPoint = "DonationServiceGerReferrerTypeList")
class DonationServiceDTO {

    @Serializable
    data class GetDonationRequestBody(
        val id: String
    )

    @Serializable
    data class  DonationResponseModel(
        @SerialName("ReferrerTypeList")
        val referrerTypeList: List<ReferrerType>?
    )

    @Serializable
    data class ReferrerType(
        @SerialName("Name")
        val name: String,
        @SerialName("ShowName")
        val showName: String

    )
}