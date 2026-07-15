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
import com.squareup.kotlinpoet.LambdaTypeName
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

        val simpleCall = FunSpec.builder("simpleCall$apiName")
            .receiver(AYAN_API)
            .apply { if (inputType != null) addParameter("input", inputType) }
            .addParameter(
                ParameterSpec.builder("endPoint", String::class)
                    .defaultValue("%S", endPoint)
                    .build()
            )
            .addParameter(
                "callback",
                LambdaTypeName.get(
                    parameters = outputType
                        ?.copy(nullable = true)
                        ?.let { listOf(ParameterSpec.unnamed(it)) }
                        .orEmpty(),
                    returnType = UNIT,
                )
            )
            .addStatement("this.simpleCall<%T>(endPoint, %L)", outputType ?: Void::class.asClassName(), inputType?.let { "input" } ?: "null")
            .addStatement("{ callback(%L) }", if (outputType != null) "it" else "")
            .build()

        val call = FunSpec.builder("call$apiName")
            .receiver(AYAN_API)
            .apply { if (inputType != null) addParameter("input", inputType) }
            .addParameter(
                ParameterSpec.builder("endPoint", String::class)
                    .defaultValue("%S", endPoint)
                    .build()
            )
            .addParameter(
                "callback",
                LambdaTypeName.get(
                    receiver = AYAN_API_CALLBACK.parameterizedBy(outputType ?: Void::class.asClassName()),
                    returnType = UNIT,
                )
            )
            .addStatement("this.call<%T>(endPoint, %L, callback)", outputType ?: Void::class.asClassName(), inputType?.let { "input" } ?: "null")
            .build()

        return listOf(simpleCall, call)
    }

    private companion object {
        val AYAN_API = ClassName("ir.ayantech.ayannetworking.api", "AyanApi")
        val AYAN_API_CALLBACK = ClassName("ir.ayantech.ayannetworking.api", "AyanApiCallback")
    }
}
