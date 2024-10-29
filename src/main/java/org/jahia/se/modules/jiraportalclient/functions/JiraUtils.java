package org.jahia.se.modules.jiraportalclient.functions;

import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.io.IOUtils;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.jcr.RepositoryException;
import java.awt.image.BufferedImage;
import java.net.URLConnection;
import java.util.Arrays;

public class JiraUtils {

    private static final Logger logger = LoggerFactory.getLogger(JiraUtils.class);

    // 1. Convert Jira Markdown to HTML
    public static String jiraToHtml(String jiraMarkdown) {
        // Pre-process Jira-style markdown into standard Markdown format
        String preProcessedMarkdown = preProcessJiraMarkdown(jiraMarkdown);

        // Flexmark options for tables and other markdown features
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));

        // Create the Flexmark parser and renderer
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // Parse the Markdown and convert it to HTML
        Node document = parser.parse(preProcessedMarkdown);
        return renderer.render(document);
    }

    // Pre-process Jira markdown for headings, tables, and other specific features
    private static String preProcessJiraMarkdown(String jiraMarkdown) {
        // Process Jira-style headings (e.g., "h1. Heading")
        String processedMarkdown = convertJiraHeadings(jiraMarkdown);

        // Process Jira bold and italic syntax
        processedMarkdown = convertJiraBoldAndItalic(processedMarkdown);

        // Process Jira blockquotes (bq.)
        processedMarkdown = convertJiraBlockquotes(processedMarkdown);

        // Process Jira code blocks and noformat blocks
        processedMarkdown = convertJiraCodeBlocks(processedMarkdown);

        // Process Jira links and images
        processedMarkdown = convertJiraLinksAndImages(processedMarkdown);

        // Process Jira tables
        processedMarkdown = convertJiraTablesToMarkdown(processedMarkdown);

        return processedMarkdown;
    }

    // Convert Jira headings (e.g., "h1. Heading") to Markdown headings (e.g., "# Heading")
    private static String convertJiraHeadings(String text) {
        // Replace Jira headings with Markdown equivalents
        text = text.replaceAll("^h1\\.\\s", "# ");
        text = text.replaceAll("^h2\\.\\s", "## ");
        text = text.replaceAll("^h3\\.\\s", "### ");
        text = text.replaceAll("^h4\\.\\s", "#### ");
        text = text.replaceAll("^h5\\.\\s", "##### ");
        text = text.replaceAll("^h6\\.\\s", "###### ");
        return text;
    }

    // Convert Jira bold (*) and italic (_) formatting to standard Markdown
    private static String convertJiraBoldAndItalic(String text) {
        // Convert bold and italic formatting
        text = text.replaceAll("\\*(.*?)\\*", "**$1**"); // Convert *text* to **text** for bold
        text = text.replaceAll("_(.*?)_", "*$1*");       // Convert _text_ to *text* for italic
        return text;
    }

    // Convert Jira blockquotes (bq.) to Markdown blockquotes
    private static String convertJiraBlockquotes(String text) {
        return text.replaceAll("^bq\\.\\s", "> "); // Convert "bq. Text" to "> Text" for blockquotes
    }

    // Convert Jira code blocks and noformat blocks to standard Markdown code blocks
    private static String convertJiraCodeBlocks(String text) {
        // Replace {code} and {noformat} blocks with ``` for code blocks
        text = text.replaceAll("\\{code\\}", "```");
        text = text.replaceAll("\\{noformat\\}", "```");
        return text;
    }

    // Convert Jira links and images to Markdown equivalents
    private static String convertJiraLinksAndImages(String text) {
        // Convert Jira-style links: [text|url] to [text](url)
        text = text.replaceAll("\\$begin:math:display\\$(.*?)\\\\\\|(.*?)\\$end:math:display\\$", "[$1]($2)");
        // Convert Jira-style images: !image.jpg! to ![image.jpg](image.jpg)
        text = text.replaceAll("!(.*?)!", "![$1]($1)");
        return text;
    }

    // Convert Jira tables (||...|| and |...|) to standard Markdown tables
    private static String convertJiraTablesToMarkdown(String jiraMarkdown) {
        StringBuilder markdown = new StringBuilder();

        // Split the Jira Markdown into lines
        String[] lines = jiraMarkdown.split("\n");

        for (String line : lines) {
            // Check if the line contains headers (||...||)
            if (line.startsWith("||")) {
                // Replace Jira's `||` headers with Markdown `|` headers
                String[] headers = line.split("\\|\\|");
                StringBuilder headerRow = new StringBuilder("|");

                for (String header : headers) {
                    if (!header.trim().isEmpty()) {
                        // Remove * for bold in headers
                        String headerText = header.replace("*", "").trim();
                        headerRow.append(headerText).append("|");
                    }
                }

                // Add underline row for Markdown table header (---)
                markdown.append(headerRow.toString()).append("\n");

                String underlineRow = headerRow.toString().replaceAll("[^|]", "-");
                markdown.append(underlineRow).append("\n");
            } else if (line.startsWith("|")) {
                // Handle regular table rows (|...|)
                String[] cells = line.split("\\|");
                StringBuilder row = new StringBuilder("|");

                for (String cell : cells) {
                    if (!cell.trim().isEmpty()) {
                        String cellText = cell.trim();
                        row.append(cellText).append("|");
                    }
                }
                markdown.append(row.toString()).append("\n");
            } else {
                // Handle other non-table markdown lines as is
                markdown.append(line).append("\n");
            }
        }

        return markdown.toString();
    }


    // 2. Convert HTML to Jira Markdown
    public static String htmlToJira(String html) {
        // Use Jsoup to parse the HTML
        Document doc = Jsoup.parse(html);

        StringBuilder jiraMarkdown = new StringBuilder();

        // Convert <table> to Jira markdown format
        Elements tables = doc.select("table");
        for (Element table : tables) {
            Elements rows = table.select("tr");

            for (Element row : rows) {
                Elements headers = row.select("th");
                if (!headers.isEmpty()) {
                    // Handle table headers (||...||)
                    jiraMarkdown.append("||");
                    for (Element header : headers) {
                        jiraMarkdown.append("*").append(header.text()).append("*").append("||");
                    }
                    jiraMarkdown.append("\n");
                }

                Elements cells = row.select("td");
                if (!cells.isEmpty()) {
                    // Handle table rows (|...|)
                    jiraMarkdown.append("|");
                    for (Element cell : cells) {
                        jiraMarkdown.append(cell.text()).append("|");
                    }
                    jiraMarkdown.append("\n");
                }
            }
        }

        return jiraMarkdown.toString();
    }

    public static String encodeImageToBase64(final String jcrPath) throws RepositoryException {
        // Check if the path is null before proceeding
        if (jcrPath == null || jcrPath.trim().isEmpty()) {
            logger.warn("Image path is null or empty. Skipping image encoding.");
            return null;
        }

        return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, session -> {
            if (session.nodeExists(jcrPath)) {
                JCRNodeWrapper fileNode = (JCRNodeWrapper) session.getNode(jcrPath);
                String mimeType = fileNode.getFileContent().getContentType();  // Extract MIME type

                try (InputStream inputStream = fileNode.getFileContent().downloadFile()) {
                    byte[] imageBytes = IOUtils.toByteArray(inputStream);
                    return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Node does not exist at path: " + jcrPath);
                return null;
            }
        });
    }
}
