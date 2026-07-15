package com.alirezabdn.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class ProcessorTest {

    @Test
    fun `generates typed v2 calls with default and configured endpoints`() {
        val generated = compile(
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI
            class GetProfile {
                data class Input(val userId: String)
                data class Output(val displayName: String)
            }

            @AyanAPI(endPoint = "CustomEndpoint")
            class UpdateProfile {
                data class Input(val displayName: String)
                data class Output(val updated: Boolean)
            }
            """.trimIndent()
        )

        assertContains(generated, "public fun AyanApi.callGetProfile(")
        assertContains(generated, "input: GetProfile.Input")
        assertContains(generated, "endPoint: String = \"GetProfile\"")
        assertContains(generated, "baseUrl: String? = null")
        assertContains(
            generated,
            "Flow<AyanAPIResult<GetProfile.Output, ApiCallStatus, Exception>>",
        )
        assertContains(
            generated,
            "post<GetProfile.Input, GetProfile.Output>(body = input, endPint = endPoint, baseUrl = baseUrl)",
        )
        assertContains(generated, "endPoint: String = \"CustomEndpoint\"")
    }

    @Test
    fun `uses Unit when input or output is missing`() {
        val generated = compile(
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI
            class NoInput {
                data class Output(val value: String)
            }

            @AyanAPI
            class NoOutput {
                data class Input(val value: String)
            }
            """.trimIndent()
        )

        assertContains(
            generated,
            "post<Unit, NoInput.Output>(body = Unit, endPint = endPoint, baseUrl = baseUrl)",
        )
        assertContains(generated, "Flow<AyanAPIResult<Unit, ApiCallStatus, Exception>>")
        assertContains(
            generated,
            "post<NoOutput.Input, Unit>(body = input, endPint = endPoint, baseUrl = baseUrl)",
        )
    }

    private fun compile(apiSource: String): String {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = listOf(
                SourceFile.kotlin("TestApi.kt", apiSource),
                SourceFile.kotlin("AyanApiStub.kt", AYAN_API_STUB),
                SourceFile.kotlin("AyanApiResultStub.kt", AYAN_API_RESULT_STUB),
                SourceFile.kotlin("ApiCallStatusStub.kt", API_CALL_STATUS_STUB),
                SourceFile.kotlin("FlowStub.kt", FLOW_STUB),
            )
            symbolProcessorProviders.add(ProcessorProvider())
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        return File(compilation.kspSourcesDir, "kotlin/ir/ayantech/networking/APIs.kt")
            .readText()
    }

    private companion object {
        val AYAN_API_STUB =
            """
            package ir.ayantech.ayannetworking.v2

            import ir.ayantech.ayannetworking.v2.api.AyanAPIResult
            import ir.ayantech.ayannetworking.v2.model.ApiCallStatus
            import kotlinx.coroutines.flow.Flow

            class AyanApi {
                inline fun <reified Body, reified Response> post(
                    body: Body,
                    endPint: String,
                    baseUrl: String? = null,
                ): Flow<AyanAPIResult<Response, ApiCallStatus, Exception>> = error("stub")
            }
            """.trimIndent()

        val AYAN_API_RESULT_STUB =
            """
            package ir.ayantech.ayannetworking.v2.api

            class AyanAPIResult<T, J, K>
            """.trimIndent()

        val API_CALL_STATUS_STUB =
            """
            package ir.ayantech.ayannetworking.v2.model

            enum class ApiCallStatus { IDLE }
            """.trimIndent()

        val FLOW_STUB =
            """
            package kotlinx.coroutines.flow

            interface Flow<T>
            """.trimIndent()
    }
}
