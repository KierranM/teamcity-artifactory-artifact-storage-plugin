<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>
<jsp:useBean id="params" class="io.github.kierranm.teamcity.artifacts.artifactory.web.ArtifactoryParametersProvider"/>

<style type="text/css">
    .runnerFormTable {
        margin-top: 1em;
    }
</style>

<l:settingsGroup title="Artifactory Parameters">
    <tr>
        <th><label for="${params.url}">Artifactory URL: </label></th>
        <td>
            <props:textProperty name="${params.url}" className="longField" maxlength="256"/>
        </td>
    </tr>
    <tr>
        <th><label for="${params.username}">Username: </label></th>
        <td>
            <props:textProperty name="${params.username}" className="longField" maxlength="256"/>
            <span class="smallNote">Leave empty if using anonymous authentication or a system access token</span>
        </td>
    </tr>
    <tr>
        <th><label for="secure:${params.password}">Password: </label></th>
        <td>
            <props:passwordProperty name="secure:${params.password}" className="longField" maxlength="256"/>
            <span id="error_secure:${params.password}" class="error"></span>
            <span class="smallNote">Password of an authorized Artifactory user.</span>
            <span class="smallNote">Leave blank if using a user access token</span>
        </td>
    </tr>
    <tr>
        <th><label for="secure:${params.accessToken}">Access Token: </label></th>
        <td>
            <props:passwordProperty name="secure:${params.accessToken}" className="longField" maxlength="256"/>
            <span id="error_secure:${params.accessToken}" class="error"></span>
            <span class="smallNote">Authorized Artifactory Access Token</span>
            <span class="smallNote">Leave blank if using a user password</span>
        </td>
    </tr>
    <tr>
        <th><label for="${params.repositoryType}">Artifactory repository type: <l:star/></label></th>
        <td>
            <div class="posRel">
                <c:set var="repository" value="${propertiesBean.properties[params.repositoryKey]}"/>
                <props:selectProperty name="${params.repositoryType}" className="longField">
                    <props:option value="local">Local</props:option>
                    <props:option value="virtual">Virtual</props:option>
                </props:selectProperty>
            </div>
            <span class="error" id="error_${params.repositoryType}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${params.repositoryKey}">Artifactory repository name: <l:star/></label></th>
        <td>
            <div class="posRel">
                <c:set var="repository" value="${propertiesBean.properties[params.repositoryKey]}"/>
                <props:selectProperty name="${params.repositoryKey}" className="longField">
                    <props:option value="">-- Select Repository --</props:option>
                    <c:if test="${not empty repository}">
                        <props:option value="${repository}"><c:out value="${repository}"/></props:option>
                    </c:if>
                </props:selectProperty>
                <i class="icon-refresh" title="Reload Repositories" id="repositories-refresh"></i>
            </div>
            <span class="smallNote">Existing Artifactory repository to store artifacts</span>
            <span class="error" id="error_${params.repositoryKey}"></span>
        </td>
    </tr>
</l:settingsGroup>

<script type="text/javascript">
    var username = BS.Util.escapeId('${params.username}');
    var url = BS.Util.escapeId('${params.url}');
    var repositoryType = BS.Util.escapeId('${params.repositoryType}');
    var password = BS.Util.escapeId('secure:${params.password}');
    var accessToken = BS.Util.escapeId('secure:${params.accessToken}');
    var pathPrefix = BS.Util.escapeId('${params.pathPrefix}');
    var $repositorySelector = $j(BS.Util.escapeId('${params.repositoryKey}'));

    function getErrors($response) {
        var $errors = $response.find("errors:eq(0) error");
        if ($errors.length) {
            return $j.map($errors, function (error) {
                return $j(error).text();
            }).join(", ");
        }

        return "";
    }

    function loadRepositories() {
        var parameters = BS.EditStorageForm.serializeParameters() + '&resource=repositories';
        var $refreshButton = $j('#repositories-refresh').addClass('icon-spin');
        $j.post(window['base_uri'] + '${params.containersPath}', parameters)
                .then(function (response) {
                    var $response = $j(response);
                    var errors = getErrors($response);
                    $j(BS.Util.escapeId('error_${params.repositoryKey}')).text(errors);
                    if (errors) {
                      return
                    }

                    // Save selected option
                    var value = $repositorySelector.val();

                    // Redraw selector
                    $repositorySelector.empty();
                    $repositorySelector.append($j("<option></option>").attr("value", "").text("-- Select Repository --"));
                    $response.find("repositories:eq(0) repository").each(function () {
                      var $this = $j(this);
                      var name = $this.text();
                      $repositorySelector.append($j("<option></option>").attr("value", name).text(name));
                    });

                    if (value) {
                        $repositorySelector.val(value);
                    }

                    $repositorySelector.change();
                })
                .always(function () {
                    $refreshButton.removeClass('icon-spin');
                });
    }
    $j(document).on('change', url + ', ' + username + ', ' + password + ', ' + accessToken + ', ' + repositoryType, function () {
        loadRepositories();
    });
    $j(document).on('click', '#repositories-refresh', function () {
        loadRepositories();
    });
</script>
