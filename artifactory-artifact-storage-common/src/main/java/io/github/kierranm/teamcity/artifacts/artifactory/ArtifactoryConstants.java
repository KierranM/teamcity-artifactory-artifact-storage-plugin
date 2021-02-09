package io.github.kierranm.teamcity.artifacts.artifactory;

/**
 * Created by Kierran McPherson
 * date: 2019/07/20
 */
public class ArtifactoryConstants {

  public static final String ARTIFACTORY_URL = "storage.artifactory.url";
  public static final String ARTIFACTORY_REPOSITORY_TYPE = "storage.artifactory.repository.type";
  public static final String ARTIFACTORY_REPOSITORY_KEY = "storage.artifactory.repository.key";
  public static final String ARTIFACTORY_USERNAME = "storage.artifactory.username";
  public static final String SECURE_ARTIFACTORY_PASSWORD = "secure:storage.artifactory.password";
  public static final String SECURE_ARTIFACTORY_ACCESS_TOKEN = "secure:storage.artifactory.access_token";
  public static final String ARTIFACTORY_PASSWORD = "storage.artifactory.password";
  public static final String ARTIFACTORY_ACCESS_TOKEN = "storage.artifactory.access_token";


  public static final String ARTIFACTORY_STORAGE_TYPE = "Artifacactory_storage";
  public static final String ARTIFACTORY_SETTINGS_PATH = "artifactory_storage_settings";

  public static final String ARTIFACTORY_REPOSITORY_PATH_PREFIX_ATTR = "storage.artifactory.path_prefix";

  public static final String ARTIFACTORY_CLEANUP_BATCH_SIZE = "storage.artifactory.cleanup.batchSize";
  public static final String ARTIFACTORY_CLEANUP_USE_PARALLEL = "storage.artifactory.cleanup.useParallel";

  public static final String ARTIFACTORY_UPLOAD_PARALLELISM = "storage.artifactory.upload.parallelism";
}
