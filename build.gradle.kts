import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.spring") version "1.4.21"
}

extra["springCloudVersion"] = "2020.0.0"
extra["aws.sdk.version"] = "2.10.64"

group = "it.valeriovaudi.onlyoneportal"
//version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {

    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.experimental:spring-graalvm-native:0.8.3")
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

tasks.getByName<BootBuildImage>("bootBuildImage") {
    builder = "paketobuildpacks/builder:tiny"
    imageName = "mrflick72/repository-service-k8s:latest"
    environment = mapOf(
            "BP_BOOT_NATIVE_IMAGE" to "1",
            "BP_BOOT_NATIVE_IMAGE_BUILD_ARGUMENTS" to """
                -Dspring.spel.ignore=true                
            """.trimIndent()
    )
}