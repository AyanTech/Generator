package com.alirezabdn.generator

import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.TypeName

/**
 * Normalized API information consumed by all source generators.
 *
 * @property requestBodyType nested request model, or `null` when an endpoint has no body.
 * @property methodName generated data-source and repository method name.
 * @property separationCategory category used to group APIs into generated types.
 * @property endPoint endpoint passed to `AyanApi.post`.
 * @property responseType nested response model, or `Unit` when it is absent.
 * @property resultType complete flow result returned by the generated method.
 */
internal data class ProcessorDataNeed(
    val requestBodyType: TypeName?,
    val methodName: String,
    val separationCategory: String,
    val endPoint: String,
    val responseType: TypeName,
    val resultType: TypeName,
) {
    val mockResultPropertyName: String
        get() = "${methodName}Result"

    val mockCallCountPropertyName: String
        get() = "${methodName}CallCount"

    val capturedRequestBodyPropertyName: String
        get() = "last${methodName.toTypePrefix()}RequestBody"

    val capturedBaseUrlPropertyName: String
        get() = "last${methodName.toTypePrefix()}BaseUrl"
}

/** Associates parsed API data with the KSP source file that produced it. */
internal data class ProcessorInput(
    val data: ProcessorDataNeed,
    val sourceFile: KSFile,
)

/**
 * APIs and originating files that contribute to one generated category.
 *
 * Keeping this data together ensures every aggregating output declares the same KSP dependencies.
 */
internal data class GenerationBatch(
    val separationCategory: String,
    val dataNeeds: List<ProcessorDataNeed>,
    val sourceFiles: List<KSFile>,
) {
    val typePrefix: String
        get() = separationCategory.toTypePrefix()
}

/** Converts an annotation category or method name into a generated type-name prefix. */
internal fun String.toTypePrefix(): String =
    replaceFirstChar { character -> character.uppercase() }
