package io.github.kierranm.teamcity.artifacts.artifactory.web;

import com.intellij.openapi.diagnostic.Logger;
import java.util.Map;
import org.jdom.Content;
import org.jdom.Element;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.model.LightweightRepository;
import static org.jfrog.artifactory.client.model.impl.RepositoryTypeImpl.LOCAL;


/**
 * Gets a list of Artifactory repositories.
 */
public class RepositoriesResourceHandler extends ArtifactoryClientResourceHandler {

  private final static Logger LOG = Logger.getInstance(RepositoriesResourceHandler.class.getName());

  @Override
  public Content getContent(final Artifactory afClient, final Map<String, String> parameters) {
    LOG.warn(parameters.toString());
    final Element repositoriesElement = new Element("repositories");
    for (LightweightRepository repo : afClient.repositories().list(LOCAL)) {
      final Element repoElement = new Element("repository");
      final String repoKey = repo.getKey();
      repoElement.setText(repoKey);
      repositoriesElement.addContent(repoElement);
    }
    return repositoriesElement;
  }
}
