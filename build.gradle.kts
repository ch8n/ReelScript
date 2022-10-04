import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = "dev.ch8n"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

buildConfig {
    buildConfigField("String", "UNSPLASH_API_KEY", "\"${ReelScriptKeys.UNSPLASH_API_KEY}\"")
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.32")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.0.32")
    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    implementation("org.mp4parser:isoparser:1.9.41")
    implementation("org.mp4parser:muxer:1.9.41")
    implementation("org.mp4parser:streaming:1.9.41")
    implementation("com.google.code.gson:gson:2.9.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}