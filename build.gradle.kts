group = "com.gradle.enterprise.api"
description = "Gradle Enterprise API sample"

plugins {
    id("org.openapi.generator") version "6.1.0"
    kotlin("jvm") version "1.7.10"
    `java-library`
    application
}

repositories {
    mavenCentral()
}

val apiSpecificationFile = resources.text.fromUri("https://docs.gradle.com/enterprise/api-manual/ref/gradle-enterprise-2022.3-api.yaml").asFile()

application {
    mainClass.set("com.gradle.enterprise.api.KotlinMainKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("info.picocli:picocli:4.6.3")
    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-adapters:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
}


val basePackageName = "com.gradle.enterprise.api"
val modelPackageName = "$basePackageName.model"
val invokerPackageName = "$basePackageName.client"
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(apiSpecificationFile.absolutePath)
    outputDir.set(project.layout.buildDirectory.file("generated/$name").map { it.asFile.absolutePath })
    ignoreFileOverride.set(project.layout.projectDirectory.file(".openapi-generator-ignore").asFile.absolutePath)
    modelPackage.set(modelPackageName)
    apiPackage.set(basePackageName)
    invokerPackage.set(invokerPackageName)
    configOptions.set(mapOf(
        "library" to "jvm-retrofit2",
        "useCoroutines" to "true",
        "serializableModel" to "true",
        "dateLibrary" to "java8",
        "hideGenerationTimestamp" to "true",
        "openApiNullable" to "false",
        "useBeanValidation" to "false",
        "disallowAdditionalPropertiesIfNotPresent" to "false",
        "sourceFolder" to ""  // makes IDEs like IntelliJ more reliably interpret the class packages.
    ))
}

sourceSets.main {
    java { srcDir(tasks.openApiGenerate) }
}
