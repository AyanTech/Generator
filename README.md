# Generator

Generator is a Kotlin Symbol Processing (KSP) library that generates type-safe extension functions for [Ayan Networking](https://github.com/AyanTech/Networking) v2 APIs.

Describe an API as a class annotated with `@AyanAPI`, add nested `Input` and `Output` models, and Generator creates an `AyanApi.call<ClassName>(...)` function for it at compile time.

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

Annotate a class with `@AyanAPI`. Nested classes whose names end in `Input` and `Output` are used as the request and response types:

```kotlin
import com.alirezabdn.generator.AyanAPI
import kotlinx.serialization.Serializable

@AyanAPI
class GetProfile {
    @Serializable
    data class Input(
        val userId: String,
    )

    @Serializable
    data class Output(
        val displayName: String,
    )
}
```

Generator creates a remote data source contract in
`ir.ayantech.networking.datasource` and its default implementation in
`ir.ayantech.networking.datasource.impl`:

```kotlin
interface GetProfileRemoteDataSource {
    operator fun invoke(
        input: GetProfile.Input,
    ): Flow<AyanAPIResult<GetProfile.Output, ApiCallStatus, Exception>>
}

class GetProfileRemoteDataSourceImpl(
    private val ayanApi: AyanApi,
) : GetProfileRemoteDataSource {
    override operator fun invoke(input: GetProfile.Input) =
        ayanApi.post<GetProfile.Input, GetProfile.Output>(
            body = input,
            endPint = "GetProfile",
            baseUrl = null,
        )
}
```

The implementation is dependency-injection-framework agnostic, so provide the
generated class from your DI module or construct it directly.

The endpoint defaults to the annotated class name. Override it when the server uses a different name:

```kotlin
@AyanAPI(endPoint = "GetUserProfile")
class GetProfile {
    // Input and Output models
}
```

## Call the generated data source

Construct the generated implementation (or provide it through your DI framework)
and invoke the data source method:

```kotlin
import ir.ayantech.ayannetworking.v2.api.onChangeState
import ir.ayantech.ayannetworking.v2.api.onFailure
import ir.ayantech.ayannetworking.v2.api.onSuccess
import kotlinx.coroutines.launch

lifecycleScope.launch {
    val dataSource = GetProfileRemoteDataSourceImpl(ayanApi)
    dataSource(
        input = GetProfile.Input(userId = "123"),
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

## APIs without input or output

Both nested models are optional.

An API with no input generates a data source method with no `input` parameter:

```kotlin
@AyanAPI
class GetStatus {
    data class Output(val status: String)
}

val dataSource = GetStatusRemoteDataSourceImpl(ayanApi)
dataSource.invoke()
```

An API with no output returns an `AyanAPIResult` whose response type is `Unit`:

```kotlin
@AyanAPI
class SendEvent {
    data class Input(val name: String)
}

val sendEvent = SendEventRemoteDataSourceImpl(ayanApi)
sendEvent(SendEvent.Input(name = "opened"))
```

If neither model is present, Generator uses `Unit` for both the request and response.

## Naming rules

- The annotated declaration must be a class.
- Every generated data source exposes an `operator fun invoke` method.
- Each API generates `<ApiName>RemoteDataSource` and
  `<ApiName>RemoteDataSourceImpl`.
- The request model is the first nested class whose name ends with `Input`.
- The response model is the first nested class whose name ends with `Output`.
- Generated interfaces are written to `ir.ayantech.networking.datasource`.
- Generated implementations are written to `ir.ayantech.networking.datasource.impl`.

## Build and test

Run the processor tests with:

```shell
./gradlew :generator:test
```

Build the complete project with:

```shell
./gradlew build
```
