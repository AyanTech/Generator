package com.alirezabdn.generator.data.remote.dto

import android.annotation.SuppressLint
import com.alirezabdn.generator.AyanAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@AyanAPI(
    endpoint = "DonationServiceGerReferrerTypeList",
    methodImplName = "getDonationReferrerTypes",
    separationCategory = "Donation",
)
class DonationServiceDTO {

    @Serializable
    data class GetDonationRequestBody(
        val id: String,
    )

    @Serializable
    data class DonationResponseModel(
        @SerialName("ReferrerTypeList")
        val referrerTypeList: List<ReferrerType>?,
    )

    @Serializable
    data class ReferrerType(
        @SerialName("Name")
        val name: String,
        @SerialName("ShowName")
        val showName: String,
    )
}
