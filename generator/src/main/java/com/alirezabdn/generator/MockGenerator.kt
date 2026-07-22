package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates dependency-free mocks for unit tests that consume data sources or repositories.
 *
 * Each generated method exposes a configurable result flow, invocation count, and captured
 * arguments.
 */
internal class MockGenerator(
    private val codeGenerator: CodeGenerator,
    private val functionFactory: ApiFunctionFactory,
) {

    /** Writes the remote data-source mock for [batch]. */
    fun generateRemoteDataSourceMock(batch: GenerationBatch) {
        val interfaceName = "${batch.typePrefix}RemoteDataSource"
        generate(
            batch = batch,
            packageName = GeneratorPackages.REMOTE_DATA_SOURCE_MOCK,
            className = "${interfaceName}Mock",
            interfaceType = ClassName(GeneratorPackages.REMOTE_DATA_SOURCE, interfaceName),
        )
    }

    /** Writes the repository mock for [batch]. */
    fun generateRepositoryMock(batch: GenerationBatch) {
        val interfaceName = "${batch.typePrefix}Repository"
        generate(
            batch = batch,
            packageName = GeneratorPackages.REPOSITORY_MOCK,
            className = "${interfaceName}Mock",
            interfaceType = ClassName(GeneratorPackages.REPOSITORY, interfaceName),
        )
    }

    private fun generate(
        batch: GenerationBatch,
        packageName: String,
        className: String,
        interfaceType: ClassName,
    ) {
        val mockSpec = TypeSpec.classBuilder(className)
            .addSuperinterface(interfaceType)
            .apply {
                batch.dataNeeds.forEach { data ->
                    addProperty(createResultProperty(data))
                    addProperty(createCallCountProperty(data))
                    data.requestBodyType?.let {
                        addProperty(createCapturedRequestBodyProperty(data, it))
                    }
                    addProperty(createCapturedBaseUrlProperty(data))
                    addFunction(createMockFunction(data))
                }
            }
            .build()

        FileSpec.builder(packageName, className)
            .addType(mockSpec)
            .build()
            .writeTo(
                codeGenerator,
                aggregating = true,
                originatingKSFiles = batch.sourceFiles,
            )
    }

    private fun createResultProperty(data: ProcessorDataNeed): PropertySpec =
        PropertySpec.builder(data.mockResultPropertyName, data.resultType)
            .mutable()
            .initializer("%M()", GeneratorTypes.emptyFlow)
            .build()

    private fun createCallCountProperty(data: ProcessorDataNeed): PropertySpec =
        PropertySpec.builder(data.mockCallCountPropertyName, Int::class.asClassName())
            .mutable()
            .initializer("0")
            .privateSetter()
            .build()

    private fun createCapturedRequestBodyProperty(
        data: ProcessorDataNeed,
        requestBodyType: TypeName,
    ): PropertySpec =
        PropertySpec.builder(
            data.capturedRequestBodyPropertyName,
            requestBodyType.copy(nullable = true),
        )
            .mutable()
            .initializer("null")
            .privateSetter()
            .build()

    private fun createCapturedBaseUrlProperty(data: ProcessorDataNeed): PropertySpec =
        PropertySpec.builder(
            data.capturedBaseUrlPropertyName,
            GeneratorTypes.string.copy(nullable = true),
        )
            .mutable()
            .initializer("null")
            .privateSetter()
            .build()

    private fun createMockFunction(data: ProcessorDataNeed): FunSpec =
        FunSpec.builder(data.methodName)
            .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
            .apply { data.requestBodyType?.let { addParameter("requestBody", it) } }
            .addParameter(functionFactory.createBaseUrlParameter(withDefault = false))
            .returns(data.resultType)
            .addStatement("%N++", data.mockCallCountPropertyName)
            .apply {
                if (data.requestBodyType != null) {
                    addStatement("%N = requestBody", data.capturedRequestBodyPropertyName)
                }
            }
            .addStatement("%N = baseUrl", data.capturedBaseUrlPropertyName)
            .addStatement("return %N", data.mockResultPropertyName)
            .build()

    private fun PropertySpec.Builder.privateSetter(): PropertySpec.Builder =
        setter(FunSpec.setterBuilder().addModifiers(KModifier.PRIVATE).build())
}
