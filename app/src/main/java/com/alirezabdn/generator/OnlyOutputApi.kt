package com.alirezabdn.generator

import kotlinx.serialization.Serializable

@AyanAPI
class OnlyOutputApi {
    @Serializable
    data class Outputput(val test: String)
}
