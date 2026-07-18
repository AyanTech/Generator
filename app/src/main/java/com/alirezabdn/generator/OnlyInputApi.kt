package com.alirezabdn.generator

import kotlinx.serialization.Serializable

@AyanAPI(endPoint = "DesiredName")
class OnlyInputApi {
    @Serializable
    data class OnlyRequestBody(val test: String)
}
