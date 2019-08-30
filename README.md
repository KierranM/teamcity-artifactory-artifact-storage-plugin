# TeamCity Artifactory Artifact Storage Plugin

![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/KierranM/teamcity-artifactory-artifact-storage-plugin?sort=semver&style=plastic)

This plugin allows replacing the TeamCity built-in artifacts storage with
[JFrog Artifactory](https://jfrog.com/artifactory/). The artifacts storage can
be changed at the project level. After changing the storage,
new artifacts produced by the builds of this project will be published to the
(specified) Artifactory repository. Besides publishing, the plugin also implements
resolving of artifact dependencies and clean-up of build artifacts.

# State

This plugin is based heavily on the [teamcity-s3-artifact-storage-plugin](github.com/JetBrains/teamcity-s3-artifact-storage-plugin).
The baseline functionality is complete, but it needs significant review.

# Compatibility

The plugin is compatible with [TeamCity](https://www.jetbrains.com/teamcity/download/) 2017.1 and greater

# Features

When installed and configured, the plugin:
* allows uploading artifacts to JFrog Artifactory
* allows downloading and removing artifacts from JFrog Artifactory
* handles resolution of artifact dependencies
* handles clean-up of artifacts 
* displays artifacts located in JFrog Artifactory in the TeamCity web UI.

# Download

You can download the latest release from [GitHub](https://github.com/KierranM/teamcity-artifactory-artifact-storage-plugin/releases)
# Installing

See [instructions](https://www.jetbrains.com/help/teamcity/?Installing+Additional+Plugins) in TeamCity documentation.

# Configuring 

The plugin adds an additional option to theArtifacts Storage tab in
the Project Settings page in the TeamCity Web UI.  T

To configure JFrog Artifactory storage for TeamCity artifacts,
perform the following:
1. Select Artifactory Storage as the storage type.
2. Provide an optional name for your storage.
3. Enter your Artifactory URL.
4. Provide your Artifactory Credentials.
5. Specify an existing Artifactory Repository to store artifacts.
6. Save your settings.
7. The configured Artifactory storage will appear on the Artifacts storage page.
   Make it active using the corresponding link.

Now the artifacts of this project, its subprojects, and build
configurations will be stored in the configured storage.

## Permissions

The credentials for Artifactory need to have permissions to read, write, and
delete artifacts from the Artifactory repository

# Known issues

# Building 

To build the plugin locally run the following command in the plugin root directory:
```
> gradle build
```

The plugin artifact will be produced in the following location
`artifactory-artifact-storage-server/build/distributions/artifactory-artifact-storage.zip`
and could be installed as [an external TeamCity plugin](https://www.jetbrains.com/help/teamcity/?Installing+Additional+Plugins).
