import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Instant

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.app.paperstow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.paperstow"
        minSdk = 26
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("DOCVAULT_KEYSTORE")
                ?: (System.getenv("HOME") + "/release.keystore")
            val ks = file(keystorePath)
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("DOCVAULT_STORE_PASSWORD") ?: ""
                keyAlias = System.getenv("DOCVAULT_KEY_ALIAS") ?: "upload_key"
                keyPassword = System.getenv("DOCVAULT_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters.clear()
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE"
        }
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        unitTests.isReturnDefaultValues = true
    }
}

configurations.configureEach {
    resolutionStrategy {
        failOnChangingVersions()
    }
}

dependencies {
    implementation(libs.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.core.ktx)
    implementation(libs.biometric)
    implementation(libs.documentfile)
    implementation(libs.zip4j)
    implementation(libs.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.document.scanner)

    implementation(libs.bouncycastle)
    implementation(libs.security.crypto)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // security-crypto 1.1.0 still pulls Gson 2.8.9 via Tink; force a patched line.
    constraints {
        implementation(libs.gson)
    }

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)

    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines.test)
    testImplementation("org.json:json:20260814")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val variantName = variant.name
        val taskName = "generate${variantName.replaceFirstChar { it.uppercase() }}Sbom"
        val generate = tasks.register<GenerateSbomTask>(taskName) {
            group = "sbom"
            description = "Write a CycloneDX SBOM for the $variantName build and package it in the APK."
            this.variantName.set(variantName)
            appName.set("Paperstow")
            appVersion.set(providers.provider { android.defaultConfig.versionName ?: "0" })
            configurationName.set(variant.runtimeConfiguration.name)
            runtimeClasspath.from(variant.runtimeConfiguration)
            outputDir.set(layout.buildDirectory.dir("generated/sbomAssets/$variantName"))
            archiveDir.set(layout.buildDirectory.dir("outputs/sbom/$variantName"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(generate, GenerateSbomTask::outputDir)
    }
}

abstract class GenerateSbomTask : DefaultTask() {
    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val appVersion: Property<String>

    @get:Input
    abstract val configurationName: Property<String>

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val archiveDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val config = project.configurations.getByName(configurationName.get())
        val components = linkedMapOf<String, Triple<String, String, String>>()
        config.incoming.resolutionResult.allComponents.forEach { component ->
            val id = component.id
            if (id is org.gradle.api.artifacts.component.ModuleComponentIdentifier && id.version.isNotBlank()) {
                components["${id.group}:${id.module}:${id.version}"] = Triple(id.group, id.module, id.version)
            }
        }
        val sorted = components.values.sortedWith(compareBy({ it.first }, { it.second }, { it.third }))
        val timestamp = Instant.now().toString()
        val json = buildCycloneDx(
            appName = appName.get(),
            appVersion = appVersion.get(),
            variant = variantName.get(),
            timestamp = timestamp,
            components = sorted
        )
        fun writeTo(dir: File) {
            dir.mkdirs()
            File(dir, "sbom.json").writeText(json)
        }
        writeTo(outputDir.get().asFile)
        writeTo(archiveDir.get().asFile)
        logger.lifecycle("SBOM ${variantName.get()}: ${sorted.size} components → ${archiveDir.get().asFile}/sbom.json")
    }

    private fun buildCycloneDx(
        appName: String,
        appVersion: String,
        variant: String,
        timestamp: String,
        components: List<Triple<String, String, String>>
    ): String {
        fun esc(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")
        val rows = components.joinToString(",\n") { (group, name, version) ->
            val purl = "pkg:maven/${esc(group)}/${esc(name)}@$version"
            """    {
      "type": "library",
      "bom-ref": "$purl",
      "group": "${esc(group)}",
      "name": "${esc(name)}",
      "version": "${esc(version)}",
      "purl": "$purl"
    }"""
        }
        return """{
  "bomFormat": "CycloneDX",
  "specVersion": "1.5",
  "version": 1,
  "metadata": {
    "timestamp": "${esc(timestamp)}",
    "component": {
      "type": "application",
      "name": "${esc(appName)}",
      "version": "${esc(appVersion)}",
      "bom-ref": "pkg:maven/com.app.paperstow/paperstow@${esc(appVersion)}"
    },
    "properties": [
      { "name": "build:variant", "value": "${esc(variant)}" }
    ]
  },
  "components": [
$rows
  ]
}
"""
    }
}
