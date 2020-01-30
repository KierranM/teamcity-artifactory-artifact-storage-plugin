package io.github.kierranm.teamcity.artifacts.artifactory;

import jetbrains.buildServer.artifacts.ArtifactListData;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryUtil {

  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  private static final Method PROBE_CONTENT_TYPE_METHOD = getProbeContentTypeMethod();
  private static final Method FILE_TO_PATH_METHOD = getFileToPathMethod();

  @NotNull
  public static Map<String, String> validateParameters(@NotNull Map<String, String> params, boolean acceptReferences) {
    final Map<String, String> invalids = new HashMap<String, String>();
    if (StringUtil.isEmptyOrSpaces(getRepositoryKey(params))) {
      invalids.put(ArtifactoryConstants.ARTIFACTORY_REPOSITORY_KEY, "Artifactory repository key must not be empty");
    }
    if (StringUtil.isEmptyOrSpaces(getUrl(params))) {
      invalids.put(ArtifactoryConstants.ARTIFACTORY_URL, "Artifactory URL must not be empty");
    }
    if (StringUtil.isEmptyOrSpaces(getUsername(params)) == false) {
      if (StringUtil.isEmptyOrSpaces(getPassword(params)) && StringUtil.isEmptyOrSpaces(getAccessToken(params))) {
        invalids.put(ArtifactoryConstants.SECURE_ARTIFACTORY_PASSWORD, "Artifactory password or token must not be empty if username is set");
        invalids.put(ArtifactoryConstants.SECURE_ARTIFACTORY_ACCESS_TOKEN, "Artifactory password or token must not be empty if username is set");
      }
    }
    return invalids;
  }

  @NotNull
  public static Map<String, String> validateParameters(@NotNull Map<String, String> params) throws IllegalArgumentException {
    final Map<String, String> invalids = validateParameters(params, false);
    if (invalids.isEmpty()) return params;
    throw new IllegalArgumentException(joinStrings(invalids.values()));
  }

  @NotNull
  private static String joinStrings(@NotNull Collection<String> strings) {
    if (strings.isEmpty()) return StringUtil.EMPTY;
    final StringBuilder sb = new StringBuilder();
    for (String s : strings) sb.append(s).append("\n");
    return sb.toString();
  }

  @Nullable
  public static String getUrl(@NotNull Map<String, String> params) {
    return params.get(ArtifactoryConstants.ARTIFACTORY_URL);
  }

  @Nullable
  public static String getRepositoryKey(@NotNull Map<String, String> params) {
    return params.get(ArtifactoryConstants.ARTIFACTORY_REPOSITORY_KEY);
  }

  @Nullable
  public static String getRepositoryType(@NotNull Map<String, String> params) {
    return params.get(ArtifactoryConstants.ARTIFACTORY_REPOSITORY_TYPE);
  }

  @Nullable
  public static String getUsername(@NotNull Map<String, String> params) {
    return params.get(ArtifactoryConstants.ARTIFACTORY_USERNAME);
  }

  @Nullable
  public static String getPassword(@NotNull Map<String, String> params) {
    return params.get(ArtifactoryConstants.SECURE_ARTIFACTORY_PASSWORD);
  }

  @Nullable
  public static String getAccessToken(@NotNull Map<String, String> params) {
    return params.get(ArtifactoryConstants.SECURE_ARTIFACTORY_ACCESS_TOKEN);
  }

  @Nullable
  public static String getPathPrefix(@NotNull ArtifactListData artifactsInfo) {
    return getPathPrefix(artifactsInfo.getCommonProperties());
  }

  @Nullable
  public static String getPathPrefix(@NotNull Map<String, String> properties) {
    return properties.get(ArtifactoryConstants.ARTIFACTORY_REPOSITORY_PATH_PREFIX_ATTR);
  }

  public static String getContentType(File file) {
    String contentType = URLConnection.guessContentTypeFromName(file.getName());
    if (StringUtil.isNotEmpty(contentType)) {
      return contentType;
    }
    if (PROBE_CONTENT_TYPE_METHOD != null && FILE_TO_PATH_METHOD != null) {
      try {
        Object result = PROBE_CONTENT_TYPE_METHOD.invoke(null, FILE_TO_PATH_METHOD.invoke(file));
        if (result instanceof String) {
          contentType = (String)result;
        }
      } catch (Exception ignored) {
      }
    }
    return StringUtil.notEmpty(contentType, DEFAULT_CONTENT_TYPE);
  }

  public static String normalizeArtifactPath(final String path, final File file) {
    if (StringUtil.isEmpty(path)) {
      return file.getName();
    } else {
      return FileUtil.normalizeRelativePath(String.format("%s/%s", path, file.getName()));
    }
  }

  public static Artifactory getClient(Map<String, String> params) {
    ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
    builder.setUrl(getUrl(params));
    if (StringUtil.isEmptyOrSpaces(getUsername(params)) == false) {
      builder.setUsername(getUsername(params));
      if (StringUtil.isEmptyOrSpaces(getPassword(params)) == false) {
        builder.setPassword(getPassword(params));
      }
    }
    if (StringUtil.isEmptyOrSpaces(getAccessToken(params)) == false) {
      builder.setAccessToken(getAccessToken(params));
    }
    return builder.build();
  }

  private static Method getProbeContentTypeMethod() {
      try {
        Class<?> filesClass = Class.forName("java.nio.file.Files");
        Class<?> pathClass = Class.forName("java.nio.file.Path");
        if (filesClass != null && pathClass != null) {
          return filesClass.getMethod("probeContentType", pathClass);
        }
      } catch (Exception ignored) {
      }
      return null;
  }

  private static Method getFileToPathMethod() {
      try {
          return File.class.getMethod("toPath");
      } catch (Exception ignored) {
      }
      return null;
  }
}
