<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="jira" uri="https://www.jahia.org/tags/jiraIssuesList" %>

<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="css" resources="jquery.dataTables.min.css"/>
<template:addResources type="css" resources="dataTables.bootstrap4.min.css"/>

<template:addResources type="css" resources="jiraTable.css"/>

<template:addResources type="javascript" resources="jiraPortal.js"/>
<template:addResources type="javascript" resources="jquery.dataTables.min.js"/>
<template:addResources type="javascript" resources="dataTables.bootstrap4.min.js"/>


<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="jiraInstance" value="${currentNode.properties['instanceName'].string}"/>
<c:set var="jiraProject" value="${currentNode.properties['projectName'].string}"/>
<c:set var="activateButton" value="${currentNode.properties['activateButton'].string}"/>
<c:set var="buttonLabel" value="${currentNode.properties['buttonLabel'].string}"/>
<c:set var="targetProjectKey" value="${currentNode.properties['targetProjectKey'].string}"/>
<c:set var="activatePdfCreation" value="${currentNode.properties['activatePdfCreation'].getBoolean()}"/>
<c:set var="useUnomiProfileProperty" value="${currentNode.properties['useUnomiProfileProperty'].getBoolean()}"/>
<c:set var="useJiraCustomField" value="${currentNode.properties['useJiraCustomField'].getBoolean()}"/>
<c:set var="logoPdf" value="${currentNode.properties['logoPdf'].node}"/>
<c:set var="language" value="${currentResource.locale.language}"/>

<c:set var="context" value="${renderContext}"/>
<c:choose>
    <c:when test="${useUnomiProfileProperty}">
        <c:set var="jiraIssueList" value="${jira:getJiraIssuesFromUnomi(jiraInstance, jiraProject, renderContext)}"/>
    </c:when>
    <c:when test="${useJiraCustomField}">
        <c:set var="jiraIssueList" value="${jira:getIssuesByCustomField(jiraInstance,jiraProject,renderContext)}"/>
    </c:when>
    <c:otherwise>
        <c:set var="jiraIssueList" value="${jira:getJiraIssues(jiraInstance, jiraProject)}"/>
    </c:otherwise>
</c:choose>

<jcr:node var="user" path="${context.user.localPath}"/>
<jcr:nodeProperty var="firstName" node="${user}" name="j:firstName"/>
<jcr:nodeProperty var="lastName" node="${user}" name="j:lastName"/>
<jcr:nodeProperty var="email" node="${user}" name="j:email"/>
<c:set var="loggedInUser" value="${firstName} ${lastName} (${email})"/>

<div class="portal-header" id="tableContainer-${currentNode.UUID}">
    <div class="module_header">
        <div class="module_title">${currentNode.properties['jcr:title'].string}</div>
        <div class="module_divider"></div>
    </div>
    <div class="module_body_noheight">
        <c:if test="${activateButton eq true}">
            <div style="float:left;line-height:12px;">
                <button type="button" class="btn btn-primary mb-3" data-toggle="modal" data-target="#newJiraModal">
                        ${buttonLabel}
                </button>
            </div>
        </c:if>
        <table id="jiraIssueList-${currentNode.UUID}" class="table table-striped">
            <thead>
            <tr>
                <th class="dt-control"></th>
                <th><fmt:message key="table.header.type" /></th>
                <th><fmt:message key="table.header.key" /></th>
                <th><fmt:message key="table.header.title" /></th>
                <th><fmt:message key="table.header.reporter" /></th>
                <th><fmt:message key="table.header.assignee" /></th>
                <th><fmt:message key="table.header.priority" /></th>
                <th><fmt:message key="table.header.status" /></th>
                <th><fmt:message key="table.header.changeStatus" /></th>
                <th><fmt:message key="table.header.createdOn" /></th>
                <th><fmt:message key="table.header.updatedOn" /></th>
                <th><fmt:message key="table.header.action" /></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${jiraIssueList}" var="jiraIssue" varStatus="status">
                <c:set var="statusList" value="${jira:getAvailableTransitions(jiraInstance, jiraIssue.getKey())}"/>
                <c:set var="activityList" value="${jira:getIssueActivities(jiraInstance, jiraIssue.getKey())}"/>
                <c:set var="activityListFormatted" value=""/>
                <c:if test="${not empty activityList}">
                    <c:set var="activityListFormatted" value="<strong>Activities: </strong><br/>"/>

                    <c:forEach items="${activityList}" var="activity">
                        <c:set var="activityListFormatted" value="${activityListFormatted}${activity}<br/>"/>
                    </c:forEach>
                </c:if>

                <tr class="main-row" data-description="${jiraIssue.getDescription()}<br/>${activityListFormatted}<br/>">
                    <td class="dt-control"></td>
                    <td><img height="16" width="16" src="${jiraIssue.getTypeIconUrl()}" alt="${jiraIssue.getType()}"
                             title="${jiraIssue.getType()}"/></td>
                    <td>
                        <a href="https://${jiraInstance}.atlassian.net/browse/${jiraIssue.getKey()}" target="_blank">${jiraIssue.getKey()}</a>
                    </td>
                    <td>${jiraIssue.getSummary()}</td>
                    <td>${jiraIssue.getReporter()}</td>
                    <td>${jiraIssue.getAssignee()}</td>
                    <td><img height="16" width="16" src="${jiraIssue.getPriorityIconUrl()}"
                             alt="${jiraIssue.getPriority()}" title="${jiraIssue.getPriority()}"/></td>
                    <td>${jiraIssue.getStatus()}</td>
                    <td>
                        <c:url var="actionURL" value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>
                        <c:set var="triggerStatusId" value="${currentNode.properties['triggerStatusId'].string}"/>

                        <select class="form-control-sm"
                                onchange="updateIssueStatus('${jiraInstance}', '${jiraProject}', '${jiraIssue.getKey()}', this.value,'${actionURL}','${targetProjectKey}','${triggerStatusId}')">
                            <option value=""><fmt:message key="select.option.selectStatus" /></option>
                            <c:forEach items="${statusList}" var="status">
                                <option value="${status.getId()}"
                                        <c:if test="${status.getValue() eq jiraIssue.getStatus()}">selected</c:if>>
                                        ${status.getValue()}
                                </option>
                            </c:forEach>
                        </select>
                    </td>
                    <td>${jiraIssue.getDateCreated(language)}</td>
                    <td>${jiraIssue.getDateModified(language)}</td>
                    <td>
                        <c:choose>
                            <c:when test="${jcr:isNodeType(currentNode, 'jpcmix:pdfCreation')}">
                                <c:set var="folderName" value="${currentNode.properties['folderName'].string}"/>

                                <!-- Display the select when activatePdfCreation is true -->
                                <c:url var="actionURL2" value="${url.base}${currentNode.path}.generatePdfFromHtml.do"/>
                                <c:url var="imagePath" value="${logoPdf.path}"/>

                                <select class="form-control-sm"
                                        onchange="handleActionChange(this, '${jiraIssue.getKey()}', '${jiraInstance}', '${jiraProject}', '${actionURL2}', '${folderName}', '${imagePath}')">
                                    <option value="" disabled selected><fmt:message key="select.option.selectAction" /></option>
                                    <option value="addComment"><fmt:message key="select.option.comment" /></option>
                                    <option value="createInvoice"><fmt:message key="select.option.createInvoice" /></option>
                                </select>
                            </c:when>
                            <c:otherwise>
                                <!-- Display the button when activatePdfCreation is false -->
                                <button type="button" class="btn-sm btn-primary"
                                        data-toggle="modal"
                                        data-target="#commentModal"
                                        onclick="openCommentModal('${jiraIssue.getKey()}', '${jiraInstance}', '${jiraProject}')">
                                    <fmt:message key="select.option.comment" />
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>


            </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<!-- Modal Structure -->
<div class="modal fade" id="commentModal" tabindex="-1" role="dialog" aria-labelledby="commentModalLabel"
     aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="commentModalLabel">
                    <fmt:message key="commentModal.label.addCommentTo"/>&nbsp;<span id="issueKeyDisplay"></span>
                </h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <!-- Form inside the modal -->
                <form id="commentForm">
                    <div class="form-group">
                        <label for="commentText"><fmt:message key="commentModal.label.comment"/></label>
                        <textarea class="form-control" id="commentText" name="commentText" rows="4"
                                  placeholder="<fmt:message key='commentModal.placeholder.enterComment'/>" required></textarea>
                    </div>
                    <c:url var="actionURL" value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>
                    <button type="button" class="btn btn-primary"
                            onclick="addNewComment('${actionURL}','${loggedInUser}')">
                        <fmt:message key="commentModal.button.add"/>
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>
<!-- Modal -->
<div class="modal fade" id="newJiraModal" tabindex="-1" role="dialog" aria-labelledby="newJiraModalLabel"
     aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="newJiraModalLabel">
                    <fmt:message key="newJiraModal.label.title" />
                </h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <div class="container mt-5">
                    <h2><fmt:message key="newJiraModal.label.heading" /></h2>
                    <form id="jiraIssueForm">
                        <!-- Summary Field -->
                        <div class="form-group">
                            <label for="summary"><fmt:message key="newJiraModal.label.summary" /></label>
                            <input type="text" class="form-control" id="summary" name="summary"
                                   placeholder="<fmt:message key='newJiraModal.placeholder.summary'/>" required>
                        </div>

                        <!-- Description Field -->
                        <div class="form-group">
                            <label for="description"><fmt:message key="newJiraModal.label.description" /></label>
                            <textarea class="form-control" id="description" name="description" rows="4"
                                      placeholder="<fmt:message key='newJiraModal.placeholder.description'/>" required></textarea>
                        </div>

                        <!-- Marché Field -->
                        <div class="form-group">
                            <label for="marketNum"><fmt:message key="newJiraModal.label.market" /></label>
                            <input type="text" class="form-control" id="marketNum" name="marketNum"
                                   placeholder="<fmt:message key='newJiraModal.placeholder.market'/>" required>
                        </div>

                        <!-- Issue Type Dropdown -->
                        <div class="form-group">
                            <label for="issueType"><fmt:message key="newJiraModal.label.type" /></label>
                            <select class="form-control" id="issueType" name="issueType" required>
                                <option value=""><fmt:message key="newJiraModal.option.selectType" /></option>
                                <!-- JSP Code to Populate Issue Types -->
                                <c:forEach items="${jira:getIssueTypesForProject(jiraInstance, jiraProject)}"
                                           var="issueType">
                                    <option value="${issueType.getLabel()}">${issueType.getLabel()}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <!-- Priority Dropdown -->
                        <div class="form-group">
                            <label for="priority"><fmt:message key="newJiraModal.label.priority" /></label>
                            <select class="form-control" id="priority" name="priority" required>
                                <option value=""><fmt:message key="newJiraModal.option.selectPriority" /></option>
                                <option value="Highest"><fmt:message key="newJiraModal.option.priorityHighest" /></option>
                                <option value="High"><fmt:message key="newJiraModal.option.priorityHigh" /></option>
                                <option value="Medium"><fmt:message key="newJiraModal.option.priorityMedium" /></option>
                                <option value="Low"><fmt:message key="newJiraModal.option.priorityLow" /></option>
                                <option value="Lowest"><fmt:message key="newJiraModal.option.priorityLowest" /></option>
                            </select>
                        </div>

                        <!-- Submit Button -->
                        <c:url var="actionURL" value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>
                        <button type="button" class="btn btn-primary" data-dismiss="modal"
                                onclick="submitNewIssue('${jiraInstance}','${jiraProject}','${actionURL}')">
                            <fmt:message key="newJiraModal.button.submit" />
                        </button>
                    </form>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal">
                    <fmt:message key="newJiraModal.button.close" />
                </button>
            </div>
        </div>
    </div>
</div>

<script>
    $(document).ready(function () {
        // Initialize DataTables
        var table = $('#jiraIssueList-${currentNode.UUID}').DataTable({
            paging: true,
            bFilter: false,
            bInfo: true,
            scrollY: '50vh', // Set a reasonable scroll height
            scrollCollapse: true,
            columnDefs: [{width: "5%", targets: 0}],
            language: {
                emptyTable: 'No Issues found'
            },
            lengthChange: false,
            pageLength: 5,
            autoWidth: false,
            responsive: true,
            searching: true,
        });

        // Handle click event on the control cell to toggle collapsible row
        $('#jiraIssueList-${currentNode.UUID} tbody').on('click', '.dt-control', function () {
            var tr = $(this).closest('tr'); // Get the current row
            var row = table.row(tr); // Get DataTable row instance
            var controlCell = $(this); // Get the control cell element

            if (row.child.isShown()) {
                // Hide child row and change control text
                row.child.hide();
                tr.removeClass('shown');
                //controlCell.text('Show');
            } else {
                // Show child row with description from data attribute and change control text
                var description = tr.data('description'); // Get description from data attribute
                var descriptionHtml = '<div class="collapse-row"><strong>Description:</strong><br>' + description + '</div>';
                row.child(descriptionHtml).show();
                tr.addClass('shown');
                //controlCell.text('Hide');
            }
        });
    });
</script>

<!-- Spinner Overlay (initially hidden) -->
<div id="spinner-overlay" style="display:none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.5); z-index: 9999; text-align: center;">
    <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);">
        <div class="spinner-border" role="status" style="width: 3rem; height: 3rem; color: white;">
            <span class="sr-only">Loading...</span>
        </div>
        <p style="color: white; margin-top: 10px;">Loading ...</p> <!-- Add loading text -->
    </div>
</div>