package com.alirezabdn.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName

/** Package names used by generated networking sources. */
internal object GeneratorPackages {
    const val REMOTE_DATA_SOURCE = "ir.ayantech.networking.datasource"
    const val REMOTE_DATA_SOURCE_IMPLEMENTATION = "$REMOTE_DATA_SOURCE.impl"
    const val REMOTE_DATA_SOURCE_MOCK = "$REMOTE_DATA_SOURCE.mock"
    const val REPOSITORY = "ir.ayantech.networking.repository"
    const val REPOSITORY_IMPLEMENTATION = "$REPOSITORY.impl"
    const val REPOSITORY_MOCK = "$REPOSITORY.mock"
}

/** External and Kotlin types referenced by generated source files. */
internal object GeneratorTypes {
    val ayanApi = ClassName("ir.ayantech.networking.v2", "AyanApi")
    val ayanApiResult = ClassName("ir.ayantech.networking.v2.api", "AyanAPIResult")
    val apiCallStatus = ClassName("ir.ayantech.networking.v2.model", "ApiCallStatus")
    val flow = ClassName("kotlinx.coroutines.flow", "Flow")
    val emptyFlow = MemberName("kotlinx.coroutines.flow", "emptyFlow")
    val string = String::class.asClassName()
}
