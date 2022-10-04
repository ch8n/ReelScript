import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "dev.ch8n"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}