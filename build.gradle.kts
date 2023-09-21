plugins {
    id("java")
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.0-beta.3"
    id("org.springframework.boot") version "3.1.4"
}

apply(plugin = "io.spring.dependency-management")

group = "de.efi23a"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("net.dv8tion:JDA:5.0.0-beta.13")

    checkstyle("com.puppycrawl.tools:checkstyle:10.12.3")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

springBoot {
    mainClass.set("de.efi23a.bot.BotApplication")
}

tasks {
    test {
        useJUnitPlatform()
    }
    bootBuildImage {
        docker {
            host.set(System.getenv("DOCKER_HOST"))
            tlsVerify.set(true)
            certPath.set("/home/mcmdev/.minikube/certs")
            imageName.set("127.0.0.1/efi23a/discord-bot:latest")
            publish.set(false)
        }
    }
}