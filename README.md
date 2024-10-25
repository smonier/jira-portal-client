# JIRA Software Ticket List 

## Jahia 8 module

Display a datatable with all the Jira Issues from https://<YOUR_INSTANCE_NAME>.atlassian.net/jira/software/c/projects/<YOU_PROJECT_NAME>/issues/

The INSTANCE_NAME & PROJECT_NAME are part of the component definition
## Installation

Download the code source on github, recompile the module locally, and deploy the module through your Jahia Modules administration panel

```bash
mvn clean install
```

## Configuration
<YOUR_INSTALL_DIR>/digital-factory-data/karaf/etc/org.jahia.se.modules.jiraportalclient.taglibs.JiraIssueList.cfg
You'll need to specify:
```bash
jiraLogin=<JIRA_SOFTWARE_USERNAME>
jiraToken=<JIRA_SOFTWARE_API_TOKEN>
```
## Functionalities
- List the issues from your preferred Atlassian Jira Software Project
- List the issues from a project attached to a visitor profile (jExperience)
- List the issues based on a custom field defined in the visitor profile

- Toggle the description of the issue within the table
- Possibility to add activities to issues
- Possibility to create new issue (modal form)
- Possibility to transfer an issue to another project based on a transition id
- Possibility to generate a PDF based on the issue description stored within the files directory of the logged user

### Screenshots
![picture](./src/main/resources/images/jiraIssuesTables.png)





