package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
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

        if (apiClasses.isEmpty()) return invalidSymbols

        apiClasses.forEach(::createRemoteDataSource)

        return invalidSymbols
    }

    private fun createRemoteDataSource(classDeclaration: KSClassDeclaration) {
        val processorDataNeed = classDeclaration.toDataNeed()
        val apiName = classDeclaration.simpleName.asString()
        val interfaceName = "${apiName}RemoteDataSource"
        val implementationName = "${interfaceName}Impl"
        val interfaceType = ClassName(REMOTE_DATA_SOURCE_INTERFACE_PACKAGE, interfaceName)
        val sourceFile = requireNotNull(classDeclaration.containingFile)

        val interfaceFunction = createRemoteDataSourceFunction(processorDataNeed, abstract = true)
        FileSpec.builder(REMOTE_DATA_SOURCE_INTERFACE_PACKAGE, interfaceName)
            .addType(
                TypeSpec.interfaceBuilder(interfaceName)
                    .addFunction(interfaceFunction)
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = false, originatingKSFiles = listOf(sourceFile))

        val constructor = FunSpec.constructorBuilder()
            .addParameter("ayanApi", AYAN_API)
            .build()
        val implementationFunction = createRemoteDataSourceFunction(processorDataNeed)

        FileSpec.builder(REMOTE_DATA_SOURCE_IMPLEMENTATION_PACKAGE, implementationName)
            .addType(
                TypeSpec.classBuilder(implementationName)
                    .primaryConstructor(constructor)
                    .addSuperinterface(interfaceType)
                    .addProperty(
                        PropertySpec.builder("ayanApi", AYAN_API, KModifier.PRIVATE)
                            .initializer("ayanApi")
                            .build()
                    )
                    .addFunction(implementationFunction)
                    .build()
            )
            .build()
            .writeTo(codeGenerator, aggregating = false, originatingKSFiles = listOf(sourceFile))
    }

    private fun createRemoteDataSourceFunction(
        data: ProcessorDataNeed,
        abstract: Boolean = false,
    ): FunSpec =
        FunSpec.builder(data.methodName)
            .addModifiers(KModifier.OPERATOR)
            .apply { if (abstract) addModifiers(KModifier.ABSTRACT) }
            .apply { if (!abstract) addModifiers(KModifier.OVERRIDE) }
            .apply { data.requestBodyType?.let { addParameter("requestBody", it) } }
            .returns(data.resultType)
            .apply {
                if (!abstract && data.requestBodyType != null) {
                    addStatement(
                        "return ayanApi.post<%T, %T>(body = requestBody, endPint = %S, baseUrl = null)",
                        data.requestBodyType,
                        data.responseType,
                        data.endPoint,
                    )
                } else if (!abstract) {
                    addStatement(
                        "return ayanApi.post<%T, %T>(body = %T, endPint = %S, baseUrl = null)",
                        UNIT,
                        data.responseType,
                        UNIT,
                        data.endPoint,
                    )
                }
            }
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
        val configuredEndPoint = annotation.arguments
            .firstOrNull { it.name?.asString() == "endPoint" }
            ?.value as? String
        val endPoint = configuredEndPoint?.takeIf(String::isNotEmpty) ?: apiName

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
            methodName = "invoke",
            endPoint = endPoint,
            responseType = responseType,
            resultType = resultType
        )
    }

    private companion object {
        const val REMOTE_DATA_SOURCE_INTERFACE_PACKAGE = "ir.ayantech.networking.datasource"
        const val REMOTE_DATA_SOURCE_IMPLEMENTATION_PACKAGE =
            "$REMOTE_DATA_SOURCE_INTERFACE_PACKAGE.impl"
        val AYAN_API = ClassName("ir.ayantech.ayannetworking.v2", "AyanApi")
        val AYAN_API_RESULT = ClassName("ir.ayantech.ayannetworking.v2.api", "AyanAPIResult")
        val API_CALL_STATUS = ClassName("ir.ayantech.ayannetworking.v2.model", "ApiCallStatus")
        val FLOW = ClassName("kotlinx.coroutines.flow", "Flow")
    }

    private data class ProcessorDataNeed(
        val requestBodyType: TypeName?,
        val methodName: String,
        val endPoint: String,
        val responseType: TypeName,
        val resultType: TypeName,
    )
}
