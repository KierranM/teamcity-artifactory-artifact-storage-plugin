package io.github.kierranm.teamcity.artifacts.artifactory.publish;

import com.intellij.openapi.diagnostic.Logger;
import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.ArtifactPublishingFailedException;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.artifacts.ArtifactDataInstance;
import jetbrains.buildServer.util.StringUtil;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.RepositoryHandle;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    final Map<String, String> params = ArtifactoryUtil.validateParameters(build.getArtifactStorageSettings());
    final String repositoryKey = getRepositoryKey(params);
    final Artifactory afClient = ArtifactoryUtil.getClient(params);
    final RepositoryHandle repository = afClient.repository(getRepositoryKey(params));

    if (repository.exists() == false) {
      throw new ArtifactPublishingFailedException("Target Artifactory artifact repository " + repositoryKey + " doesn't exist", false, null);
    }

    int parallelism = ArtifactoryUtil.getParallelism(params);
    ExecutorService parallelExecutor = Executors.newFixedThreadPool(parallelism);
    AtomicReference<Throwable> fatalError = new AtomicReference<>(null);

    List<ArtifactDataInstance> artifacts;
    try {
      List<Future<ArtifactDataInstance>> futures = filesToPublish.entrySet().stream().sequential()
              .sorted((a, b) -> Long.compare(b.getKey().length(), a.getKey().length()))  // upload larger files first
              .map(e -> parallelExecutor.submit(() -> uploadArtifactTask(build, pathPrefix, repository, fatalError, e)))
              .collect(Collectors.toList());
      artifacts = futures.stream()
              .map(this::quietlyGetResult)
              .collect(Collectors.toList());
    } finally {
      try {
        parallelExecutor.shutdown();
      } catch (Exception e) {
        LOG.error(e);
      }
    }

    if (fatalError.get() != null) {
      throw new ArtifactPublishingFailedException(fatalError.get().getMessage(), false, fatalError.get());
    }

    return artifacts;
  }

  @Nullable
  private ArtifactDataInstance uploadArtifactTask(@NotNull AgentRunningBuild build, @NotNull String pathPrefix, RepositoryHandle repository, AtomicReference<Throwable> fatalError, Map.Entry<File, String> entry) {
    if (fatalError.get() != null) {
      return null;
    }

    final File file = entry.getKey();
    final String path = entry.getValue();
    final String artifactPath = ArtifactoryUtil.normalizeArtifactPath(path, file);
    final String fullPath = pathPrefix + artifactPath;

    try {
      repository.upload(fullPath, file).doUpload();
    } catch (Throwable t) {
      fatalError.set(t);
      if (t instanceof HttpResponseException) {
        final HttpResponseException ex = (HttpResponseException) t;
        final String message = ex.getMessage();
        final int code = ex.getStatusCode();
        if (StringUtil.isNotEmpty(message)) {
          LOG.warn(code + ": " + message);
          build.getBuildLogger().error(message);
        }
      }
      return null;
    }

    return ArtifactDataInstance.create(artifactPath, file.length());
  }

  private <T> T quietlyGetResult(java.util.concurrent.Future<T> f) {
    try {
      return f.get();
    } catch (InterruptedException e) {
      LOG.error(e);
      throw new ArtifactPublishingFailedException(e.getMessage(), false, e);
    } catch (ExecutionException e) {
      LOG.error(e);
      throw new ArtifactPublishingFailedException(e.getCause().getMessage(), false, e.getCause());
    }
  }

}
