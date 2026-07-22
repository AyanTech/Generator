package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

/** Generates a repository interface and its remote data-source-backed implementation. */
internal class RepositoryGenerator(
    private val codeGenerator: CodeGenerator,
    private val functionFactory: ApiFunctionFactory,
) {

    /** Writes both repository types for a single separation category. */
    fun generate(batch: GenerationBatch) {
        val interfaceName = "${batch.typePrefix}Repository"
        val interfaceType = ClassName(GeneratorPackages.REPOSITORY, interfaceName)

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
                    addFunction(functionFactory.createRepositoryFunction(data, abstract = true))
                }
            }
            .build()

        FileSpec.builder(GeneratorPackages.REPOSITORY, interfaceName)
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
        val remoteDataSourceType = ClassName(
            GeneratorPackages.REMOTE_DATA_SOURCE,
            "${batch.typePrefix}RemoteDataSource",
        )
        val constructor = FunSpec.constructorBuilder()
            .addParameter("remoteDataSource", remoteDataSourceType)
            .build()
        val implementationSpec = TypeSpec.classBuilder(implementationName)
            .primaryConstructor(constructor)
            .addSuperinterface(interfaceType)
            .addProperty(
                PropertySpec.builder(
                    "remoteDataSource",
                    remoteDataSourceType,
                    KModifier.PRIVATE,
                )
                    .initializer("remoteDataSource")
                    .build()
            )
            .apply {
                batch.dataNeeds.forEach { data ->
                    addFunction(functionFactory.createRepositoryFunction(data))
                }
            }
            .build()

        FileSpec.builder(GeneratorPackages.REPOSITORY_IMPLEMENTATION, implementationName)
            .addType(implementationSpec)
            .build()
            .writeTo(
                codeGenerator,
                aggregating = true,
                originatingKSFiles = batch.sourceFiles,
            )
    }
}
