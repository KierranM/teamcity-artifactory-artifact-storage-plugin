package io.github.kierranm.teamcity.artifacts.artifactory;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.artifacts.ArtifactData;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.artifacts.ArtifactContentProvider;
import jetbrains.buildServer.serverSide.artifacts.StoredBuildArtifactInfo;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.RepositoryHandle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryArtifactContentProvider implements ArtifactContentProvider {

  private final static Logger LOG = Logger.getInstance(ArtifactoryArtifactContentProvider.class.getName());
  private final ServerPaths myServerPaths;

  public ArtifactoryArtifactContentProvider(@NotNull ServerPaths serverPaths) {
    myServerPaths = serverPaths;
  }

  @NotNull
  @Override
  public String getType() {
    return ArtifactoryConstants.ARTIFACTORY_STORAGE_TYPE;
  }

  @NotNull
  @Override
  public InputStream getContent(@NotNull StoredBuildArtifactInfo storedBuildArtifactInfo) throws IOException {
    final Map<String, String> params;
    final ArtifactData artifactData = storedBuildArtifactInfo.getArtifactData();
    if (artifactData == null) {
      throw new IOException("Invalid artifact data: Artifactory path property is not set");
    }

    final String artifactPath = artifactData.getPath();
    try {
      params = ArtifactoryUtil.validateParameters(storedBuildArtifactInfo.getStorageSettings());
    } catch (IllegalArgumentException e) {
      throw new IOException("Failed to get artifact " + artifactPath + " content: Invalid storage settings " + e.getMessage(), e);
    }

    final String repositoryKey = ArtifactoryUtil.getRepositoryKey(params);
    final String key = ArtifactoryUtil.getPathPrefix(storedBuildArtifactInfo.getCommonProperties()) + artifactPath;

    try {
      Artifactory afClient = ArtifactoryUtil.getClient(params);
      RepositoryHandle repository = afClient.repository(repositoryKey);

      return repository.download(key).doDownload();
    } catch (Throwable t) {
      final String details = t.getMessage();
      if (StringUtil.isNotEmpty(details)) {
        LOG.warn(details);
      }
      throw new IOException(String.format(
        "Failed to get artifact '%s' content in repository '%s': %s",
        artifactPath, repositoryKey, details
      ), t);
    }
  }
}
