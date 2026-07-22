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
    fun `uses annotation endpoint and method name in data source and repository`() {
        val compilation = compileProject(
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI(
                endpoint = "",
                methodImplName = "getProfile",
                separationCategory = "Profile",
            )
            class GetProfile {
                data class GetProfileRequestBody(val userId: String)
                data class GetProfileResponseModel(val displayName: String)
            }

            @AyanAPI(
                endpoint = "CustomEndpoint",
                methodImplName = "updateProfile",
                separationCategory = "Profile",
            )
            class UpdateProfile {
                data class UpdateProfileRequestBody(val displayName: String)
                data class UpdateProfileResponseModel(val updated: Boolean)
            }
            """.trimIndent()
        )

        val implementation = remoteDataSourceImplementation(compilation, "Profile")
        val repositoryImplementation = repositoryImplementation(compilation, "Profile")

        assertContains(implementation, "override suspend fun getProfile(")
        assertContains(implementation, "override suspend fun updateProfile(")
        assertContains(implementation, "requestBody: GetProfile.GetProfileRequestBody")
        assertContains(
            implementation,
            "Flow<AyanAPIResult<GetProfile.GetProfileResponseModel, ApiCallStatus, Exception>>",
        )
        assertContains(
            implementation,
            "ayanApi.post<GetProfile.GetProfileRequestBody, GetProfile.GetProfileResponseModel>(body = requestBody, endPoint = \"GetProfile\", baseUrl = baseUrl)",
        )
        assertContains(implementation, "endPoint = \"CustomEndpoint\"")
        assertContains(repositoryImplementation, "private val remoteDataSource: ProfileRemoteDataSource")
        assertContains(
            repositoryImplementation,
            "= remoteDataSource.getProfile(requestBody = requestBody, baseUrl = baseUrl)",
        )
        assertContains(
            repositoryImplementation,
            "= remoteDataSource.updateProfile(requestBody = requestBody, baseUrl = baseUrl)",
        )
        assertFalse(File(compilation.kspSourcesDir, "kotlin/ir/ayantech/networking/APIs.kt").exists())
    }

    @Test
    fun `uses Unit when input or output is missing`() {
        val compilation = compileProject(
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI(
                endpoint = "NoInput",
                methodImplName = "getNoInput",
                separationCategory = "Utility",
            )
            class NoInput {
                data class NoInputResponseModel(val value: String)
            }

            @AyanAPI(
                endpoint = "NoOutput",
                methodImplName = "sendNoOutput",
                separationCategory = "Utility",
            )
            class NoOutput {
                data class NoOutputRequestBody(val value: String)
            }
            """.trimIndent()
        )

        assertContains(
            remoteDataSourceImplementation(compilation, "Utility"),
            "ayanApi.post<Unit, NoInput.NoInputResponseModel>(body = Unit, endPoint = \"NoInput\", baseUrl = baseUrl)",
        )
        assertContains(
            remoteDataSourceImplementation(compilation, "Utility"),
            "Flow<AyanAPIResult<Unit, ApiCallStatus, Exception>>",
        )
        assertContains(
            remoteDataSourceImplementation(compilation, "Utility"),
            "ayanApi.post<NoOutput.NoOutputRequestBody, Unit>(body = requestBody, endPoint = \"NoOutput\", baseUrl = baseUrl)",
        )
        assertContains(
            repositoryImplementation(compilation, "Utility"),
            "= remoteDataSource.getNoInput(baseUrl = baseUrl)",
        )
    }

    @Test
    fun `generates a data source and repository pair for every separation category`() {
        val compilation = compileProject(
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI(
                endpoint = "GetProfile",
                methodImplName = "getProfile",
                separationCategory = "Profile",
            )
            class GetProfile {
                data class GetProfileRequestBody(val userId: String)
                data class GetProfileResponseModel(val displayName: String)
            }
            """.trimIndent(),
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI(
                endpoint = "GetStatus",
                methodImplName = "getStatus",
                separationCategory = "Status",
            )
            class GetStatus {
                data class GetStatusResponseModel(val status: String)
            }
            """.trimIndent(),
            """
            package test

            import com.alirezabdn.generator.AyanAPI

            @AyanAPI(
                endpoint = "UpdateProfile",
                methodImplName = "updateProfile",
                separationCategory = "Profile",
            )
            class UpdateProfile {
                data class UpdateProfileRequestBody(val name: String)
                data class UpdateProfileResponseModel(val updated: Boolean)
            }

            @AyanAPI(
                endpoint = "SubmitPayment",
                methodImplName = "submitPayment",
                separationCategory = "Payment",
            )
            class SubmitPayment {
                data class SubmitPaymentRequestBody(val amount: Long)
                data class SubmitPaymentResponseModel(val accepted: Boolean)
            }
            """.trimIndent()
        )

        val dataSourceDirectory = File(
            compilation.kspSourcesDir,
            "kotlin/ir/ayantech/networking/datasource",
        )
        val repositoryDirectory = File(
            compilation.kspSourcesDir,
            "kotlin/ir/ayantech/networking/repository",
        )
        val profileDataSource = File(dataSourceDirectory, "ProfileRemoteDataSource.kt").readText()
        val profileDataSourceImpl =
            File(dataSourceDirectory, "impl/ProfileRemoteDataSourceImpl.kt").readText()
        val profileRepository = File(repositoryDirectory, "ProfileRepository.kt").readText()
        val profileRepositoryImpl =
            File(repositoryDirectory, "impl/ProfileRepositoryImpl.kt").readText()

        assertEquals(
            listOf(
                "PaymentRemoteDataSource.kt",
                "ProfileRemoteDataSource.kt",
                "StatusRemoteDataSource.kt",
            ),
            dataSourceDirectory.kotlinFileNames(),
        )
        assertEquals(
            listOf(
                "PaymentRemoteDataSourceImpl.kt",
                "ProfileRemoteDataSourceImpl.kt",
                "StatusRemoteDataSourceImpl.kt",
            ),
            File(dataSourceDirectory, "impl").kotlinFileNames(),
        )
        assertEquals(
            listOf("PaymentRepository.kt", "ProfileRepository.kt", "StatusRepository.kt"),
            repositoryDirectory.kotlinFileNames(),
        )
        assertEquals(
            listOf("PaymentRepositoryImpl.kt", "ProfileRepositoryImpl.kt", "StatusRepositoryImpl.kt"),
            File(repositoryDirectory, "impl").kotlinFileNames(),
        )
        assertContains(profileDataSource, "public interface ProfileRemoteDataSource")
        assertContains(profileDataSource, "public suspend fun getProfile(")
        assertContains(profileDataSource, "public suspend fun updateProfile(")
        assertContains(profileDataSource, "baseUrl: String? = null")
        assertContains(
            profileDataSource,
            "Flow<AyanAPIResult<GetProfile.GetProfileResponseModel, ApiCallStatus, Exception>>",
        )
        assertContains(profileDataSourceImpl, "public class ProfileRemoteDataSourceImpl(")
        assertContains(profileDataSourceImpl, ": ProfileRemoteDataSource")
        assertContains(profileDataSourceImpl, "override suspend fun getProfile(")
        assertContains(profileDataSourceImpl, "override suspend fun updateProfile(")
        assertContains(profileRepository, "public interface ProfileRepository")
        assertContains(profileRepository, "public suspend fun getProfile(")
        assertContains(profileRepository, "public suspend fun updateProfile(")
        assertContains(profileRepository, "baseUrl: String? = null")
        assertContains(profileRepositoryImpl, "public class ProfileRepositoryImpl(")
        assertContains(profileRepositoryImpl, ": ProfileRepository")
        assertContains(profileRepositoryImpl, "override suspend fun getProfile(")
        assertContains(profileRepositoryImpl, "override suspend fun updateProfile(")
        assertContains(
            profileRepositoryImpl,
            "= remoteDataSource.getProfile(requestBody = requestBody, baseUrl = baseUrl)",
        )
        assertContains(
            profileRepositoryImpl,
            "= remoteDataSource.updateProfile(requestBody = requestBody, baseUrl = baseUrl)",
        )
    }

    private fun remoteDataSourceImplementation(
        compilation: KotlinCompilation,
        category: String,
    ): String =
        File(
            compilation.kspSourcesDir,
            "kotlin/ir/ayantech/networking/datasource/impl/${category}RemoteDataSourceImpl.kt",
        ).readText()

    private fun repositoryImplementation(
        compilation: KotlinCompilation,
        category: String,
    ): String =
        File(
            compilation.kspSourcesDir,
            "kotlin/ir/ayantech/networking/repository/impl/${category}RepositoryImpl.kt",
        ).readText()

    private fun File.kotlinFileNames(): List<String> =
        listFiles()
            .orEmpty()
            .filter { it.extension == "kt" }
            .map(File::getName)
            .sorted()

    private fun compileProject(vararg apiSources: String): KotlinCompilation {
        val compilation = KotlinCompilation().apply {
            useKsp2()
            sources = apiSources.mapIndexed { index, source ->
                SourceFile.kotlin("TestApi$index.kt", source)
            } + listOf(
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
            package ir.ayantech.networking.v2

            import ir.ayantech.networking.v2.api.AyanAPIResult
            import ir.ayantech.networking.v2.model.ApiCallStatus
            import kotlinx.coroutines.flow.Flow

            class AyanApi {
                inline fun <reified Body, reified Response> post(
                    body: Body,
                    endPoint: String,
                    baseUrl: String? = null,
                ): Flow<AyanAPIResult<Response, ApiCallStatus, Exception>> = error("stub")
            }
            """.trimIndent()

        val AYAN_API_RESULT_STUB =
            """
            package ir.ayantech.networking.v2.api

            class AyanAPIResult<T, J, K>
            """.trimIndent()

        val API_CALL_STATUS_STUB =
            """
            package ir.ayantech.networking.v2.model

            enum class ApiCallStatus { IDLE }
            """.trimIndent()

        val FLOW_STUB =
            """
            package kotlinx.coroutines.flow

            interface Flow<T>
            """.trimIndent()
    }
}
