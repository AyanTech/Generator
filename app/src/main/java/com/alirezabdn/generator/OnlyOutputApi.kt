package com.alirezabdn.generator

import kotlinx.serialization.Serializable

@AyanAPI
class OnlyOutputApi {
    @Serializable
    data class OnlyResponseModel(val test: String)
}
