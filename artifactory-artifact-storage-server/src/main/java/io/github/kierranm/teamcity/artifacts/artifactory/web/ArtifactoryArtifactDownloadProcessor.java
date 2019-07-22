package io.github.kierranm.teamcity.artifacts.artifactory.web;

import com.intellij.openapi.diagnostic.Logger;
import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryConstants;
import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import jetbrains.buildServer.artifacts.ArtifactData;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.artifacts.StoredBuildArtifactInfo;
import jetbrains.buildServer.web.openapi.artifacts.ArtifactDownloadProcessor;
import org.jetbrains.annotations.NotNull;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.model.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryArtifactDownloadProcessor implements ArtifactDownloadProcessor {

  private final static Logger LOG = Logger.getInstance(ArtifactoryArtifactDownloadProcessor.class.getName());

  @NotNull
  @Override
  public String getType() {
    return ArtifactoryConstants.ARTIFACTORY_STORAGE_TYPE;
  }

  @Override
  public boolean processDownload(@NotNull StoredBuildArtifactInfo storedBuildArtifactInfo,
                                 @NotNull BuildPromotion buildPromotion,
                                 @NotNull HttpServletRequest httpServletRequest,
                                 @NotNull HttpServletResponse httpServletResponse) throws IOException {
    final ArtifactData artifactData = storedBuildArtifactInfo.getArtifactData();
    if (artifactData == null) throw new IOException("Can not process artifact download request for a folder");

    final Map<String, String> params = ArtifactoryUtil.validateParameters(storedBuildArtifactInfo.getStorageSettings());

    final String repositoryKey = ArtifactoryUtil.getRepositoryKey(params);
    if (repositoryKey == null) {
      final String message = "Failed to create URL: repository key is not specified, check Artifactory storage profile settings";
      LOG.warn(message);
      throw new IOException(message);
    }

    RepositoryHandle repository = ArtifactoryUtil.getClient(params).repository(repositoryKey);
    final String artifactPath = artifactData.getPath();
    final String key = ArtifactoryUtil.getPathPrefix(storedBuildArtifactInfo.getCommonProperties()) + artifactPath;

    File file = repository.file(key).info();
    httpServletResponse.sendRedirect(file.getDownloadUri());
    return true;
  }
}
