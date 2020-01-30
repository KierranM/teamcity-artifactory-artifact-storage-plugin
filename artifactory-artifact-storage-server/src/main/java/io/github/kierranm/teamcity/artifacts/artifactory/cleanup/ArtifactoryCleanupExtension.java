package io.github.kierranm.teamcity.artifacts.artifactory.cleanup;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryConstants;
import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import jetbrains.buildServer.artifacts.ArtifactData;
import jetbrains.buildServer.artifacts.ArtifactListData;
import jetbrains.buildServer.artifacts.ServerArtifactStorageSettingsProvider;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.serverSide.artifacts.ServerArtifactHelper;
import jetbrains.buildServer.serverSide.cleanup.*;
import jetbrains.buildServer.serverSide.impl.cleanup.ArtifactPathsEvaluator;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.positioning.PositionAware;
import jetbrains.buildServer.util.positioning.PositionConstraint;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.model.File;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryCleanupExtension implements CleanupExtension, PositionAware {

  @NotNull
  private final ServerArtifactStorageSettingsProvider mySettingsProvider;
  @NotNull
  private final ServerArtifactHelper myHelper;
  @NotNull
  private final ServerPaths myServerPaths;

  public ArtifactoryCleanupExtension(
    @NotNull ServerArtifactHelper helper,
    @NotNull ServerArtifactStorageSettingsProvider settingsProvider,
    @NotNull ServerPaths serverPaths) {
    myHelper = helper;
    mySettingsProvider = settingsProvider;
    myServerPaths = serverPaths;
  }

  @Override
  public void cleanupBuildsData(@NotNull BuildCleanupContext cleanupContext) {
    for (SFinishedBuild build : cleanupContext.getBuilds()) {
      try {
        final ArtifactListData artifactsInfo = myHelper.getArtifactList(build);
        if (artifactsInfo == null) {
          continue;
        }
        final String pathPrefix = ArtifactoryUtil.getPathPrefix(artifactsInfo);
        if (pathPrefix == null) {
          continue;
        }
        List<String> pathsToDelete = ArtifactPathsEvaluator.getPathsToDelete((BuildCleanupContextEx) cleanupContext, build, artifactsInfo);
        if (pathsToDelete.isEmpty()) {
          continue;
        }
        doClean(cleanupContext.getErrorReporter(), build, pathPrefix, pathsToDelete);
      } catch (Throwable e) {
        Loggers.CLEANUP.debug(e);
        cleanupContext.getErrorReporter().buildCleanupError(build.getBuildId(), "Failed to remove S3 artifacts: " + e.getMessage());
      }
    }
  }

  private void doClean(@NotNull ErrorReporter errorReporter, @NotNull SFinishedBuild build, @NotNull String pathPrefix, @NotNull List<String> pathsToDelete) throws IOException {
    final Map<String, String> params = ArtifactoryUtil.validateParameters(mySettingsProvider.getStorageSettings(build));
    final String repositoryKey = ArtifactoryUtil.getRepositoryKey(params);
    final Artifactory afClient = ArtifactoryUtil.getClient(params);
    final RepositoryHandle repository = afClient.repository(repositoryKey);


    final String suffix = " from Artifactory repository [" + repositoryKey + "]" + " from path [" + pathPrefix + "]";

    final AtomicInteger succeededNum = new AtomicInteger();
    final AtomicInteger errorNum = new AtomicInteger();

    final int batchSize = TeamCityProperties.getInteger(ArtifactoryConstants.ARTIFACTORY_CLEANUP_BATCH_SIZE, 1000);
    final boolean useParallelStream = TeamCityProperties.getBooleanOrTrue(ArtifactoryConstants.ARTIFACTORY_CLEANUP_USE_PARALLEL);
    final List<List<String>> partitions = Lists.partition(pathsToDelete, batchSize);
    final Stream<List<String>> listStream = partitions.size() > 1 && useParallelStream
      ? partitions.parallelStream()
      : partitions.stream();

    listStream.forEach(part -> {
      final List<String> files = part.stream().map(path -> pathPrefix + path).collect(Collectors.toList());
      for (String file : files) {
        try {
          repository.delete(file);
          succeededNum.addAndGet(1);
        } catch (Throwable t) {
          if (t instanceof HttpResponseException) {
            final HttpResponseException ex = (HttpResponseException) t;
            if (ex.getStatusCode() == 404) {
              succeededNum.addAndGet(1);
            } else {
              errorNum.addAndGet(1);
            }
          }
        }
      }
    });

    if (errorNum.get() > 0) {
      String errorMessage = "Failed to remove [" + errorNum + "] Artifactory " + StringUtil.pluralize("artifact", errorNum.get()) + suffix;
      errorReporter.buildCleanupError(build.getBuildId(), errorMessage);
    }

    Loggers.CLEANUP.info("Removed [" + succeededNum + "] S3 " + StringUtil.pluralize("object", succeededNum.get()) + suffix);

    myHelper.removeFromArtifactList(build, pathsToDelete);
  }

  @Override
  public void afterCleanup(@NotNull CleanupProcessState cleanupProcessState) {
    // do nothing
  }

  @NotNull
  @Override
  public PositionConstraint getConstraint() {
    return PositionConstraint.first();
  }

  @NotNull
  @Override
  public String getOrderId() {
    return ArtifactoryConstants.ARTIFACTORY_STORAGE_TYPE;
  }
}
