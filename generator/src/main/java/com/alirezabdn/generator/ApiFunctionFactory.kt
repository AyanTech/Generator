package com.alirezabdn.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.UNIT

/** Builds method specifications shared by generated interfaces and implementations. */
internal class ApiFunctionFactory {

    /**
     * Builds a remote data-source method.
     *
     * @param data endpoint types and names.
     * @param abstract whether to emit an interface declaration instead of an implementation.
     */
    fun createRemoteDataSourceFunction(
        data: ProcessorDataNeed,
        abstract: Boolean = false,
    ): FunSpec =
        commonFunctionBuilder(data, abstract)
            .apply {
                if (!abstract && data.requestBodyType != null) {
                    addStatement(
                        "return ayanApi.post<%T, %T>(body = requestBody, endPoint = %S, baseUrl = baseUrl)",
                        data.requestBodyType,
                        data.responseType,
                        data.endPoint,
                    )
                } else if (!abstract) {
                    addStatement(
                        "return ayanApi.post<%T, %T>(body = %T, endPoint = %S, baseUrl = baseUrl)",
                        UNIT,
                        data.responseType,
                        UNIT,
                        data.endPoint,
                    )
                }
            }
            .build()

    /**
     * Builds a repository method that delegates to its remote data source.
     *
     * @param data endpoint types and names.
     * @param abstract whether to emit an interface declaration instead of an implementation.
     */
    fun createRepositoryFunction(
        data: ProcessorDataNeed,
        abstract: Boolean = false,
    ): FunSpec =
        commonFunctionBuilder(data, abstract)
            .apply {
                if (!abstract && data.requestBodyType != null) {
                    addStatement(
                        "return remoteDataSource.%N(requestBody = requestBody, baseUrl = baseUrl)",
                        data.methodName,
                    )
                } else if (!abstract) {
                    addStatement(
                        "return remoteDataSource.%N(baseUrl = baseUrl)",
                        data.methodName,
                    )
                }
            }
            .build()

    /** Builds the nullable base URL parameter used by all generated API methods. */
    fun createBaseUrlParameter(withDefault: Boolean): ParameterSpec =
        ParameterSpec.builder("baseUrl", GeneratorTypes.string.copy(nullable = true))
            .apply { if (withDefault) defaultValue("null") }
            .build()

    private fun commonFunctionBuilder(
        data: ProcessorDataNeed,
        abstract: Boolean,
    ): FunSpec.Builder =
        FunSpec.builder(data.methodName)
            .addModifiers(KModifier.SUSPEND)
            .apply {
                addModifiers(if (abstract) KModifier.ABSTRACT else KModifier.OVERRIDE)
                data.requestBodyType?.let { addParameter("requestBody", it) }
            }
            .addParameter(createBaseUrlParameter(withDefault = abstract))
            .returns(data.resultType)
}
