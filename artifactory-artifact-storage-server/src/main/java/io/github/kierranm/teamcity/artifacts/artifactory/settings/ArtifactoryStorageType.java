package io.github.kierranm.teamcity.artifacts.artifactory.settings;

import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryConstants;
import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.serverSide.ServerSettings;
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageType;
import jetbrains.buildServer.serverSide.artifacts.ArtifactStorageTypeRegistry;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.RepositoryHandle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryStorageType extends ArtifactStorageType {

  @NotNull private final String mySettingsJSP;
  @NotNull private final ServerSettings myServerSettings;
  @NotNull private final ServerPaths myServerPaths;

  public ArtifactoryStorageType(@NotNull ArtifactStorageTypeRegistry registry,
                                @NotNull PluginDescriptor descriptor,
                                @NotNull ServerSettings serverSettings,
                                @NotNull ServerPaths serverPaths) {
    mySettingsJSP = descriptor.getPluginResourcesPath(ArtifactoryConstants.ARTIFACTORY_SETTINGS_PATH + ".jsp");
    myServerSettings = serverSettings;
    myServerPaths = serverPaths;
    registry.registerStorageType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return ArtifactoryConstants.ARTIFACTORY_STORAGE_TYPE;
  }

  @NotNull
  @Override
  public String getName() {
    return "Artifactory Storage";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Uses Artifactory repository to store build artifacts";
  }

  @NotNull
  @Override
  public String getEditStorageParametersPath() {
    return mySettingsJSP;
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultParameters() {
    return new HashMap<>();
  }

  @Nullable
  @Override
  public PropertiesProcessor getParametersProcessor() {
    return params -> {
      final ArrayList<InvalidProperty> invalids = new ArrayList<>();
      for (Map.Entry<String, String> e : ArtifactoryUtil.validateParameters(params, true).entrySet()) {
        invalids.add(new InvalidProperty(e.getKey(), e.getValue()));
      }

      final String repositoryKey = ArtifactoryUtil.getRepositoryKey(params);
      if (repositoryKey != null) {
        try {
          Artifactory afClient = ArtifactoryUtil.getClient(params);
          RepositoryHandle repository = afClient.repository(repositoryKey);

          if (repository.exists() == false) {
            invalids.add(new InvalidProperty(ArtifactoryConstants.ARTIFACTORY_REPOSITORY_KEY, "Repository does not exist"));
          }
        } catch (Throwable e) {
          invalids.add(new InvalidProperty(ArtifactoryConstants.ARTIFACTORY_REPOSITORY_KEY, e.getMessage()));
        }
      }

      return invalids;
    };
  }

  @NotNull
  @Override
  public SettingsPreprocessor getSettingsPreprocessor() {
    return input -> {
      final Map<String, String> output = new HashMap<>(input);
      if(StringUtil.isNotEmpty(input.get(ArtifactoryConstants.SECURE_ARTIFACTORY_ACCESS_TOKEN))){
        output.remove(ArtifactoryConstants.SECURE_ARTIFACTORY_PASSWORD);
      }
      if(StringUtil.isNotEmpty(input.get(ArtifactoryConstants.SECURE_ARTIFACTORY_PASSWORD))){
        output.remove(ArtifactoryConstants.SECURE_ARTIFACTORY_ACCESS_TOKEN);
      }
      return output;
    };
  }
}
