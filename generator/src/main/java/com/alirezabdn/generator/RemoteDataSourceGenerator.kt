package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

/** Generates a remote data-source interface and its Ayan API-backed implementation. */
internal class RemoteDataSourceGenerator(
    private val codeGenerator: CodeGenerator,
    private val functionFactory: ApiFunctionFactory,
) {

    /** Writes both remote data-source types for a single separation category. */
    fun generate(batch: GenerationBatch) {
        val interfaceName = "${batch.typePrefix}RemoteDataSource"
        val interfaceType = ClassName(GeneratorPackages.REMOTE_DATA_SOURCE, interfaceName)

        generateInterface(batch, interfaceName)
        generateImplementation(batch, interfaceType, "${interfaceName}Impl")
    }

    private fun generateInterface(
        batch: GenerationBatch,
        interfaceName: String,
    ) {
        val interfaceSpec = TypeSpec.interfaceBuilder(interfaceName)
            .apply {
                batch.dataNeeds.forEach { data ->
                    addFunction(
                        functionFactory.createRemoteDataSourceFunction(data, abstract = true)
                    )
                }
            }
            .build()

        FileSpec.builder(GeneratorPackages.REMOTE_DATA_SOURCE, interfaceName)
            .addType(interfaceSpec)
            .build()
            .writeTo(
                codeGenerator,
                aggregating = true,
                originatingKSFiles = batch.sourceFiles,
            )
    }

    private fun generateImplementation(
        batch: GenerationBatch,
        interfaceType: ClassName,
        implementationName: String,
    ) {
        val constructor = FunSpec.constructorBuilder()
            .addParameter("ayanApi", GeneratorTypes.ayanApi)
            .build()
        val implementationSpec = TypeSpec.classBuilder(implementationName)
            .primaryConstructor(constructor)
            .addSuperinterface(interfaceType)
            .addProperty(
                PropertySpec.builder("ayanApi", GeneratorTypes.ayanApi, KModifier.PRIVATE)
                    .initializer("ayanApi")
                    .build()
            )
            .apply {
                batch.dataNeeds.forEach { data ->
                    addFunction(functionFactory.createRemoteDataSourceFunction(data))
                }
            }
            .build()

        FileSpec.builder(
            GeneratorPackages.REMOTE_DATA_SOURCE_IMPLEMENTATION,
            implementationName,
        )
            .addType(implementationSpec)
            .build()
            .writeTo(
                codeGenerator,
                aggregating = true,
                originatingKSFiles = batch.sourceFiles,
            )
    }
}
