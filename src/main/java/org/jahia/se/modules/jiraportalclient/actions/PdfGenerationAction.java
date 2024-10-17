package org.jahia.se.modules.jiraportalclient.actions;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.se.modules.jiraportalclient.services.JiraIssueService;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.jahia.se.modules.jiraportalclient.functions.JiraUtils.jiraToHtml;

@Component(service = Action.class, immediate = true)
public class PdfGenerationAction extends Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfGenerationAction.class);
    private JiraIssueService jiraIssueService;

    @Activate
    public void activate() {
        LOGGER.info("Activating PdfGenerationAction");
        setName("generatePdfFromHtml");
        setRequiredMethods("GET,POST");
    }

    @Reference(service = JiraIssueService.class)
    public void setJiraIssueService(JiraIssueService jiraIssueService) {
        this.jiraIssueService = jiraIssueService;
    }

    private static String jiraLogin;
    private static String jiraToken;

    public static void setJiraCredentials(String login, String token) {
        LOGGER.info("Setting Jira credentials");
        jiraLogin = login;
        jiraToken = token;
    }

    @Override
    public ActionResult doExecute(
            final HttpServletRequest request,
            final RenderContext renderContext,
            final Resource resource,
            final JCRSessionWrapper session,
            Map<String, List<String>> parameters,
            final URLResolver urlResolver) throws Exception {

        LOGGER.info("Starting PDF generation process");

        // Retrieve the required parameters
        String jiraInstance = getParameter(parameters, "jiraInstance");
        if (jiraInstance == null) {
            LOGGER.error("Missing required parameter: jiraInstance");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST);
        }

        String issueKey = getParameter(parameters, "issueKey");
        if (issueKey == null) {
            LOGGER.error("Missing required parameter: issueKey");
            return new ActionResult(HttpServletResponse.SC_BAD_REQUEST);
        }

        LOGGER.info("Generating PDF for issue key: {} in instance: {}", issueKey, jiraInstance);
        String pdfFileName = getParameter(parameters, "pdfFileName");

        // Get HTML content from Jira issue
        String htmlContent = jiraToHtml(jiraIssueService.getIssueDescription(jiraInstance, issueKey));
        LOGGER.info("HTML content retrieved for issue {}: {}", issueKey, htmlContent);
        String invoiceContent = generateInvoice(pdfFileName,htmlContent);
        // Generate PDF from HTML
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(pdfOutputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);

            HtmlConverter.convertToPdf(invoiceContent, pdfDoc.getWriter());
           // pdfDoc.close();

            LOGGER.info("PDF generated successfully for issue: {}", issueKey);
        } catch (Exception e) {
            LOGGER.error("Error generating PDF from HTML for issue: {}", issueKey, e);
            return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // Store the PDF in JCR
        return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, new JCRCallback<ActionResult>() {
            @Override
            public ActionResult doInJCR(JCRSessionWrapper session) throws RepositoryException {
                try {
                    JCRUserNode user = JahiaUserManagerService.getInstance().lookupUserByPath(renderContext.getUser().getLocalPath());
                    JCRNodeWrapper userNode = session.getNode(user.getPath());

                    LOGGER.info("Storing PDF under user node: {}", userNode.getPath());

                    // Define file name
                    String pdfFileName = getParameter(parameters, "pdfFileName");
                    if (pdfFileName == null || pdfFileName.isEmpty()) {
                        pdfFileName = issueKey + ".pdf";  // Default file name if not provided
                    }

                    // Create or get the 'factures' folder
                    JCRNodeWrapper filesNode;
                    if (!userNode.hasNode("files")) {
                        filesNode = userNode.addNode("files", "jnt:folder");
                        if (!filesNode.isNodeType("jmix:autoPublish")) {
                            filesNode.addMixin("jmix:autoPublish");

                        }
                    }
                    JCRNodeWrapper filesFolderNode = userNode.getNode("files");

                    if (!filesFolderNode.hasNode("factures")) {
                        filesNode = filesFolderNode.addNode("factures", "jnt:folder");
                        filesNode.addMixin("jmix:autoPublish");
                        LOGGER.info("Created new 'factures' folder for user: {}", userNode.getPath());
                    } else {
                        filesNode = filesFolderNode.getNode("factures");
                        LOGGER.info("'Factures' folder exists for user: {}", userNode.getPath());
                    }

                    // Convert the PDF stream to InputStream and store it
                    ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(pdfOutputStream.toByteArray());
                    JCRNodeWrapper pdfFileNode = filesNode.uploadFile(pdfFileName, pdfInputStream, "application/pdf");

                    // Ensure autoPublish mixin is added to the PDF node
                    if (!pdfFileNode.isNodeType("jmix:autoPublish")) {
                        pdfFileNode.addMixin("jmix:autoPublish");
                    }

                    session.save();
                    LOGGER.info("PDF stored successfully at: {}", pdfFileNode.getPath());

                    // Return the response with the path of the stored PDF
                    JSONObject resp = new JSONObject();
                    resp.put("status", "success");
                    resp.put("path", pdfFileNode.getPath());

                    return new ActionResult(HttpServletResponse.SC_OK, null, resp);
                } catch (Exception e) {
                    LOGGER.error("Error storing PDF in JCR", e);
                    return new ActionResult(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        });
    }

    private String generateInvoice(String pdfFileName, String itemsDescription) {
        String invoiceContent =
                "<!DOCTYPE html>\n" +
                        "<html lang=\"fr\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <title>Facture</title>\n" +
                        "    <style>\n" +
                        "        body {\n" +
                        "            font-family: Arial, sans-serif;\n" +
                        "            background-color: #f5f5f5;\n" +
                        "            margin: 0;\n" +
                        "            padding: 20px;\n" +
                        "        }\n" +
                        "        .invoice-container {\n" +
                        "            max-width: 800px;\n" +
                        "            margin: 0 auto;\n" +
                        "            background-color: #fff;\n" +
                        "            padding: 20px;\n" +
                        "            border-radius: 8px;\n" +
                        "            box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\n" +
                        "        }\n" +
                        "        .header {\n" +
                        "            text-align: center;\n" +
                        "            margin-bottom: 20px;\n" +
                        "        }\n" +
                        "        .header img {\n" +
                        "            max-width: 150px;\n" +
                        "        }\n" +
                        "        .header h1 {\n" +
                        "            margin: 0;\n" +
                        "            font-size: 24px;\n" +
                        "        }\n" +
                        "        .invoice-details {\n" +
                        "            margin-bottom: 30px;\n" +
                        "        }\n" +
                        "        .invoice-details .info {\n" +
                        "            display: flex;\n" +
                        "            justify-content: space-between;\n" +
                        "            margin-bottom: 20px;\n" +
                        "        }\n" +
                        "        .invoice-details h2 {\n" +
                        "            font-size: 20px;\n" +
                        "        }\n" +
                        "        .invoice-items {\n" +
                        "            width: 100%;\n" +
                        "            border-collapse: collapse;\n" +
                        "            margin-bottom: 30px;\n" +
                        "        }\n" +
                        "        .invoice-items th, .invoice-items td {\n" +
                        "            padding: 10px;\n" +
                        "            border: 1px solid #ccc;\n" +
                        "            text-align: left;\n" +
                        "        }\n" +
                        "        .invoice-items th {\n" +
                        "            background-color: #f0f0f0;\n" +
                        "        }\n" +
                        "        .invoice-summary {\n" +
                        "            text-align: right;\n" +
                        "            margin-top: 20px;\n" +
                        "        }\n" +
                        "        .invoice-summary h3 {\n" +
                        "            margin: 0;\n" +
                        "            font-size: 18px;\n" +
                        "        }\n" +
                        "        .footer {\n" +
                        "            text-align: center;\n" +
                        "            margin-top: 30px;\n" +
                        "            font-size: 12px;\n" +
                        "            color: #777;\n" +
                        "        }\n" +
                        "    </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"invoice-container\">\n" +
                        "        <!-- Invoice Header -->\n" +
                        "        <div class=\"header\">\n" +
                        "            <img src=\"https://www.ugap.fr/_nuxt/img/logo_ugap.02a35b1.svg\" alt=\"Logo de l'entreprise\">\n" +
                        "            <h1>Facture</h1>\n" +
                        "        </div>\n" +
                        "\n" +
                        "        <!-- Invoice Details -->\n" +
                        "        <div class=\"invoice-details\">\n" +
                        "            <div class=\"info\">\n" +
                        "                <div>\n" +
                        "                    <h2>De :</h2>\n" +
                        "                    <p><strong>Nom de l'entreprise</strong></p>\n" +
                        "                    <p>Adresse de l'entreprise</p>\n" +
                        "                    <p>Email : contact@entreprise.com</p>\n" +
                        "                    <p>Téléphone : +33 1 23 45 67 89</p>\n" +
                        "                </div>\n" +
                        "                <div>\n" +
                        "                    <h2>À :</h2>\n" +
                        "                    <p><strong>Nom du client</strong></p>\n" +
                        "                    <p>Adresse du client</p>\n" +
                        "                    <p>Email : client@example.com</p>\n" +
                        "                    <p>Téléphone : +33 6 78 90 12 34</p>\n" +
                        "                </div>\n" +
                        "            </div>\n" +
                        "            <p><strong>Date de la facture :</strong> " + getCurrentDateWithPattern("d MMMM yyyy") +"</p>\n" +
                        "            <p><strong>Numéro de la facture :</strong> " + pdfFileName + "</p>\n" +
                        "        </div>\n" +
                        "\n" +
                        itemsDescription +
                        "\n" +
                        "        <!-- Footer -->\n" +
                        "        <div class=\"footer\">\n" +
                        "            <p>Merci pour votre confiance.</p>\n" +
                        "            <p>Nom de l'entreprise - www.entreprise.com</p>\n" +
                        "        </div>\n" +
                        "    </div>\n" +
                        "</body>\n" +
                        "</html>";
        return invoiceContent;
    }

    // Function to get the current date formatted with the given pattern
    public static String getCurrentDateWithPattern(String pattern) {
        // Get the current date
        LocalDate currentDate = LocalDate.now();

        // Create a DateTimeFormatter with the provided pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        // Format the current date
        return currentDate.format(formatter);
    }
}