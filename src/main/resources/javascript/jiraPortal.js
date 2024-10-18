function handleActionChange(selectElement, issueKey, jiraInstance, jiraProject, actionUrl) {
    var selectedValue = selectElement.value;

    // Check the selected option and perform the corresponding action
    if (selectedValue === 'addComment') {
        // Open the modal programmatically
        openCommentModal(issueKey, jiraInstance, jiraProject);
        $('#commentModal').modal('show');  // This line opens the modal using jQuery/Bootstrap
    } else if (selectedValue === 'createInvoice') {
        document.getElementById('spinner-overlay').style.display = 'block';
        // Add your logic for creating an invoice here
        createInvoice(issueKey, jiraInstance, jiraProject, actionUrl);
    }

    // Reset the select after the action is triggered
    selectElement.selectedIndex = 0;
}

// Example function for creating an invoice
function createInvoice(issueKey, jiraInstance, jiraProject, actionUrl) {
    setTimeout(async function () {
        var pdfFileName = issueKey + "-" + generateTimestamp();
        const formData = new FormData();
        formData.append('jiraInstance', jiraInstance);
        formData.append('jiraProject', jiraProject);
        formData.append('pdfFileName', pdfFileName);
        formData.append('issueKey', issueKey);

        console.log("Create Invoice action triggered for", pdfFileName);

        // Simulate an async operation (e.g., AJAX call)
        // You should replace this with your actual AJAX or asynchronous logic
        // Simulate processing
        console.log("Processing action for issue: " + issueKey);


        try {
            const response = await fetch(actionUrl, {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const result = await response.text();
                alert("Commande : " + pdfFileName + " créée");

            } else {
                alert("Error creating PDF: " + response.status + " " + response.message);
            }
        } catch (error) {
            alert("Request failed: " + error.message);
        }
        document.getElementById('spinner-overlay').style.display = 'none';
    }, 3000);
}

async function addNewComment(actionUrl, user) {
    document.getElementById('spinner-overlay').style.display = 'block';
    setTimeout(async function () {
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
    formData.append('user', user);


    try {
        const response = await fetch(actionUrl, {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const result = await response.text();
            // alert("Comment added successfully for Request : " + commentIssueKey);
            $('#commentModal').modal('hide');
            location.reload(true);
        } else {
            alert("Error adding comment: " + response.status + " " + response.message);
        }
    } catch (error) {
        alert("Request failed: " + error.message);
    }
        document.getElementById('spinner-overlay').style.display = 'none';
    }, 3000);
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

async function updateIssueStatus(jiraInstance, jiraProject, jiraIssueKey, jiraNewStatus, actionURL, targetProjectKey) {
    document.getElementById('spinner-overlay').style.display = 'block';
    setTimeout(async function () {
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
        formData.append('targetProjectKey', targetProjectKey);

        try {
            const response = await fetch(actionURL, {
                method: 'POST',
                body: formData
            });

            if (response.ok) {
                const result = await response.text();
                //    alert("Status updated successfully for Request : " + issueKey);
                location.reload(true);
            } else {
                alert("Error updating status: " + response.status + " " + response.statusText);
            }
        } catch (error) {
            alert("Request failed: " + error.message);
        }
        document.getElementById('spinner-overlay').style.display = 'none';
    }, 3000);
}

async function submitNewIssue(jiraInstance, jiraProject, actionURL) {
    // Get form data
    const summary = document.getElementById('summary').value;
    const description = document.getElementById('description').value;
    const issueType = document.getElementById('issueType').value;
    const priority = document.getElementById('priority').value;
    const marketNum = document.getElementById('marketNum').value;

    // Validation check
    if (!summary || !description || !issueType || !priority || !marketNum) {
        alert("All fields are required!");
        return;
    }

    // Create form data object
    const formData = new FormData();
    formData.append('summary', summary);
    formData.append('description', description);
    formData.append('issueType', issueType);
    formData.append('priority', priority);
    formData.append('marketNum', marketNum);
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
            //    alert("Issue created successfully " + result);
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

function generateTimestamp() {
    var now = new Date();

    var year = now.getFullYear();
    var month = ('0' + (now.getMonth() + 1)).slice(-2); // Months are 0-indexed
    var day = ('0' + now.getDate()).slice(-2);
    var hours = ('0' + now.getHours()).slice(-2);
    var minutes = ('0' + now.getMinutes()).slice(-2);
    var seconds = ('0' + now.getSeconds()).slice(-2);

    return year + month + day + hours + minutes + seconds;
}
