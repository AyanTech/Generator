package com.alirezabdn.generator

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AyanAPI(
    val endpoint: String,
    val methodImplName: String,
    val separationCategory: String,
)
