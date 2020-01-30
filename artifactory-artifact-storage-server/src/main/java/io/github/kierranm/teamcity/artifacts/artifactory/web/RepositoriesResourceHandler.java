package io.github.kierranm.teamcity.artifacts.artifactory.web;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Map;

import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import org.jdom.Content;
import org.jdom.Element;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.model.LightweightRepository;
import org.jfrog.artifactory.client.model.RepositoryType;
import org.springframework.security.access.method.P;

import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.LOCAL;
import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.VIRTUAL;


/**
 * Gets a list of Artifactory repositories.
 */
public class RepositoriesResourceHandler extends ArtifactoryClientResourceHandler {

  private final static Logger LOG = Logger.getInstance(RepositoriesResourceHandler.class.getName());
  private final static String LOCAL_REPO = "local";
  private final static String VIRTUAL_REPO = "virtual";

  @Override
  public Content getContent(final Artifactory afClient, final Map<String, String> parameters) {
    LOG.warn(parameters.toString());
    final Element repositoriesElement = new Element("repositories");

    RepositoryType repoType;
    String selectedRepoType = ArtifactoryUtil.getRepositoryType(parameters);

    switch (selectedRepoType) {
      case LOCAL_REPO:
        repoType = LOCAL;
        break;
      case VIRTUAL_REPO:
        repoType = VIRTUAL;
        break;
      default:
        repoType = LOCAL;
        break;
    }
    for (LightweightRepository repo : afClient.repositories().list(repoType)) {
      final Element repoElement = new Element("repository");
      final String repoKey = repo.getKey();
      repoElement.setText(repoKey);
      repositoriesElement.addContent(repoElement);
    }
    return repositoriesElement;
  }
}
