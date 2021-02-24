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
<%@ taglib prefix="jira" uri="http://www.jahia.org/tags/jiraIssuesList" %>

<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>


<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>
<c:set var="jiraInstance" value="${currentNode.properties['instanceName'].string}"/>
<c:set var="jiraProject" value="${currentNode.properties['projectName'].string}"/>

<c:set var="jiraIssueList" value="${jira:getJiraIssues(jiraInstance,jiraProject)}"/>


<div class="module_header">
    <div class="module_title">${currentNode.properties['jcr:title'].string}</div>
    <div class="module_divider">
    </div>
</div>
<div class="module_body_noheight">
    <table id="jiraIssueList-${currentNode.UUID}" class="table table-striped" style="width:100%">
        <thead>
        <tr>
            <th>Type</th>
            <th>Key</th>
            <th>Summary</th>
            <th>Assignee</th>
            <th>Reporter</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Created</th>
            <th>Last Modified</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${jiraIssueList}" var="jiraIssue" varStatus="status">
            <tr>
                <td><img height="16" width="16" src="${jiraIssue.getTypeIconUrl()}" alt="${jiraIssue.getType()}" title="${jiraIssue.getType()}"/></td>
                <td><a href="https://${jiraInstance}.atlassian.net/browse/${jiraIssue.getKey()}">${jiraIssue.getKey()}</a></td>
                <td>${jiraIssue.getSummary()}</td>
                <td>${jiraIssue.getAssignee()}</td>
                <td>${jiraIssue.getReporter()}</td>
                <td><img height="16" width="16" src="${jiraIssue.getPriorityIconUrl()}" alt="${jiraIssue.getPriority()}" title="${jiraIssue.getPriority()}"/></td>
                <td>${jiraIssue.getStatus()}</td>
                <td>${jiraIssue.getDateCreated()}</td>
                <td>${jiraIssue.getDateModified()}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>


<script language="JavaScript">
    $(document).ready(function() {
        $('#jiraIssueList-${currentNode.UUID}').DataTable();
    } );
</script>