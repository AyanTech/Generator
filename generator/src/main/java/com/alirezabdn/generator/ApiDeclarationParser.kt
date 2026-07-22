package com.alirezabdn.generator

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toTypeName

/** Converts an annotated KSP class into the generator's framework-independent input model. */
internal class ApiDeclarationParser {

    /** Parses one validated [AyanAPI] declaration and records its originating source file. */
    fun parse(declaration: KSClassDeclaration): ProcessorInput =
        ProcessorInput(
            data = declaration.toDataNeed(),
            sourceFile = requireNotNull(declaration.containingFile),
        )

    private fun KSClassDeclaration.toDataNeed(): ProcessorDataNeed {
        val nestedClasses = declarations.filterIsInstance<KSClassDeclaration>().toList()
        val requestBodyType = nestedClasses
            .firstOrNull { it.simpleName.asString().endsWith("RequestBody") }
            ?.asStarProjectedType()
            ?.toTypeName()
        val responseModelType = nestedClasses
            .firstOrNull { it.simpleName.asString().endsWith("ResponseModel") }
            ?.asStarProjectedType()
            ?.toTypeName()
        val apiName = simpleName.asString()
        val annotation = annotations.first {
            it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                AyanAPI::class.qualifiedName
        }
        val configuredEndPoint = annotation.stringArgument("endpoint")
        val responseType = responseModelType ?: UNIT

        return ProcessorDataNeed(
            requestBodyType = requestBodyType,
            methodName = annotation.stringArgument("methodImplName"),
            separationCategory = annotation.stringArgument("separationCategory"),
            endPoint = configuredEndPoint.takeIf(String::isNotEmpty) ?: apiName,
            responseType = responseType,
            resultType = GeneratorTypes.flow.parameterizedBy(
                GeneratorTypes.ayanApiResult.parameterizedBy(
                    responseType,
                    GeneratorTypes.apiCallStatus,
                    Exception::class.asClassName(),
                )
            ),
        )
    }

    private fun KSAnnotation.stringArgument(name: String): String =
        arguments.first { it.name?.asString() == name }.value as String
}
