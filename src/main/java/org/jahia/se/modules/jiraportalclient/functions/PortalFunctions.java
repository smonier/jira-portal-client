
package org.jahia.se.modules.jiraportalclient.functions;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.jahia.modules.jexperience.admin.ContextServerService;
import org.jahia.services.content.decorator.JCRSiteNode;

import org.jahia.services.render.RenderContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.unomi.api.CustomItem;
import org.apache.unomi.api.Event;
import org.apache.unomi.api.Item;
import org.apache.unomi.api.Profile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class PortalFunctions {

    private static final Logger logger = LoggerFactory.getLogger(PortalFunctions.class);
    private ContextServerService contextServerService;
    private JCRSiteNode site;

    @Reference
    public void setContextServerService(ContextServerService contextServerService) {
        this.contextServerService = contextServerService;
    }

    public void setSite(JCRSiteNode site) {
        this.site = site;
    }

    public PortalFunctions(JCRSiteNode site, ContextServerService contextServerService) {
        if (site == null || contextServerService == null) {
            throw new IllegalArgumentException("Site or ContextServerService cannot be null");
        }
        this.site = site;
        this.contextServerService = contextServerService;
    }

    // Function to fetch profile properties
    public String getPropertyValue(String propertyName, RenderContext renderContext)
     {
         String profileId = contextServerService.getProfileId(renderContext.getRequest(), site.getSiteKey());
         if (profileId == null) {
             logger.error("Failed to retrieve ProfileId for siteKey: {}", site.getSiteKey());
             return null;
         }
         logger.info("ProfileId: {}", profileId);
         return (String) Objects.requireNonNull(getProfile(profileId)).getProperty(propertyName);
     }

    private Profile getProfile(String profileId) {
        try {

            return contextServerService.executeGetRequest(
                    site.getSiteKey(),
                    "/cxs/profiles/"+profileId,
                    null,
                    null,
                    Profile.class
            );

        }catch (IOException e) {
            logger.error("Error happened", e);
        }
        return null;
    };


    // Function to update profile
    public Event updateProfile(JSONObject dmpVal, String siteKey) throws JSONException {
        Item source = new CustomItem();
        source.setScope(siteKey);
        source.setItemId("wemProfile");
        source.setItemType("wemProfile");

        Event event = new Event("updateProperties", null, new Profile(), siteKey, source, null, new Date());
        event.setProperty("update", createMap(dmpVal));
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