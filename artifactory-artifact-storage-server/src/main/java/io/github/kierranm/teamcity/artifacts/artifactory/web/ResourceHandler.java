package io.github.kierranm.teamcity.artifacts.artifactory.web;

import java.util.Map;
import org.jdom.Content;

public interface ResourceHandler {
  public Content getContent(Map<String, String> parameters) throws Exception;
}
