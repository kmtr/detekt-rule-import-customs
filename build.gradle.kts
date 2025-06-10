plugins {
    kotlin("jvm") version "2.1.21"
    `maven-publish`
}

group = "com.github.kmtr.detektimportcustoms"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform() // useJUnitPlatformをこのように呼び出す
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
    systemProperty("compile-snippet-tests", project.hasProperty("compile-test-snippets"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
