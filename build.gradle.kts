import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("com.palantir.docker") version "0.25.0"

    kotlin("jvm") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72"
}

extra["springCloudVersion"] = "Hoxton.SR9"
extra["aws.sdk.version"] = "2.10.64"

group = "it.valeriovaudi.onlyoneportal"
//version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth")
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    implementation("software.amazon.awssdk:s3:${property("aws.sdk.version")}")
    implementation("software.amazon.awssdk:sqs:${property("aws.sdk.version")}")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.projectreactor:reactor-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

docker {
    name = "mrflick72/repository-service-k8s:latest"
    setDockerfile(file("src/main/docker/Dockerfile"))
    copySpec.from("build/libs/repository-service.jar").into("repository-service.jar")
    pull(true)
    noCache(true)
}