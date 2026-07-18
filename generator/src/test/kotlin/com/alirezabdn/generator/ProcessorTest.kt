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
import kotlin.test.assertFalse
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class ProcessorTest {

    @Test
    fun `generates typed data sources with default and configured endpoints`() {
        val compilation = compileProject(
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

        val profile = implementation(compilation, "GetProfile")
        val update = implementation(compilation, "UpdateProfile")

        assertContains(profile, "input: GetProfile.Input")
        assertContains(
            profile,
            "Flow<AyanAPIResult<GetProfile.Output, ApiCallStatus, Exception>>",
        )
        assertContains(
            profile,
            "ayanApi.post<GetProfile.Input, GetProfile.Output>(body = input, endPint = \"GetProfile\", baseUrl = null)",
        )
        assertContains(update, "endPint = \"CustomEndpoint\"")
        assertFalse(File(compilation.kspSourcesDir, "kotlin/ir/ayantech/networking/APIs.kt").exists())
    }

    @Test
    fun `uses Unit when input or output is missing`() {
        val compilation = compileProject(
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
            implementation(compilation, "NoInput"),
            "ayanApi.post<Unit, NoInput.Output>(body = Unit, endPint = \"NoInput\", baseUrl = null)",
        )
        assertContains(
            implementation(compilation, "NoOutput"),
            "Flow<AyanAPIResult<Unit, ApiCallStatus, Exception>>",
        )
        assertContains(
            implementation(compilation, "NoOutput"),
            "ayanApi.post<NoOutput.Input, Unit>(body = input, endPint = \"NoOutput\", baseUrl = null)",
        )
    }

    @Test
    fun `generates a remote data source interface and implementation for every API`() {
        val compilation = compileProject(
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI
            class GetProfile {
                data class Input(val userId: String)
                data class Output(val displayName: String)
            }

            @AyanAPI
            class GetStatus {
                data class Output(val status: String)
            }
            """.trimIndent()
        )

        val interfaceDirectory = File(
            compilation.kspSourcesDir,
            "kotlin/ir/ayantech/networking/datasource",
        )
        val implementationDirectory = File(interfaceDirectory, "impl")
        val profileInterface = File(interfaceDirectory, "GetProfileRemoteDataSource.kt").readText()
        val profileImplementation =
            File(implementationDirectory, "GetProfileRemoteDataSourceImpl.kt").readText()
        val statusImplementation =
            File(implementationDirectory, "GetStatusRemoteDataSourceImpl.kt").readText()

        assertContains(profileInterface, "package ir.ayantech.networking.datasource")
        assertContains(profileInterface, "public interface GetProfileRemoteDataSource")
        assertContains(profileInterface, "public operator fun invoke(")
        assertContains(profileInterface, "input: GetProfile.Input")
        assertContains(
            profileInterface,
            "Flow<AyanAPIResult<GetProfile.Output, ApiCallStatus, Exception>>",
        )
        assertContains(
            profileImplementation,
            "package ir.ayantech.networking.datasource.`impl`",
        )
        assertContains(
            profileImplementation,
            "public class GetProfileRemoteDataSourceImpl(",
        )
        assertContains(profileImplementation, "private val ayanApi: AyanApi")
        assertContains(profileImplementation, ": GetProfileRemoteDataSource")
        assertContains(profileImplementation, "override operator fun invoke(")
        assertContains(profileImplementation, "ayanApi.post<GetProfile.Input, GetProfile.Output>")
        assertContains(statusImplementation, "ayanApi.post<Unit, GetStatus.Output>")
    }

    private fun implementation(compilation: KotlinCompilation, apiName: String): String =
        File(
            compilation.kspSourcesDir,
            "kotlin/ir/ayantech/networking/datasource/impl/${apiName}RemoteDataSourceImpl.kt",
        ).readText()

    private fun compileProject(apiSource: String): KotlinCompilation {
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
        return compilation
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
