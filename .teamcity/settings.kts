import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.PullRequests
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs

version = "2019.1"

project {
    params {
        param("teamcity.ui.settings.readOnly", "true")
        password("github.publishing_token", "credentialsJSON:e463dfac-587e-44e7-9aff-8f7faf91090f")
    }
    buildType(Build)
    buildType(PublishToRelease)

}

object Build : BuildType({
    name = "Build"
    description = "Build and package the artifact"

    artifactRules = "+:artifactory-artifact-storage-server/build/distributions/artifactory-artifact-storage.zip"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Set Version Number"
            scriptContent = """
                #!/bin/bash -e
                branch="%teamcity.build.branch%"
                regex='v([0-9]+\.[0-9]+\.[0-9]+.*)'
                if [[ ${'$'}branch =~ ${'$'}regex ]]; then
                    version=${'$'}{BASH_REMATCH[1]}
                    echo "##teamcity[setParameter name='env.ORG_GRADLE_PROJECT_versionNumber' value='${"$"}{version}']"
                else
                    echo "Not a release build"
                fi
            """.trimIndent()
        }
        gradle {
            tasks = "clean build"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    triggers {
        vcs {
            branchFilter = """
                +:<default>
                +:pull/*
                +:v*
            """.trimIndent()
        }
    }

    features {
        commitStatusPublisher {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "credentialsJSON:a502bb02-a4fe-4261-ac1d-763e4cc112b3"
                }
            }
        }
        pullRequests {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            provider = github {
                authType = vcsRoot()
                filterAuthorRole = PullRequests.GitHubRoleFilter.MEMBER
            }
        }
    }
})

object PublishToRelease: BuildType({
    name = "Publish"
    description = "Publish Plugin to GitHub Release"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Rename Archive"
            scriptContent = "mv artifactory-artifact-storage.zip artifactory-artifact-storage-%teamcity.build.branch%.zip"
        }
        script {
            name = "Push Artifact to Release"
            scriptContent = """
                upload_url=$(curl -H "Authorization: token %github.publishing_token%" "https://api.github.com/repos/KierranM/teamcity-artifactory-artifact-storage-plugin/releases/tags/%teamcity.build.branch%" | jq -r .upload_url | sed 's/{.*}//' )
                curl -H "Authorization: token %github.publishing_token%" -H "Content-Type: application/zip" -XPOST -i -T artifactory-artifact-storage-%teamcity.build.branch%.zip "${'$'}{upload_url}?name=artifactory-artifact-storage-%teamcity.build.branch%.zip"
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            branchFilter = """
                -:*
                +:v*
            """.trimIndent()
        }
    }

    dependencies {
        dependency(Build) {
            artifacts {
                artifactRules = "+:artifactory-artifact-storage.zip"
                cleanDestination = true
                buildRule = sameChainOrLastFinished()
            }
            snapshot {}
        }
    }
})