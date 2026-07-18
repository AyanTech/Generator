package com.alirezabdn.generator

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@AyanAPI
class GetEndUserInquiryHistoryDetail {
    @Serializable
    data class Input(
        @SerialName("InquiryType")
        val inquiryType: String)

    @Serializable
    data class Output(
        @SerialName("InquiryHistory")
        val inquiryHistory: List<InquiryModel>,
        @SerialName("TotalInquiryHistoryCount")
        val totalInquiryHistoryCount: Long
    )

    @Serializable
    data class InquiryModel(
        @SerialName("Description")
        val description: String,
        @SerialName("IsFavorite")
        val isFavorite: Boolean,
        @SerialName("IsElectronic")
        val isElectronic: Boolean,
        @SerialName("ID")
        val id: Long,
        @SerialName("Type")
        val type: String,
        @SerialName("Value")
        val value: String
    )
}
