package io.github.kierranm.teamcity.artifacts.artifactory.web;

import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryConstants;

public class ArtifactoryParametersProvider {
  public String getUrl() {
    return ArtifactoryConstants.ARTIFACTORY_URL;
  }

  public String getRepositoryKey() {
    return ArtifactoryConstants.ARTIFACTORY_REPOSITORY_KEY;
  }

  public String getPathPrefix() {
    return ArtifactoryConstants.ARTIFACTORY_REPOSITORY_PATH_PREFIX_ATTR;
  }

  public String getUsername() {
    return ArtifactoryConstants.ARTIFACTORY_USERNAME;
  }

  public String getPassword() {
    return ArtifactoryConstants.ARTIFACTORY_PASSWORD;
  }

  public String getAccessToken() {
    return ArtifactoryConstants.ARTIFACTORY_ACCESS_TOKEN;
  }

  public String getContainersPath() {
    return String.format("/plugins/artifactory-artifact-storage/%s.html", ArtifactoryConstants.ARTIFACTORY_SETTINGS_PATH);
  }
}
