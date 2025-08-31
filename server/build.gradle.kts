plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("com.jeffmolenaar.revix.server.ApplicationKt")
}

dependencies {
    implementation(project(":shared"))
    
    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    
    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgres)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    
    // DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    
    // Crypto & Auth
    implementation(libs.bcrypt)
    implementation(libs.jwt)
    
    // Logging
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    
    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}

// Produce a runnable fat JAR with proper manifest
/*
tasks.shadowJar {
    archiveBaseName.set("server")
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClass.get()
            )
        )
    }
}
*/
