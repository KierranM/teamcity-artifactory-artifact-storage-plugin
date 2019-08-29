package io.github.kierranm.teamcity.artifacts.artifactory.publish;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ArtifactsConstants;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.artifacts.AgentArtifactHelper;
import jetbrains.buildServer.artifacts.ArtifactDataInstance;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryConstants.*;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryArtifactsPublisher implements ArtifactsPublisher {

  private static final Logger LOG = Logger.getInstance(ArtifactoryArtifactsPublisher.class.getName());
  private static final String ERROR_PUBLISHING_ARTIFACTS_LIST = "Error publishing artifacts list";

  private final CurrentBuildTracker myTracker;
  private final AgentArtifactHelper myHelper;
  private final BuildAgentConfiguration myBuildAgentConfiguration;

  private final List<ArtifactDataInstance> myArtifacts = new ArrayList<ArtifactDataInstance>();
  private ArtifactoryFileUploader myFileUploader;

  public ArtifactoryArtifactsPublisher(@NotNull final AgentArtifactHelper helper,
                                       @NotNull final EventDispatcher<AgentLifeCycleListener> dispatcher,
                                       @NotNull final CurrentBuildTracker tracker,
                                       @NotNull final BuildAgentConfiguration buildAgentConfiguration) {
    myHelper = helper;
    myTracker = tracker;
    myBuildAgentConfiguration = buildAgentConfiguration;
    dispatcher.addListener(new AgentLifeCycleAdapter() {
      @Override
      public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
        myFileUploader = null;
        myArtifacts.clear();
      }
    });
  }

  @Override
  public int publishFiles(@NotNull final Map<File, String> map) throws ArtifactPublishingFailedException {
    Map<File, String> filteredMap = CollectionsUtil.filterMapByValues(map, new Filter<String>() {
      @Override
      public boolean accept(@NotNull String s) {
        return !s.startsWith(ArtifactsConstants.TEAMCITY_ARTIFACTS_DIR);
      }
    });

    if (!filteredMap.isEmpty()) {
      final AgentRunningBuild build = myTracker.getCurrentBuild();
      final String pathPrefix = getPathPrefix(build);
      final ArtifactoryFileUploader fileUploader = getFileUploader(build);
      myArtifacts.addAll(fileUploader.publishFiles(build, pathPrefix, filteredMap));
      publishArtifactsList(build);
    }

    return filteredMap.size();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @NotNull
  @Override
  public String getType() {
    return ARTIFACTORY_STORAGE_TYPE;
  }

  private void publishArtifactsList(AgentRunningBuild build) {
    if (!myArtifacts.isEmpty()) {
      final String pathPrefix = getPathPrefix(build);
      try {
        myHelper.publishArtifactList(myArtifacts, CollectionsUtil.asMap(ARTIFACTORY_REPOSITORY_PATH_PREFIX_ATTR, pathPrefix));
      } catch (IOException e) {
        build.getBuildLogger().error(ERROR_PUBLISHING_ARTIFACTS_LIST + ": " + e.getMessage());
        LOG.warnAndDebugDetails(ERROR_PUBLISHING_ARTIFACTS_LIST + "for build " + LogUtil.describe(build), e);
      }
    }
  }

  @NotNull
  private String getPathPrefix(@NotNull AgentRunningBuild build) {
    final List<String> pathSegments = new ArrayList<String>();
    pathSegments.add(build.getArtifactStorageSettings().get(ARTIFACTORY_REPOSITORY_PATH_PREFIX_ATTR));
    pathSegments.add(build.getSharedConfigParameters().get(ServerProvidedProperties.TEAMCITY_PROJECT_ID_PARAM));
    pathSegments.add(build.getBuildTypeExternalId());
    pathSegments.add(Long.toString(build.getBuildId()));
    return StringUtil.join("/", pathSegments) + "/";
  }

  @NotNull
  private ArtifactoryFileUploader getFileUploader(@NotNull final AgentRunningBuild build) {
    if (myFileUploader == null) {
      myFileUploader = new ArtifactoryFileUploader(myBuildAgentConfiguration);
    }
    return myFileUploader;
  }
}
