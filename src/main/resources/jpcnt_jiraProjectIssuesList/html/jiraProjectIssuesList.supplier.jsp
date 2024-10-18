<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
<template:addResources type="css" resources="jiraTable.css"/>

<template:addResources type="javascript" resources="jiraPortal.js"/>
<template:addResources type="javascript" resources="jquery.dataTables.min.js"/>
<template:addResources type="javascript" resources="marked.min.js"/>


<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="jiraInstance" value="${currentNode.properties['instanceName'].string}"/>
<c:set var="jiraProject" value="${currentNode.properties['projectName'].string}"/>
<c:set var="activateButton" value="${currentNode.properties['activateButton'].string}"/>
<c:set var="buttonLabel" value="${currentNode.properties['buttonLabel'].string}"/>
<c:set var="context" value="${renderContext}"/>
<%--<c:set var="statusList" value="${['Requested', 'In Review', 'Approved', 'Rejected']}"/>--%>
<c:set var="jiraIssueList" value="${jira:getIssuesByCustomField(jiraInstance,jiraProject,context)}"/>


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
                <th>Type</th>
                <th>Key</th>
                <th>Summary</th>
                <th>EIN</th>
                <th>Assignee</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Change Status</th>
                <th>Created</th>
                <th>Last Modified</th>
                <th>Action</th>
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
                    <td><img height="16" width="16" src="${jiraIssue.getTypeIconUrl()}" alt="${jiraIssue.getType()}" title="${jiraIssue.getType()}"/></td>
                    <td><a href="https://${jiraInstance}.atlassian.net/browse/${jiraIssue.getKey()}">${jiraIssue.getKey()}</a></td>
                    <td>${jiraIssue.getSummary()}</td>
                    <td>${jiraIssue.getEin()}</td>
                    <td>${jiraIssue.getAssignee()}</td>
                    <td><img height="16" width="16" src="${jiraIssue.getPriorityIconUrl()}" alt="${jiraIssue.getPriority()}" title="${jiraIssue.getPriority()}"/></td>
                    <td>${jiraIssue.getStatus()}</td>
                    <td>
                        <c:url var="actionURL" value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>

                        <select onchange="updateIssueStatus('${jiraInstance}', '${jiraProject}', '${jiraIssue.getKey()}', this.value,'${actionURL}')">
                            <option value="">Select Status</option>
                            <c:forEach items="${statusList}" var="status">
                                <option value="${status.getId()}" <c:if test="${status.getValue() eq jiraIssue.getStatus()}">selected</c:if>>
                                        ${status.getValue()}
                                </option>
                            </c:forEach>
                        </select>
                    </td>
                    <td>${jiraIssue.getDateCreated()}</td>
                    <td>${jiraIssue.getDateModified()}</td>
                    <td>
                        <!-- Button to trigger the modal -->
                        <button type="button" class="btn-sm btn-primary"
                                data-toggle="modal"
                                data-target="#commentModal"
                                onclick="openCommentModal('${jiraIssue.getKey()}', '${jiraInstance}', '${jiraProject}')">
                            Add Comment
                        </button>
                    </td>
                </tr>


            </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<!-- Modal Structure -->
<div class="modal fade" id="commentModal" tabindex="-1" role="dialog" aria-labelledby="commentModalLabel" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="commentModalLabel">Add Comment to <span id="issueKeyDisplay"></span></h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <!-- Form inside the modal -->
                <form id="commentForm">
                    <div class="form-group">
                        <label for="commentText">Comment:</label>
                        <textarea class="form-control" id="commentText" name="commentText" rows="4" placeholder="Enter your comment here" required></textarea>
                    </div>
                    <c:url var="actionURL" value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>
                    <button type="button" class="btn btn-primary" onclick="addNewComment('${actionURL}')">Add Comment
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>
<!-- Modal -->
<div class="modal fade" id="newJiraModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
    <div class="modal-dialog" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="exampleModalLabel">Create a ${buttonLabel}</h5>
                <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
            <div class="modal-body">
                <div class="container mt-5">
                    <h2>Create New Change Request</h2>
                    <form id="jiraIssueForm">
                        <!-- Summary Field -->
                        <div class="form-group">
                            <label for="summary">Summary</label>
                            <input type="text" class="form-control" id="summary" name="summary" placeholder="Enter issue summary" required>
                        </div>

                        <!-- Description Field -->
                        <div class="form-group">
                            <label for="description">Description</label>
                            <textarea class="form-control" id="description" name="description" rows="4" placeholder="Enter issue description" required></textarea>
                        </div>

                        <!-- Issue Type Dropdown -->
                        <div class="form-group">
                            <label for="issueType">Issue Type</label>
                            <select class="form-control" id="issueType" name="issueType" required>
                                <option value="">Select Issue Type</option>
                                <!-- JSP Code to Populate Issue Types -->
                                <c:forEach items="${jira:getIssueTypesForProject(jiraInstance, jiraProject)}" var="issueType">
                                    <option value="${issueType.getLabel()}">${issueType.getLabel()}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <!-- Priority Dropdown -->
                        <div class="form-group">
                            <label for="priority">Priority</label>
                            <select class="form-control" id="priority" name="priority" required>
                                <option value="">Select Priority</option>
                                <option value="Highest">Highest</option>
                                <option value="High">High</option>
                                <option value="Medium">Medium</option>
                                <option value="Low">Low</option>
                                <option value="Lowest">Lowest</option>
                            </select>
                        </div>

                        <!-- Submit Button -->
                        <c:url var="actionURL" value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>
                        <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="submitNewIssue('${jiraInstance}','${jiraProject}','${actionURL}')">Create Issue</button>
                    </form>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
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
            columnDefs: [{ width: "5%", targets: 0 }],
            language: {
                emptyTable: 'No Tickets found'
            },
            lengthChange: false,
            pageLength: 10,
            autoWidth: false,
            responsive: true
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