/*
package org.jahia.se.modules.jiraportalclient.functions;


import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
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

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class PortalFunctions {

    private static Logger logger = LoggerFactory.getLogger(PortalFunctions.class);


    public String getPropertyValue(String propertyName, RenderContext renderContext, ContextServerService contextServerService) throws RepositoryException, ExecutionException, InterruptedException, JSONException, IOException {

        Object propertyValue = null;
        logger.info("Getting Property Value for: " + propertyName);
        HttpServletRequest httpServletRequest = renderContext.getRequest();
        //   logger.info("httpServletRequest: "+httpServletRequest.getRequestedSessionId());

        String siteKey = renderContext.getSite().getSiteKey();
        logger.info("siteKey: " + siteKey);


        final AsyncHttpClient asyncHttpClient = contextServerService
                .initAsyncHttpClient(siteKey);
        String profileId = contextServerService.getProfileId(httpServletRequest, siteKey);
        logger.info("profileId: " + profileId);
        if (asyncHttpClient != null) {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = contextServerService
                    .initAsyncRequestBuilder(siteKey, asyncHttpClient, "/cxs/profiles/" + profileId,
                            true, true, true);
            ListenableFuture<Response> future = requestBuilder.execute(new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) {
                    asyncHttpClient.closeAsynchronously();
                    return response;
                }
            });
            JSONObject responseBody = new JSONObject(future.get().getResponseBody());
            //logger.info("responseBody : "+responseBody.toString());
            JSONObject profileProperties = responseBody.optJSONObject("properties");
            try {
                propertyValue = profileProperties.getString(propertyName);
                if (propertyValue instanceof JSONArray) {
                    // It's an array
                    JSONArray propertyJsonArray = (JSONArray) propertyValue;
                    return propertyJsonArray.toString();
                } else if (propertyValue instanceof JSONObject) {
                    // It's an object
                    JSONObject propertyObject = (JSONObject) propertyValue;
                    return propertyObject.toString();
                } else {
                    return propertyValue.toString();
                }
            } catch (JSONException e) {
                logger.error("Error Property Value");
                e.getMessage();
                return null;
            }
        } else {
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
}*/
