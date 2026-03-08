import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose Multiplatform Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Networking (same Retrofit/OkHttp as Android - JVM compatible)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines (Swing dispatcher for desktop UI thread)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Image loading (Coil 3 - multiplatform)
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    // Media playback (VLCJ - embedded VLC player)
    implementation("uk.co.caprica:vlcj:4.8.3")
}

compose.desktop {
    application {
        mainClass = "com.void.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "Void for Jellyfin"
            packageVersion = "0.2.6"
            description = "A beautiful Jellyfin client for Windows"
            vendor = "Void"
            includeAllModules = true

            appResourcesRootDir = project.layout.projectDirectory.dir("src/main/resources")

            windows {
                menuGroup = "Void for Jellyfin"
                upgradeUuid = "e4e1c0ea-7a25-4e1a-8e5d-4f6c8f7d8a9b"
                iconFile.set(project.file("src/main/resources/icon.ico"))
                shortcut = true
                dirChooser = true
                // Create both installer and portable versions
                perUserInstall = true
            }

            modules("java.sql", "jdk.unsupported")
        }
    }
}
