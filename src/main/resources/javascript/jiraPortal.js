async function addNewComment(actionUrl) {
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
        const response = await fetch(actionUrl, {
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

async function updateIssueStatus(jiraInstance, jiraProject, jiraIssueKey, jiraNewStatus, actionURL) {
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
        const response = await fetch(actionURL, {
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

async function submitNewIssue(jiraInstance, jiraProject,actionURL) {
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
        const response = await fetch(actionURL, {
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