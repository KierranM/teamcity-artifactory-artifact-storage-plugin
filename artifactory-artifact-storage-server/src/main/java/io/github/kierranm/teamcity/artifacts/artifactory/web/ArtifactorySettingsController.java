package io.github.kierranm.teamcity.artifacts.artifactory.web;

import com.intellij.openapi.diagnostic.Logger;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.github.kierranm.teamcity.artifacts.artifactory.ArtifactoryConstants;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.servlet.ModelAndView;

public class ArtifactorySettingsController extends BaseFormXmlController {

  private final static Logger LOG = Logger.getInstance(ArtifactorySettingsController.class.getName());
  private static final String FAILED_TO_PROCESS_REQUEST_FORMAT = "Failed to process '%s' request: ";
  private final Map<String, ResourceHandler> myHandlers = new HashMap<>();
  private final ServerPaths myServerPaths;

  public ArtifactorySettingsController(@NotNull final WebControllerManager manager,
                                       @NotNull final PluginDescriptor descriptor,
                                       @NotNull final ServerPaths serverPaths) {
    myServerPaths = serverPaths;
    final String path = descriptor.getPluginResourcesPath(ArtifactoryConstants.ARTIFACTORY_SETTINGS_PATH + ".html");
    manager.registerController(path, this);
    myHandlers.put("repositories", new RepositoriesResourceHandler());
  }

  @Override
  protected ModelAndView doGet(@NotNull final HttpServletRequest request,
                               @NotNull final HttpServletResponse response) {
    return null;
  }

  @Override
  protected void doPost(@NotNull final HttpServletRequest request,
                        @NotNull final HttpServletResponse response,
                        @NotNull final Element xmlResponse) {
    final ActionErrors errors = new ActionErrors();
    final Map<String, String> parameters = getProperties(request);

    final String resource = request.getParameter("resource");
    if (resource == null) {
      errors.addError("resource", "Invalid request: resource parameter was not set");
    } else {
      final ResourceHandler handler = myHandlers.get(resource);
      if (handler == null) {
        errors.addError("resource","Invalid request: unsupported resource " + resource);
      } else {
        try {
          xmlResponse.addContent(handler.getContent(parameters));
        } catch (IllegalArgumentException e) {
          final String message = String.format(FAILED_TO_PROCESS_REQUEST_FORMAT, resource);
          if (LOG.isDebugEnabled()) {
            LOG.debug(message, e);
          } else {
            LOG.info(message + e.getMessage());
          }
          errors.addError(resource, message);
        } catch (Throwable e) {
          final StringBuilder messageBuilder = new StringBuilder(String.format(FAILED_TO_PROCESS_REQUEST_FORMAT, resource));
          messageBuilder.append(e.getMessage());
          final String message = messageBuilder.toString();
          LOG.infoAndDebugDetails(message, e);
          errors.addError(resource, message);
        }
      }
    }

    if (errors.hasErrors()) {
      errors.serialize(xmlResponse);
    }
  }

  private Map<String, String> getProperties(final HttpServletRequest request) {
    final BasePropertiesBean propsBean = new BasePropertiesBean(null);
    PluginPropertiesUtil.bindPropertiesFromRequest(request, propsBean, true);
    return propsBean.getProperties();
  }
}
