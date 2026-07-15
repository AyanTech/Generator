package com.alirezabdn.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
        val apiClasses = symbols.filterIsInstance<KSClassDeclaration>().filter(KSAnnotated::validate)

        if (apiClasses.isEmpty()) return invalidSymbols

        val functions = apiClasses.flatMap(::createFunctions)
        val sourceFiles = apiClasses.mapNotNull(KSClassDeclaration::containingFile).distinct()

        FileSpec.builder("ir.ayantech.networking", "APIs")
            .apply { functions.forEach(::addFunction) }
            .build()
            .writeTo(codeGenerator, aggregating = true, originatingKSFiles = sourceFiles)

        return invalidSymbols
    }

    private fun createFunctions(apiClass: KSClassDeclaration): List<FunSpec> {
        val nestedClasses = apiClass.declarations.filterIsInstance<KSClassDeclaration>().toList()
        val inputType = nestedClasses.firstOrNull { it.simpleName.asString().endsWith("Input") }
            ?.asStarProjectedType()?.toTypeName()
        val outputType = nestedClasses.firstOrNull { it.simpleName.asString().endsWith("Output") }
            ?.asStarProjectedType()?.toTypeName()
        val apiName = apiClass.simpleName.asString()
        val annotation = apiClass.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == AyanAPI::class.qualifiedName
        }
        val configuredEndPoint = annotation.arguments
            .firstOrNull { it.name?.asString() == "endPoint" }
            ?.value as? String
        val endPoint = configuredEndPoint?.takeIf(String::isNotEmpty) ?: apiName

        val responseType = outputType ?: UNIT
        val resultType = FLOW.parameterizedBy(
            AYAN_API_RESULT.parameterizedBy(responseType, API_CALL_STATUS, Exception::class.asClassName())
        )

        val call = FunSpec.builder("call$apiName")
            .receiver(AYAN_API)
            .apply { if (inputType != null) addParameter("input", inputType) }
            .addParameter(
                ParameterSpec.builder("endPoint", String::class)
                    .defaultValue("%S", endPoint)
                    .build()
            )
            .addParameter(
                ParameterSpec.builder("baseUrl", String::class.asClassName().copy(nullable = true))
                    .defaultValue("null")
                    .build()
            )
            .returns(resultType)
            .apply {
                if (inputType != null) {
                    addStatement(
                        "return post<%T, %T>(body = input, endPint = endPoint, baseUrl = baseUrl)",
                        inputType,
                        responseType,
                    )
                } else {
                    addStatement(
                        "return post<%T, %T>(body = %T, endPint = endPoint, baseUrl = baseUrl)",
                        UNIT,
                        responseType,
                        UNIT,
                    )
                }
            }
            .build()

        return listOf(call)
    }

    private companion object {
        val AYAN_API = ClassName("ir.ayantech.ayannetworking.v2", "AyanApi")
        val AYAN_API_RESULT = ClassName("ir.ayantech.ayannetworking.v2.api", "AyanAPIResult")
        val API_CALL_STATUS = ClassName("ir.ayantech.ayannetworking.v2.model", "ApiCallStatus")
        val FLOW = ClassName("kotlinx.coroutines.flow", "Flow")
    }
}
