import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    `maven-publish`
}

group = "com.github.kmtr.detektimportcustoms"
version = providers.gradleProperty("version").orElse("1.0.0").get()

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

dependencies {
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")

    testImplementation("io.gitlab.arturbosch.detekt:detekt-core:1.23.8")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:1.23.8")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
            pom {
                name = "Detekt Import Customs"
                description = "Directional dependency restrictions for Kotlin imports and fully qualified references."
                url = "https://github.com/kmtr/detekt-rule-import-customs"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "kmtr"
                        name = "kmtr"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/kmtr/detekt-rule-import-customs.git"
                    developerConnection = "scm:git:ssh://git@github.com/kmtr/detekt-rule-import-customs.git"
                    url = "https://github.com/kmtr/detekt-rule-import-customs"
                }
            }
        }
    }
    repositories {
        maven {
            name = "project"
            url = uri(layout.buildDirectory.dir("repository").get().asFile)
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kmtr/detekt-rule-import-customs")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
