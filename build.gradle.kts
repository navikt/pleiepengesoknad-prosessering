import org.gradle.internal.impldep.org.fusesource.jansi.AnsiRenderer.test
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val logbackVersion = "1.2.1"
val ktorVersion = "1.0.1"
val jacksonVersion = "2.9.2"
val wiremockVersion = "2.19.0"
val logstashLogbackVersion = "5.2"
val kafkaVersion = "2.0.1"
val prometheusVersion = "0.6.0"

val mainClass = "no.nav.helse.AppKt"

plugins {
    kotlin("jvm") version "1.3.11"
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.0")
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))

    // Ktor Server
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    //compile ("io.ktor:ktor-auth-jwt:$ktorVersion")
    //compile ("io.ktor:ktor-server-core:$ktorVersion")
    compile ("io.ktor:ktor-jackson:$ktorVersion")
    //compile ("io.ktor:ktor-locations:$ktorVersion")
    //compile ("io.ktor:ktor-metrics:$ktorVersion")
    //compile ("io.ktor:ktor-server-host-common:$ktorVersion")

    // JSON Serialization
    compile ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // Logging
    compile ( "ch.qos.logback:logback-classic:$logbackVersion")
    compile ("net.logstash.logback:logstash-logback-encoder:$logstashLogbackVersion")

    // Prometheus
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    // Ktor Client
    compile ("io.ktor:ktor-client-core:$ktorVersion")
    compile ("io.ktor:ktor-client-core-jvm:$ktorVersion")
    compile ("io.ktor:ktor-client-json-jvm:$ktorVersion")
    compile ("io.ktor:ktor-client-jackson:$ktorVersion")
    compile ("io.ktor:ktor-client-apache:$ktorVersion")

}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")

    jcenter()
    mavenLocal()
    mavenCentral()
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    configurations["compile"].forEach {
        val file = File("$buildDir/libs/${it.name}")
        if (!file.exists())
            it.copyTo(file)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.0"
}
