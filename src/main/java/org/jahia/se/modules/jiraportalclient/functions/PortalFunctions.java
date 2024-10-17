
package org.jahia.se.modules.jiraportalclient.functions;


import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.services.render.RenderContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.unomi.api.CustomItem;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.Item;
import org.apache.unomi.api.Profile;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class PortalFunctions {

    private static Logger logger = LoggerFactory.getLogger(PortalFunctions.class);

    public String getPropertyValue(String propertyName, RenderContext renderContext, ContextServerService contextServerService)
            throws RepositoryException, JSONException, IOException, InterruptedException, ExecutionException {

        Object propertyValue = null;
        logger.info("Getting Property Value for: " + propertyName);

        HttpServletRequest httpServletRequest = renderContext.getRequest();
        if (httpServletRequest == null) {
            logger.error("HttpServletRequest is null for the given RenderContext.");
            return null;
        }

        String siteKey = renderContext.getSite().getSiteKey();
        if (siteKey == null) {
            logger.error("SiteKey is null for the given RenderContext.");
            return null;
        }
        logger.info("SiteKey: " + siteKey);

        AsyncHttpClient asyncHttpClient = contextServerService.initAsyncHttpClient(siteKey);
        if (asyncHttpClient == null) {
            logger.error("Failed to initialize AsyncHttpClient for siteKey: " + siteKey);
            return null;
        }

        String profileId = contextServerService.getProfileId(httpServletRequest, siteKey);
        if (profileId == null) {
            logger.error("Failed to retrieve ProfileId for siteKey: " + siteKey);
            asyncHttpClient.closeAsynchronously();
            return null;
        }
        logger.info("ProfileId: " + profileId);

        CompletableFuture<Response> futureResponse = CompletableFuture.supplyAsync(() -> {
            try {
                AsyncHttpClient.BoundRequestBuilder requestBuilder = contextServerService
                        .initAsyncRequestBuilder(siteKey, asyncHttpClient, "/cxs/profiles/" + profileId, true, true, true);

                Response response = requestBuilder.execute().get(); // blocking for the response
                asyncHttpClient.closeAsynchronously();

                return response;
            } catch (Exception e) {
                logger.error("Error during async request execution", e);
                return null;
            }
        });

        try {
            // Process the response when it's available
            Response response = futureResponse.get(); // Blocking call
            if (response != null && response.getStatusCode() == 200) {
                String responseBody = response.getResponseBody();
                logger.info("Received response: " + responseBody);

                JSONObject responseBodyJson = new JSONObject(responseBody);
                JSONObject profileProperties = responseBodyJson.optJSONObject("properties");

                if (profileProperties != null) {
                    propertyValue = profileProperties.opt(propertyName);
                    logger.info("Retrieved property value for " + propertyName + ": " + propertyValue);

                    if (propertyValue instanceof JSONArray) {
                        JSONArray propertyJsonArray = (JSONArray) propertyValue;
                        logger.info("Property is a JSONArray: " + propertyJsonArray);
                        return propertyJsonArray.toString();
                    } else if (propertyValue instanceof JSONObject) {
                        JSONObject propertyObject = (JSONObject) propertyValue;
                        logger.info("Property is a JSONObject: " + propertyObject);
                        return propertyObject.toString();
                    } else if (propertyValue != null) {
                        logger.info("Property is a primitive type: " + propertyValue);
                        return propertyValue.toString();
                    } else {
                        logger.warn("Property value is null for " + propertyName);
                        return null;
                    }
                } else {
                    logger.warn("No properties found in profile for profileId: " + profileId);
                    return null;
                }
            } else {
                logger.error("Failed to fetch property value. Status Code: " + (response != null ? response.getStatusCode() : "null response"));
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error while processing the async response.", e);
            return null;
        }
    }

    public Event updateProfile(JSONObject dmpVal, String siteKey) throws JSONException {

        Item source = new CustomItem();
        source.setScope(siteKey);
        source.setItemId("wemProfile");
        source.setItemType("wemProfile");

        Event event =
                new Event("updateProperties", null, new Profile(), siteKey, source, null, new Date());

        Map<String, Object> map = createMap(dmpVal);

        event.setProperty("update", map);
        event.setProperty("targetType", Profile.ITEM_TYPE);

        return event;
    }

    private Map<String, Object> createMap(JSONObject dmpVal) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = dmpVal.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            map.put("properties." + key, dmpVal.get(key));
        }
        return map;
    }
}
