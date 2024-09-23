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
<template:addResources type="javascript" resources="jquery.dataTables.min.js"/>

<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="jiraInstance" value="${currentNode.properties['instanceName'].string}"/>
<c:set var="jiraProject" value="${currentNode.properties['projectName'].string}"/>
<c:set var="activateButton" value="${currentNode.properties['activateButton'].string}"/>
<c:set var="buttonLabel" value="${currentNode.properties['buttonLabel'].string}"/>
<c:set var="context" value="${renderContext}"/>
<%--<c:set var="statusList" value="${['Requested', 'In Review', 'Approved', 'Rejected']}"/>--%>
<c:set var="jiraIssueList" value="${jira:getJiraTickets(jiraInstance,jiraProject,context)}"/>

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
                <th>Type</th>
                <th>Key</th>
                <th>Summary</th>
                <th>Assignee</th>
                <th>Reporter</th>
                <th>Priority</th>
                <th>Status</th>
                <th>Change Status</th>
                <th>Created</th>
                <th>Last Modified</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${jiraIssueList}" var="jiraIssue" varStatus="status">
                <c:set var="statusList" value="${jira:getAvailableTransitions(jiraInstance, jiraIssue.getKey())}"/>

                <tr>
                    <td><img height="16" width="16" src="${jiraIssue.getTypeIconUrl()}" alt="${jiraIssue.getType()}" title="${jiraIssue.getType()}"/></td>
                    <td><a href="https://${jiraInstance}.atlassian.net/browse/${jiraIssue.getKey()}">${jiraIssue.getKey()}</a></td>
                    <td>${jiraIssue.getSummary()}</td>
                    <td>${jiraIssue.getAssignee()}</td>
                    <td>${jiraIssue.getReporter()}</td>
                    <td><img height="16" width="16" src="${jiraIssue.getPriorityIconUrl()}" alt="${jiraIssue.getPriority()}" title="${jiraIssue.getPriority()}"/></td>
                    <td>${jiraIssue.getStatus()}</td>
                    <td>
                        <select onchange="updateIssueStatus('${jiraInstance}', '${jiraProject}', '${jiraIssue.getKey()}', this.value)">
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
                </tr>
            </c:forEach>
            </tbody>
        </table>
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
                        <button type="button" class="btn btn-primary" data-dismiss="modal" onclick="submitNewIssue('${jiraInstance}','${jiraProject}')">Create Issue</button>
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
    function resizePage() {
        const container = $("tableContainer-${currentNode.UUID}");
        const height = container.height() - container.find(".dataTables_scrollHead").height();
        updateDataTable(height + "px");
    };

    var resizeTimer;

    $(window).resize(function () {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(resizePage, 100);

    });

    function updateDataTable(scrollHeight) {
        var table = $('#jiraIssueList-${currentNode.UUID}').DataTable(
            {
                destroy: true,
                paging: true,
                "bFilter": false,
                "bInfo": true,
                scrollY: scrollHeight,
                columnDefs: [{width: "5%", targets: 0}],
                language: {
                    emptyTable: 'No Tickets found'
                },
                lengthChange: false,
                pageLength: 10
            }
        );
        // $( table.table().container() ).removeClass( 'form-inline' );
        $("table.dataTable").css("font-size", "12px");
        table.columns.adjust().draw();
    }

    $(document).ready(function () {
        updateDataTable('1px'); // give it any height, it will be changed by the timer event, but it needs some size for the page to work
        resizeTimer = setTimeout(resizePage, 100);

    });
</script>

<script>
    async function updateIssueStatus(jiraInstance,jiraProject,jiraIssueKey,jiraNewStatus) {
        const selectedStatus = jiraNewStatus;

        // Check if a valid status is selected
        if (!selectedStatus) {
            alert("Please select a status.");
            return;
        }

        // Assuming these variables are available in the context
        const issueKey = jiraIssueKey; // Replace with your actual issue key

        const formData = new FormData();
        formData.append('jiraInstance', jiraInstance);
        formData.append('jiraProject', jiraProject);
        formData.append('jiraAction', "updateStatus");
        formData.append('issueKey', issueKey);
        formData.append('newStatus', selectedStatus);

        try {
            const response = await fetch('<c:url value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const result = await response.text();
                alert("Status updated successfully for Request : " + issueKey);
                location.reload(true);
            } else {
                alert("Error updating status: " + response.status + " " + response.statusText);
            }
        } catch (error) {
            alert("Request failed: " + error.message);
        }
    }
</script>

<script>
    async function submitNewIssue(jiraInstance,jiraProject) {
        // Get form data

        const summary = document.getElementById('summary').value;
        const description = document.getElementById('description').value;
        const issueType = document.getElementById('issueType').value;
        const priority = document.getElementById('priority').value;

        // Validation check
        if (!summary || !description || !issueType || !priority) {
            alert("All fields are required!");
            return;
        }

        // Create form data object
        const formData = new FormData();
        formData.append('summary', summary);
        formData.append('description', description);
        formData.append('issueType', issueType);
        formData.append('priority', priority);
        formData.append('jiraInstance', jiraInstance);
        formData.append('jiraProject', jiraProject);
        formData.append('jiraAction', "createIssue");


        try {
            // Make an AJAX POST request to the Java action URL
            const response = await fetch('<c:url value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>', {
                method: 'POST',
                body: formData
            });

            // Check response status
            if (response.ok) {
                const result = await response.text();
                alert("Issue created successfully " + result);
                location.reload(true);

            } else {
                const errorText = await response.text();
                alert("Failed to create issue: " + errorText);
            }
        } catch (error) {
            console.error("Error:", error);
            alert("An error occurred: " + error.message);
        }
    }
</script>
