package com.alirezabdn.generator

/**
 * Marks an API descriptor for remote data-source, repository, and test-mock generation.
 *
 * Nested classes ending in `RequestBody` and `ResponseModel` define the generated method's input
 * and output types. APIs sharing a [separationCategory] are grouped into the same generated types.
 *
 * @property endpoint endpoint passed to `AyanApi.post`; an empty value uses the annotated class name.
 * @property methodImplName name of the generated data-source and repository method.
 * @property separationCategory category and type-name prefix for the generated networking layer.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AyanAPI(
    val endpoint: String,
    val methodImplName: String,
    val separationCategory: String,
)
