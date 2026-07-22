package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        Processor(environment.codeGenerator)
}

private class Processor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(AyanAPI::class.qualifiedName!!)
            .toList()
        val invalidSymbols = symbols.filterNot(KSAnnotated::validate)
        val apiClasses =
            symbols.filterIsInstance<KSClassDeclaration>().filter(KSAnnotated::validate)

        if (apiClasses.isEmpty() || invalidSymbols.isNotEmpty()) return invalidSymbols

        apiClasses
            .sortedBy { it.qualifiedName?.asString() }
            .map { declaration ->
                ProcessorInput(
                    data = declaration.toDataNeed(),
                    sourceFile = requireNotNull(declaration.containingFile),
                )
            }
            .groupBy { it.data.separationCategory }
            .toSortedMap()
            .forEach { (separationCategory, inputs) ->
                val dataNeeds = inputs.map(ProcessorInput::data)
                val sourceFiles = inputs.map(ProcessorInput::sourceFile).distinct()
                createRemoteDataSource(separationCategory, dataNeeds, sourceFiles)
                createRepository(separationCategory, dataNeeds, sourceFiles)
            }

        return invalidSymbols
    }

    private fun createRemoteDataSource(
        separationCategory: String,
        processorDataNeeds: List<ProcessorDataNeed>,
        sourceFiles: List<KSFile>,
    ) {
        val interfaceName = "${separationCategory.toTypePrefix()}RemoteDataSource"
        val implementationName = "${interfaceName}Impl"
        val interfaceType = ClassName(
            REMOTE_DATA_SOURCE_INTERFACE_PACKAGE,
            interfaceName,
        )

        FileSpec.builder(REMOTE_DATA_SOURCE_INTERFACE_PACKAGE, interfaceName)
            .addType(
                TypeSpec.interfaceBuilder(interfaceName)
                    .apply {
                        processorDataNeeds.forEach { data ->
                            addFunction(createRemoteDataSourceFunction(data, abstract = true))
                        }
                    }
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = true, originatingKSFiles = sourceFiles)

        val constructor = FunSpec.constructorBuilder()
            .addParameter("ayanApi", AYAN_API)
            .build()

        FileSpec.builder(
            REMOTE_DATA_SOURCE_IMPLEMENTATION_PACKAGE,
            implementationName,
        )
            .addType(
                TypeSpec.classBuilder(implementationName)
                    .primaryConstructor(constructor)
                    .addSuperinterface(interfaceType)
                    .addProperty(
                        PropertySpec.builder("ayanApi", AYAN_API, KModifier.PRIVATE)
                            .initializer("ayanApi")
                            .build()
                    )
                    .apply {
                        processorDataNeeds.forEach { data ->
                            addFunction(createRemoteDataSourceFunction(data))
                        }
                    }
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = true, originatingKSFiles = sourceFiles)
    }

    private fun createRepository(
        separationCategory: String,
        processorDataNeeds: List<ProcessorDataNeed>,
        sourceFiles: List<KSFile>,
    ) {
        val typePrefix = separationCategory.toTypePrefix()
        val remoteDataSourceType = ClassName(
            REMOTE_DATA_SOURCE_INTERFACE_PACKAGE,
            "${typePrefix}RemoteDataSource",
        )
        val interfaceName = "${typePrefix}Repository"
        val implementationName = "${interfaceName}Impl"
        val interfaceType = ClassName(REPOSITORY_INTERFACE_PACKAGE, interfaceName)

        FileSpec.builder(REPOSITORY_INTERFACE_PACKAGE, interfaceName)
            .addType(
                TypeSpec.interfaceBuilder(interfaceName)
                    .apply {
                        processorDataNeeds.forEach { data ->
                            addFunction(createRepositoryFunction(data, abstract = true))
                        }
                    }
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = true, originatingKSFiles = sourceFiles)

        val constructor = FunSpec.constructorBuilder()
            .addParameter("remoteDataSource", remoteDataSourceType)
            .build()

        FileSpec.builder(REPOSITORY_IMPLEMENTATION_PACKAGE, implementationName)
            .addType(
                TypeSpec.classBuilder(implementationName)
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
                        processorDataNeeds.forEach { data ->
                            addFunction(createRepositoryFunction(data))
                        }
                    }
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = true, originatingKSFiles = sourceFiles)
    }

    private fun createRemoteDataSourceFunction(
        data: ProcessorDataNeed,
        abstract: Boolean = false,
    ): FunSpec =
        FunSpec.builder(data.methodName)
            .addModifiers(KModifier.SUSPEND)
            .apply { if (abstract) addModifiers(KModifier.ABSTRACT) }
            .apply { if (!abstract) addModifiers(KModifier.OVERRIDE) }
            .apply { data.requestBodyType?.let { addParameter("requestBody", it) } }
            .addParameter(createBaseUrlParameter(withDefault = abstract))
            .returns(data.resultType)
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

    private fun createRepositoryFunction(
        data: ProcessorDataNeed,
        abstract: Boolean = false,
    ): FunSpec =
        FunSpec.builder(data.methodName)
            .addModifiers(KModifier.SUSPEND)
            .apply { if (abstract) addModifiers(KModifier.ABSTRACT) }
            .apply { if (!abstract) addModifiers(KModifier.OVERRIDE) }
            .apply { data.requestBodyType?.let { addParameter("requestBody", it) } }
            .addParameter(createBaseUrlParameter(withDefault = abstract))
            .returns(data.resultType)
            .apply {
                if (!abstract && data.requestBodyType != null) {
                    addStatement(
                        "return remoteDataSource.%N(requestBody = requestBody, baseUrl = baseUrl)",
                        data.methodName,
                    )
                } else if (!abstract) {
                    addStatement("return remoteDataSource.%N(baseUrl = baseUrl)", data.methodName)
                }
            }
            .build()

    private fun createBaseUrlParameter(withDefault: Boolean): ParameterSpec =
        ParameterSpec.builder("baseUrl", STRING.copy(nullable = true))
            .apply { if (withDefault) defaultValue("null") }
            .build()

    private fun KSClassDeclaration.toDataNeed(): ProcessorDataNeed {
        val nestedClasses =
            this.declarations.filterIsInstance<KSClassDeclaration>().toList()
        val requestBodyType = nestedClasses
            .firstOrNull { it.simpleName.asString().endsWith("RequestBody") }
            ?.asStarProjectedType()?.toTypeName()
        val responseModelType = nestedClasses
            .firstOrNull { it.simpleName.asString().endsWith("ResponseModel") }
            ?.asStarProjectedType()?.toTypeName()
        val apiName = this.simpleName.asString()
        val annotation = this.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == AyanAPI::class.qualifiedName
        }
        val configuredEndPoint = annotation.stringArgument("endpoint")
        val endPoint = configuredEndPoint.takeIf(String::isNotEmpty) ?: apiName
        val methodImplName = annotation.stringArgument("methodImplName")
        val separationCategory = annotation.stringArgument("separationCategory")

        val responseType = responseModelType ?: UNIT
        val resultType = FLOW.parameterizedBy(
            AYAN_API_RESULT.parameterizedBy(
                responseType,
                API_CALL_STATUS,
                Exception::class.asClassName()
            )
        )

        return ProcessorDataNeed(
            requestBodyType = requestBodyType,
            methodName = methodImplName,
            separationCategory = separationCategory,
            endPoint = endPoint,
            responseType = responseType,
            resultType = resultType
        )
    }

    private fun KSAnnotation.stringArgument(name: String): String =
        arguments.first { it.name?.asString() == name }.value as String

    private fun String.toTypePrefix(): String =
        replaceFirstChar { character -> character.uppercase() }

    private companion object {
        const val REMOTE_DATA_SOURCE_INTERFACE_PACKAGE = "ir.ayantech.networking.datasource"
        const val REMOTE_DATA_SOURCE_IMPLEMENTATION_PACKAGE =
            "$REMOTE_DATA_SOURCE_INTERFACE_PACKAGE.impl"
        const val REPOSITORY_INTERFACE_PACKAGE = "ir.ayantech.networking.repository"
        const val REPOSITORY_IMPLEMENTATION_PACKAGE = "$REPOSITORY_INTERFACE_PACKAGE.impl"
        val AYAN_API = ClassName("ir.ayantech.networking.v2", "AyanApi")
        val AYAN_API_RESULT = ClassName("ir.ayantech.networking.v2.api", "AyanAPIResult")
        val API_CALL_STATUS = ClassName("ir.ayantech.networking.v2.model", "ApiCallStatus")
        val FLOW = ClassName("kotlinx.coroutines.flow", "Flow")
        val STRING = String::class.asClassName()
    }

    private data class ProcessorDataNeed(
        val requestBodyType: TypeName?,
        val methodName: String,
        val separationCategory: String,
        val endPoint: String,
        val responseType: TypeName,
        val resultType: TypeName,
    )

    private data class ProcessorInput(
        val data: ProcessorDataNeed,
        val sourceFile: KSFile,
    )
}
