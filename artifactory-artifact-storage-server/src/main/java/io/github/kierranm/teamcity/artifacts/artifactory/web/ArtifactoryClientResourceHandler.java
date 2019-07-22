package io.github.kierranm.teamcity.artifacts.artifactory.web;

import java.util.Map;

import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import org.jdom.Content;
import org.jfrog.artifactory.client.Artifactory;

public abstract class ArtifactoryClientResourceHandler implements ResourceHandler {
  @Override
  public Content getContent(final Map<String, String> parameters) throws Exception {
    return getContent(ArtifactoryUtil.getClient(parameters), parameters);
  }

  protected abstract Content getContent(final Artifactory afClient, final Map<String, String> parameters) throws Exception;

}
