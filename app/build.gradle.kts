plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.cash.sqldelight)
}

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "${projectDir}/schemas")
}

val llamatikVersion = "1.7.0"

val llamatikJvmArtifact: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    llamatikJvmArtifact("com.llamatik:library-jvm:$llamatikVersion")
}

val llamatikWithLinuxNativesOnly = tasks.register<Jar>("llamatikWithLinuxNativesOnly") {
    archiveFileName.set("llamatik-linux-natives-only.jar")
    destinationDirectory.set(layout.buildDirectory.dir("llamatik"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(llamatikJvmArtifact.elements.map { artifacts -> artifacts.map { zipTree(it.asFile) } }) {
        exclude("native/macos/**")
        exclude("native/windows/**")
        exclude("META-INF/MANIFEST.MF")
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop") {
        mainRun {
            mainClass.set("com.ben.emberr.DesktopMainKt")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)

                implementation(libs.koin.core)
                implementation(libs.kotlinx.serialization.json)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
                implementation(libs.androidx.room.runtime)
                implementation(libs.haze)
                implementation(libs.koin.compose.multiplatform)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.datetime)
                implementation(libs.coil.compose)
                implementation(libs.navigation.compose.kmp)
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.okio)

                implementation("io.ktor:ktor-client-core:3.3.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
                implementation("io.ktor:ktor-client-auth:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")

                // On-device LLM inference via llama.cpp
                implementation("com.llamatik:library:$llamatikVersion")
                // SQLDelight coroutines extensions
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")

                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.process)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.androidx.room.ktx)
                implementation(libs.sqlcipher)
                implementation(libs.androidx.sqlite.ktx)
                implementation(libs.androidx.security.crypto)
                implementation(libs.androidx.glance.appwidget)
                implementation("io.coil-kt.coil3:coil-compose:3.3.0")
                implementation("com.composables:icons-lucide:1.1.0")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
                implementation("org.jsoup:jsoup:1.17.2")
                implementation("io.ktor:ktor-client-okhttp:3.3.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
                implementation("io.ktor:ktor-client-auth:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
                implementation("androidx.documentfile:documentfile:1.1.0")
                implementation(libs.koin.androidx.workmanager)

                // CameraX
                implementation("androidx.camera:camera-camera2:1.6.0")
                implementation("androidx.camera:camera-lifecycle:1.6.0")
                implementation("androidx.camera:camera-view:1.6.0")

                // ML Kit Barcode Scanning
                implementation("com.google.mlkit:barcode-scanning:17.3.0")
                implementation("com.google.guava:guava:33.4.8-android")

                implementation("androidx.core:core-splashscreen:1.2.0")

                // SQLDelight Android driver
                implementation("app.cash.sqldelight:android-driver:2.0.2")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jsoup:jsoup:1.17.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
                implementation("io.ktor:ktor-client-java:3.3.0")
                implementation("io.ktor:ktor-server-netty:3.3.0")
                implementation("io.ktor:ktor-server-content-negotiation:3.3.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
                implementation("io.ktor:ktor-server-auth:3.3.0")
                implementation("org.jmdns:jmdns:3.5.9")
                implementation("com.google.zxing:core:3.5.3")
                implementation("com.github.javakeyring:java-keyring:1.0.4")
                implementation("org.apache.pdfbox:pdfbox:3.0.7")

                // SQLDelight JVM driver
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

                runtimeOnly(files(llamatikWithLinuxNativesOnly))
            }
        }
    }
}

configurations.named("desktopRuntimeClasspath") {
    exclude(mapOf("group" to "com.llamatik", "module" to "library-jvm"))
}

compose.desktop {
    application {
        mainClass = "com.ben.emberr.DesktopMainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "Emberr"
            packageVersion = "1.0.0"

            modules(
                "java.sql",
                "java.prefs",
                "java.net.http",
                "java.naming",
                "java.management",
                "java.instrument",
                "jdk.unsupported",
                "jdk.crypto.cryptoki",
                "jdk.security.auth",
                "jdk.net"
            )

            linux {
                packageName = "emberr"
                rpmLicenseType = "AGPL-3.0-or-later"
                appCategory = "Office"
                menuGroup = "Office"
                appRelease = "1"
                shortcut = true
                iconFile.set(project.file("packaging/linux/emberr.png"))
            }
        }
    }
}

sqldelight {
    databases {
        create("EmberrDatabase") {
            packageName.set("com.emberr.database")
        }
    }
}

android {
    namespace = "com.ben.emberr"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ben.emberr"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        noCompress += listOf("so", "mdl", "fst", "conf", "int", "dubm", "ie", "mat", "stats", "gguf")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    sourceSets["main"].assets.srcDirs("src/androidMain/assets")
    sourceSets["main"].java.srcDirs("src/androidMain/kotlin")
}

dependencies {
    add("kspCommonMainMetadata", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.jetbrains.skiko") {
                useVersion("0.9.37.4")
            }
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-stdlib")) {
                useVersion("2.1.21")
            }
        }
    }
}