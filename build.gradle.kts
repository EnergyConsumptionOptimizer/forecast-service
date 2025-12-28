import io.github.andreabrighi.gradle.gitsemver.conventionalcommit.ConventionalCommit
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath("io.github.andreabrighi:conventional-commit-strategy-for-git-sensitive-semantic-versioning-gradle-plugin:1.0.15")
    }
}

dependencies {
    testImplementation(libs.bundles.kotlin.testing)
    testImplementation(libs.ktor.server.test.host)
    implementation(libs.bundles.ktor)
    implementation(libs.logback.classic)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kmongo.coroutine.serialization)
    implementation(libs.dotenv.kotlin)
    implementation(libs.smile.core)
}

gitSemVer {
    commitNameBasedUpdateStrategy(ConventionalCommit::semanticVersionUpdate)
    minimumVersion.set("0.1.0")
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("$rootDir/doc"))
    }
}

tasks.withType<Detekt>().configureEach {
    config.setFrom(files("$rootDir/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("lint") {
    group = "quality"
    dependsOn("detekt")
}

tasks.register("checkFormat") {
    group = "quality"
    dependsOn("ktlintCheck")
}

tasks.named("build") {
    dependsOn("ktlintFormat", "detekt", "test")
}
