[![](https://jitpack.io/v/AyanTech/Generator.svg)](https://jitpack.io/#AyanTech/Generator)

# Generator

Generator is a Kotlin Symbol Processing (KSP) library that generates category-based remote data source and repository interfaces, along with their default implementations, for [Ayan Networking](https://github.com/AyanTech/Networking) v2 APIs.

Describe APIs as classes annotated with `@AyanAPI` and add nested request and response models. Generator groups APIs by category and creates a remote data source and repository pair for every category at compile time.

## Requirements

- Java 17
- Kotlin with KSP
- Ayan Networking v2
- Kotlin coroutines

## Installation

Add JitPack to your dependency repositories:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Enable KSP and add Generator to both the implementation and KSP configurations:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
}

dependencies {
    implementation("com.github.AyanTech:Generator:<Generator-version>")
    ksp("com.github.AyanTech:Generator:<Generator-version>")

    implementation("com.github.AyanTech:Networking:<networking-version>")
}
```

Use a KSP version compatible with the Kotlin version used by your project.

## Define an API

Annotate a class with `@AyanAPI`. Nested classes whose names end in `RequestBody` and `ResponseModel` are used as the request and response types:

```kotlin
import com.alirezabdn.generator.AyanAPI
import kotlinx.serialization.Serializable

@AyanAPI(
    endpoint = "GetUserProfile",
    methodImplName = "getProfile",
    separationCategory = "Profile",
)
class GetProfile {
    @Serializable
    data class GetProfileRequestBody(
        val userId: String,
    )

    @Serializable
    data class GetProfileResponseModel(
        val displayName: String,
    )
}
```

The annotation values control generation:

- `endpoint` is sent to `AyanApi.post`.
- `methodImplName` is used for the generated data source and repository method.
- `separationCategory` groups related APIs and provides the generated type prefix.

For the `Profile` category, Generator creates these production and test-support types:

- `ProfileRemoteDataSource`
- `ProfileRemoteDataSourceImpl`
- `ProfileRemoteDataSourceMock`
- `ProfileRepository`
- `ProfileRepositoryImpl`
- `ProfileRepositoryMock`

All APIs with `separationCategory = "Profile"` contribute methods to those same six types:

```kotlin
interface ProfileRemoteDataSource {
    fun getProfile(
        requestBody: GetProfile.GetProfileRequestBody,
        baseUrl: String? = null,
    ): Flow<AyanAPIResult<GetProfile.GetProfileResponseModel, ApiCallStatus, Exception>>
}

class ProfileRemoteDataSourceImpl(
    private val ayanApi: AyanApi,
) : ProfileRemoteDataSource {
    override fun getProfile(
        requestBody: GetProfile.GetProfileRequestBody,
        baseUrl: String?,
    ) =
        ayanApi.post<GetProfile.GetProfileRequestBody, GetProfile.GetProfileResponseModel>(
            body = requestBody,
            endPoint = "GetUserProfile",
            baseUrl = baseUrl,
        )
}

interface ProfileRepository {
    fun getProfile(
        requestBody: GetProfile.GetProfileRequestBody,
        baseUrl: String? = null,
    ): Flow<AyanAPIResult<GetProfile.GetProfileResponseModel, ApiCallStatus, Exception>>
}

class ProfileRepositoryImpl(
    private val remoteDataSource: ProfileRemoteDataSource,
) : ProfileRepository {
    override fun getProfile(
        requestBody: GetProfile.GetProfileRequestBody,
        baseUrl: String?,
    ) = remoteDataSource.getProfile(requestBody, baseUrl)
}
```

The implementations are dependency-injection-framework agnostic. Provide the
generated remote data source and repository implementations from your DI module,
or construct them directly.

Every generated method accepts `baseUrl: String? = null`. Omit it to use the
`AyanApi` configuration, or provide a URL to override the base URL for that call.

## Call the generated repository

Construct the generated implementations or provide them through your DI framework:

```kotlin
import ir.ayantech.networking.v2.api.onChangeState
import ir.ayantech.networking.v2.api.onFailure
import ir.ayantech.networking.v2.api.onSuccess
import kotlinx.coroutines.launch

lifecycleScope.launch {
    val remoteDataSource = ProfileRemoteDataSourceImpl(ayanApi)
    val repository = ProfileRepositoryImpl(remoteDataSource)
    repository.getProfile(
        requestBody = GetProfile.GetProfileRequestBody(userId = "123"),
    ).collect { result ->
        result.onSuccess { profile ->
            println(profile.displayName)
        }

        result.onFailure { exception ->
            println(exception.message)
        }

        result.onChangeState { state ->
            println(state)
        }
    }
}
```

## APIs without a request or response

Both nested models are optional.

An API with no request body generates a data source method with no `requestBody` parameter:

```kotlin
@AyanAPI(
    endpoint = "GetStatus",
    methodImplName = "getStatus",
    separationCategory = "Status",
)
class GetStatus {
    data class GetStatusResponseModel(val status: String)
}

val repository = StatusRepositoryImpl(StatusRemoteDataSourceImpl(ayanApi))
repository.getStatus()
```

An API with no response model returns an `AyanAPIResult` whose response type is `Unit`:

```kotlin
@AyanAPI(
    endpoint = "SendEvent",
    methodImplName = "sendEvent",
    separationCategory = "Event",
)
class SendEvent {
    data class SendEventRequestBody(val name: String)
}

val repository = EventRepositoryImpl(EventRemoteDataSourceImpl(ayanApi))
repository.sendEvent(SendEvent.SendEventRequestBody(name = "opened"))
```

If neither model is present, Generator uses `Unit` for both the request and response.

## Unit-test mocks and mock data

Generator creates framework-free mocks in
`ir.ayantech.networking.datasource.mock` and
`ir.ayantech.networking.repository.mock`. Every generated API method has:

- A mutable `<methodName>Result` flow, initialized with `emptyFlow()`.
- A read-only-from-tests `<methodName>CallCount` invocation counter.
- A captured `last<MethodName>RequestBody` when the API has a request model.
- A captured `last<MethodName>BaseUrl`.

This lets tests supply mock responses and verify repository delegation without a
mocking framework:

```kotlin
@Test
fun `getProfile delegates its arguments`() = runTest {
    val request = GetProfile.GetProfileRequestBody(userId = "123")
    val expectedResult = flowOf(mockApiResult)
    val remoteDataSource = ProfileRemoteDataSourceMock().apply {
        getProfileResult = expectedResult
    }
    val repository = ProfileRepositoryImpl(remoteDataSource)

    val actualResult = repository.getProfile(request, baseUrl = "https://example.test")

    assertSame(expectedResult, actualResult)
    assertEquals(1, remoteDataSource.getProfileCallCount)
    assertEquals(request, remoteDataSource.lastGetProfileRequestBody)
    assertEquals("https://example.test", remoteDataSource.lastGetProfileBaseUrl)
}
```

Use the generated repository mock in the same way when unit-testing consumers
of a repository.

## Naming rules

- The annotated declaration must be a class.
- `methodImplName` must be a valid Kotlin function name and should be unique within its category.
- The first character of `separationCategory` is capitalized and used as the type prefix.
- Every distinct category generates `<Category>RemoteDataSource`,
  `<Category>RemoteDataSourceImpl`, `<Category>RemoteDataSourceMock`,
  `<Category>Repository`, `<Category>RepositoryImpl`, and
  `<Category>RepositoryMock`.
- The request model is the first nested class whose name ends with `RequestBody`.
- The response model is the first nested class whose name ends with `ResponseModel`.
- Data source interfaces and implementations are written to
  `ir.ayantech.networking.datasource` and `ir.ayantech.networking.datasource.impl`.
- Data source mocks are written to `ir.ayantech.networking.datasource.mock`.
- Repository interfaces and implementations are written to
  `ir.ayantech.networking.repository` and `ir.ayantech.networking.repository.impl`.
- Repository mocks are written to `ir.ayantech.networking.repository.mock`.

## Sample app architecture

The `app` module demonstrates Clean Architecture with MVVM and Koin while using
the generated networking layer:

```text
MainActivity
    → DonationViewModel
    → GetDonationReferrerTypesUseCase
    → domain.repository.DonationRepository
    → data.repository.DonationRepositoryImpl
    → generated DonationRepositoryImpl
    → generated DonationRemoteDataSourceImpl
    → AyanApi
```

- `data` owns annotated DTOs, generated-network adaptation, and domain mapping.
- `domain` owns models, the repository contract, and use cases without Android
  or Ayan Networking dependencies.
- `presentation` owns immutable UI state, the ViewModel, and Activity rendering.
- `di` binds the generated data source and repository into the domain graph.

`GeneratorApplication` starts Koin, and `MainActivity` obtains
`DonationViewModel` through Koin's lifecycle-aware ViewModel delegate.

## Build and test

Run the processor tests with:

```shell
./gradlew :generator:test
```

Run the sample app unit tests with:

```shell
./gradlew :app:testDebugUnitTest
```

Build the complete project with:

```shell
./gradlew build
```
