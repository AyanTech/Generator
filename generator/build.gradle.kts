plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.api)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.kotlin.compile.testing.ksp)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.github.AyanTech"
            artifactId = "generator"
            version = "2.0.0"

            from(components["java"])
            pom {
                name.set("Ayan Gnerator")
                description.set("Generator is a Kotlin Symbol Processing (KSP) library based on Ayan-Networking")
                url.set("https://github.com/AyanTech/Generator")
            }

        }
    }
}
