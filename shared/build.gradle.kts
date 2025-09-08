plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        
        jvmMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.okhttp)
        }
    }
}

// Generate sources JAR
kotlin.jvm().withJava()

// Optional: Create a fat JAR with all dependencies included
// Uncomment and run './gradlew :shared:fatJar' to generate a standalone JAR
/*
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Create a JAR with all dependencies included"
    archiveClassifier.set("fat")
    
    from(kotlin.jvm().compilations.getByName("main").output)
    
    dependsOn(configurations.named("jvmRuntimeClasspath"))
    from({
        configurations.named("jvmRuntimeClasspath").get().filter { 
            it.name.endsWith("jar") 
        }.map { zipTree(it) }
    })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Exclude signature files that can cause issues
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}
*/
