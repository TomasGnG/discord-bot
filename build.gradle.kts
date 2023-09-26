plugins {
    id("java")
    id("checkstyle")
    id("org.springframework.boot") version "3.1.4"
}

apply(plugin = "io.spring.dependency-management")

group = "de.efi23a"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Lombgok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Spring
    implementation("org.springframework.boot:spring-boot-starter")

    // JDA
    implementation("net.dv8tion:JDA:5.0.0-beta.13")

    // Checkstyle
    checkstyle("com.puppycrawl.tools:checkstyle:10.12.3")

    // JUnit 5
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // RESTful, Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

springBoot {
    mainClass.set("de.efi23a.bot.BotApplication")
}

checkstyle {
    isIgnoreFailures = false
    maxWarnings = 0
    maxErrors = 0
}

tasks {
    bootJar {
        archiveVersion = ""
    }
    build {
        dependsOn("check")
    }
    test {
        useJUnitPlatform()
    }
}