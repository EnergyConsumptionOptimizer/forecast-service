plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.6"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

gitHooks {
    preCommit {
        tasks("ktlintCheck")
    }
    commitMsg { conventionalCommits() }
    createHooks(true)
}

rootProject.name = "Template-for-Kotlin-projects"
