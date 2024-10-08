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
                <th class="dt-control"></th>
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
                <th>Action</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${jiraIssueList}" var="jiraIssue" varStatus="status">
                <c:set var="statusList" value="${jira:getAvailableTransitions(jiraInstance, jiraIssue.getKey())}"/>
                <c:set var="activityList" value="${jira:getIssueActivities(jiraInstance, jiraIssue.getKey())}"/>

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
                    <button type="button" class="btn btn-primary" onclick="addNewComment()">Add Comment</button>
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

<script>
    // Function to open the modal and set dynamic content
    function openCommentModal(issueKey, jiraInstance, jiraProject) {
        // Set the issue key in the modal title
        document.getElementById('issueKeyDisplay').textContent = issueKey;

        // Set form data attributes for later use
        document.getElementById('commentForm').setAttribute('data-issue-key', issueKey);
        document.getElementById('commentForm').setAttribute('data-jira-instance', jiraInstance);
        document.getElementById('commentForm').setAttribute('data-jira-project', jiraProject);

        // Clear the textarea
        document.getElementById('commentText').value = '';
    }

    async function updateIssueStatus(jiraInstance, jiraProject, jiraIssueKey, jiraNewStatus) {
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

    async function submitNewIssue(jiraInstance, jiraProject) {
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

    async function addNewComment() {
        var form = document.getElementById('commentForm');
        var commentIssueKey = form.getAttribute('data-issue-key');
        var jiraInstance = form.getAttribute('data-jira-instance');
        var jiraProject = form.getAttribute('data-jira-project');
        var commentText = document.getElementById('commentText').value;

        const formData = new FormData();
        formData.append('jiraInstance', jiraInstance);
        formData.append('jiraProject', jiraProject);
        formData.append('jiraAction', "addComment");
        formData.append('issueKey', commentIssueKey);
        formData.append('commentText', commentText);

        try {
            const response = await fetch('<c:url value="${url.base}${currentNode.path}.requestJiraUpdate.do"/>', {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const result = await response.text();
                alert("Comment added successfully for Request : " + commentIssueKey);
                $('#commentModal').modal('hide');
                location.reload(true);
            } else {
                alert("Error updating status: " + response.status + " " + response.message);
            }
        } catch (error) {
            alert("Request failed: " + error.message);
        }
    }

</script>