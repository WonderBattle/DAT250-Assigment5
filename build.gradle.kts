// --- Plugins section ---------------------------------------------------------
plugins {
    java                                                       // Java plugin
    id("org.springframework.boot") version "3.5.5"             // Spring Boot plugin
    id("io.spring.dependency-management") version "1.1.7"      // Dependency mgmt
    id("com.github.node-gradle.node") version "7.0.2"          // Node plugin for frontend
}

// --- Project metadata --------------------------------------------------------
group = "com.Assigment5"                                       // Maven group id
version = "0.0.1-SNAPSHOT"                                     // App version
description = "DAT250: Software Technology Experiment Assignment 5"

// --- Java toolchain ----------------------------------------------------------
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))        // Use Java 21
    }
}

// --- Source sets (custom folder layout) --------------------------------------
sourceSets {
    named("main") {
        java.srcDirs("backend/src/main/java")                  // Java sources
        resources.srcDirs("backend/src/main/resources")        // Resources
    }
    named("test") {
        java.srcDirs("backend/src/test/java")                  // Test sources
        resources.srcDirs("backend/src/test/resources")        // Test resources
    }
}

// --- Repositories ------------------------------------------------------------
repositories {
    mavenCentral()
}

// --- Dependencies ------------------------------------------------------------
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // API docs (keep only consistent version!)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")

    // Hibernate and JPA (Assigment 4)
    implementation("org.hibernate.orm:hibernate-core:7.1.1.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("com.h2database:h2:2.3.232")

    // Redis dependencies (Assigment 5)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("redis.clients:jedis:6.2.0")
}

// --- Testing configuration ---------------------------------------------------
tasks.withType<Test> {
    useJUnitPlatform()
}

// --- Node.js frontend build integration --------------------------------------
node {
    version.set("22.12.0")                                      // Node.js version
    npmVersion.set("10.5.1")                                   // npm version
    download.set(true)                                         // Download locally
    nodeProjectDir.set(file("${projectDir}/frontend"))         // Point to frontend folder
}

// Task: Run `npm run build` inside frontend/
val frontendBuild by tasks.registering(com.github.gradle.node.npm.task.NpmTask::class) {
    args.set(listOf("run", "build"))
    workingDir.set(file("${projectDir}/frontend"))             // Run inside frontend folder
    dependsOn("npmInstall") // ensure dependencies are installed first
}

// Task: Copy dist/ output → backend/src/main/resources/static
val copyFrontend by tasks.registering(Copy::class) {
    from("${projectDir}/frontend/dist")
    into("${projectDir}/backend/src/main/resources/static")
    dependsOn(frontendBuild)                                   // Ensure build runs first
}

// Ensure Spring Boot always uses the latest frontend build
tasks.named("processResources") {
    dependsOn(copyFrontend)
}
