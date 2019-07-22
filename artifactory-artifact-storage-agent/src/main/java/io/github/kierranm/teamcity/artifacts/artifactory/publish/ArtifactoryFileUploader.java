package io.github.kierranm.teamcity.artifacts.artifactory.publish;

import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.artifactory.client.Artifactory;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.ArtifactPublishingFailedException;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.artifacts.ArtifactDataInstance;
import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import org.apache.http.client.HttpResponseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil.getRepositoryKey;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryFileUploader {

  private static final Logger LOG = Logger.getInstance(ArtifactoryFileUploader.class.getName());

  private BuildAgentConfiguration myBuildAgentConfiguration;

  public ArtifactoryFileUploader(@NotNull final BuildAgentConfiguration buildAgentConfiguration) {
    myBuildAgentConfiguration = buildAgentConfiguration;
  }

  @NotNull
  public Collection<ArtifactDataInstance> publishFiles(@NotNull final AgentRunningBuild build,
                                                       @NotNull final String pathPrefix,
                                                       @NotNull final Map<java.io.File, String> filesToPublish) {
    final String homeDir = myBuildAgentConfiguration.getAgentHomeDirectory().getPath();

    final Map<String, String> params = ArtifactoryUtil.validateParameters(build.getArtifactStorageSettings());
    final String repositoryKey = getRepositoryKey(params);
    final Artifactory afClient = ArtifactoryUtil.getClient(params);
    final RepositoryHandle repository = afClient.repository(getRepositoryKey(params));

    if (repository.exists() == false) {
      throw new ArtifactPublishingFailedException("Target Artifactory artifact repository " + repositoryKey + " doesn't exist", false, null);
    }

    try {
      final List<ArtifactDataInstance> artifacts = new ArrayList<ArtifactDataInstance>();
      CollectionsUtil.convertAndFilterNulls(filesToPublish.entrySet(), new Converter<File, Map.Entry<java.io.File, String>>() {
          @Override
          public File createFrom(@NotNull Map.Entry<java.io.File, String> entry) {
            final java.io.File file = entry.getKey();
            final String path = entry.getValue();
            final String artifactPath = ArtifactoryUtil.normalizeArtifactPath(path, file);
            final String fullPath = pathPrefix + artifactPath;

            artifacts.add(ArtifactDataInstance.create(artifactPath, file.length()));

            File uploadedFile = repository.upload(fullPath, file).doUpload();

            return uploadedFile;
          }
      });
      return artifacts;
    } catch (Throwable t) {
      final String message = t.getMessage();
      if (t instanceof HttpResponseException) {
        final HttpResponseException ex = (HttpResponseException)t;
        final int code = ex.getStatusCode();

        if (StringUtil.isNotEmpty(message)) {
          LOG.warn(code + ": " + message);
          build.getBuildLogger().error(message);
        }
      }

      throw new ArtifactPublishingFailedException(message, false, t);
    }
  }
}
