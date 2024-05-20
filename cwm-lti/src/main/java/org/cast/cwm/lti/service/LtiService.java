/*
 * Copyright 2011-2019 CAST, Inc.
 *
 * This file is part of the CAST Wicket Modules:
 * see <http://code.google.com/p/cast-wicket-modules>.
 *
 * The CAST Wicket Modules are free software: you can redistribute and/or
 * modify them under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The CAST Wicket Modules are distributed in the hope that they will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cast.cwm.lti.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.cast.cwm.CwmSession;
import org.cast.cwm.data.*;
import org.cast.cwm.db.service.IDBService;
import org.cast.cwm.db.service.IModelProvider;
import org.cast.cwm.lti.ILtiResourceProvider;
import org.cast.cwm.lti.data.Score;
import org.cast.cwm.lti.util.LockByKey;
import org.cast.cwm.service.ICwmSessionService;
import org.cast.cwm.service.IEventService;
import org.cast.cwm.service.ISiteService;
import org.cast.cwm.service.IUserService;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
public class LtiService implements ILtiService {

    private static final String CLAIM_MESSAGE_TYPE = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    private static final String CLAIM_CUSTOM = "https://purl.imsglobal.org/spec/lti/claim/custom";
    private static final String CLAIM_ENDPOINT = "https://purl.imsglobal.org/spec/lti-ags/claim/endpoint";
    public static final String CLAIM_DEPLOYMENT_ID = "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
    private static final String CLAIM_DEEP_LINKING_SETTINGS = "https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings";
    private static final String CLAIM_LINKING_CONTENT_ITEMS = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";
    private static final String CLAIM_ROLES = "https://purl.imsglobal.org/spec/lti/claim/roles";

    private static final String MESSAGE_TYPE_RESOURCE_REQUEST = "LtiResourceLinkRequest";
    private static final String MESSAGE_TYPE_LINKING_REQUEST = "LtiDeepLinkingRequest";
    private static final String MESSAGE_TYPE_LINKING_RESPONSE = "LtiDeepLinkingResponse";

    private static final String ROLE_LEARNER = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Learner";
    private static final String ROLE_INSTRUCTOR = "http://purl.imsglobal.org/vocab/lis/v2/institution/person#Instructor";
    private static final String ROLE_MEMBERSHIP_INSTRUCTOR = "http://purl.imsglobal.org/vocab/lis/v2/membership#Instructor";
    private static final String ROLE_MEMBERSHIP_LEARNER = "http://purl.imsglobal.org/vocab/lis/v2/membership#Learner";

    private static final String CLAIM_CONTEXT = "https://purl.imsglobal.org/spec/lti/claim/context";

    private static final float SCORE_MAXIMUM = 1.0f;
    private static final String SCORE_SUFFIX = "/scores";


    @Inject
    private ICwmSessionService sessionService;

    @Inject
    private IUserService userService;

    @Inject
    private ISiteService siteService;

    @Inject
    private IEventService eventService;

    @Inject
    private IDBService dbService;

    @Inject
    private IModelProvider modelProvider;

    @Inject
    private ILtiResourceProvider resourceProvider;

    private LockByKey locks = new LockByKey();

    @Override
    public String onLaunch(JsonObject payload) {

        String iss = payload.get("iss").getAsString();
        String aud = payload.get("aud").getAsString();
        String deploymentId = payload.get(CLAIM_DEPLOYMENT_ID).getAsString();

        LtiPlatform platform = siteService.getPlatformByIssuerClientIdDeploymentId(iss, aud, deploymentId).getObject();
        if (platform == null) {
            throw new IllegalArgumentException(String.format("unknown platform '%s', '%s', '%s'",
                    iss, aud, deploymentId));
        }

        Site site = platform.getSite();
        Period period = initPeriod(payload, site);
        User user = initUser(payload, site, period);
        dbService.flushChanges();
        login(user, period, site);

        String messageType = payload.get(CLAIM_MESSAGE_TYPE).getAsString();
        switch (messageType) {
            case MESSAGE_TYPE_RESOURCE_REQUEST:
                ResourceState resourceState = new ResourceState();
                resourceState.iss = iss;
                resourceState.aud = aud;
                resourceState.deploymentId = deploymentId;
                CwmSession.get().setMetaData(ResourceState.ATTRIBUTE, resourceState);

                JsonObject endpoint = payload.getAsJsonObject(CLAIM_ENDPOINT);
                if (endpoint != null) {
                    JsonElement lineItem = endpoint.get("lineitem");
                    if (lineItem != null) {
                        resourceState.lineItemUrl = lineItem.getAsString();
                    }
                }

                JsonObject custom = payload.getAsJsonObject(CLAIM_CUSTOM);
                return resourceProvider.onResourceRequested(custom);
            case MESSAGE_TYPE_LINKING_REQUEST:
                JsonObject deepLinkingSettings = payload.getAsJsonObject(CLAIM_DEEP_LINKING_SETTINGS);
                DeepLinkingState deepLinkingState = new DeepLinkingState();
                deepLinkingState.iss = iss;
                deepLinkingState.aud = aud;
                deepLinkingState.deploymentId = deploymentId;
                deepLinkingState.returnUrl = deepLinkingSettings.get("deep_link_return_url").getAsString();
                if (deepLinkingSettings.get("data") != null) {
                    deepLinkingState.data = deepLinkingSettings.get("data").getAsString();
                }
                CwmSession.get().setMetaData(DeepLinkingState.ATTRIBUTE, deepLinkingState);
                return resourceProvider.onDeepLinkingRequested();
        }
        throw new IllegalArgumentException(String.format("unrecognized message type '%s'", messageType));
    }

    private void login(User user, Period period, Site site) {
        CwmSession session = CwmSession.get();

        User currentUser = session.getUser();
        if (currentUser == null || !currentUser.getId().equals(user.getId())) {
            session.signOut();
            session.bind();
            session.signIn(user, true);
            eventService.createLoginSession(null);

            session.setCurrentPeriodModel(modelProvider.modelOf(period));
            session.setCurrentSiteModel(modelProvider.modelOf(site));
        }
    }

    private Period initPeriod(JsonObject payload, Site site) {

        JsonObject context = payload.getAsJsonObject(CLAIM_CONTEXT);
        String ltiId = getLtiId(context.get("id"));

        return locks.locked(ltiId, () -> {
            Period period = siteService.getPeriodBySiteAndLtiId(site, ltiId).getObject();
            if (period == null) {
                period = siteService.newPeriod();
                period.setLtiId(ltiId);
                period.setClassId("lti-" + ltiId);
                period.setSite(site);
            }
            String name = StringUtils.substring(
                    Strings.join(" - ",
                        context.get("label").getAsString(),
                        context.get("title").getAsString(),
                        "(" + ltiId + ")"),
                    0, 255);
            period.setName(name);
            dbService.save(period);
            return period;
        });
    }

    private User initUser(JsonObject payload, Site site, Period period) {
        String ltiId = getLtiId(payload.get("sub"));

        return locks.locked(ltiId, () -> {
            User user = userService.getBySiteAndLtiId(site, ltiId).getObject();
            if (user == null) {
                user = userService.newUser();
                user.setCreateDate(new Date());
                user.setValid(true);
                user.setPassword(UUID.randomUUID().toString());
                user.setUsername(UUID.randomUUID().toString());
                user.setSubjectId(UUID.randomUUID().toString());
                user.setLtiId(ltiId);
            }
            user.setFirstName(payload.get("given_name").getAsString());
            user.setLastName(payload.get("family_name").getAsString());
            user.setRole(mapRole(payload));
            if (!user.getPeriods().contains(period)) {
                user.getPeriods().add(period);
            }
            dbService.save(user);
            return user;
        });
    }

    private Role mapRole(JsonObject payload) {
        JsonArray roles = payload.getAsJsonArray(CLAIM_ROLES);
        if (roles.contains(new JsonPrimitive(ROLE_LEARNER)) || roles.contains(new JsonPrimitive(ROLE_MEMBERSHIP_LEARNER))) {
            return Role.STUDENT;
        }
        if (roles.contains(new JsonPrimitive(ROLE_INSTRUCTOR)) || roles.contains(new JsonPrimitive(ROLE_MEMBERSHIP_INSTRUCTOR))) {
            return Role.TEACHER;
        }
        return Role.GUEST;
    }

    /**
     * Get the  LTI id.
     *
     * @param id from LTI
     * @return id
     */
    private String getLtiId(JsonElement id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return id.getAsString();
    }

    @Override
    public <T> Request respondDeepLinking(String launchUrl, List<T> resources) {

        DeepLinkingState state = getState(DeepLinkingState.ATTRIBUTE);

        IModel<LtiPlatform> platform = siteService.getPlatformByIssuerClientIdDeploymentId(state.iss, state.aud, state.deploymentId);

        JsonObject payload = new JsonObject();
        payload.addProperty("iss", state.aud);
        payload.addProperty("aud", state.iss);
        payload.addProperty(CLAIM_DEPLOYMENT_ID, state.deploymentId);
        if (state.data != null) {
            payload.addProperty("https://purl.imsglobal.org/spec/lti-dl/claim/data", state.data);
        }
        payload.addProperty("https://purl.imsglobal.org/spec/lti/claim/version", "1.3.0");
        payload.addProperty("nonce", UUID.randomUUID().toString());

        // issued_at and expires are mandatory
        Instant now = Instant.now();
        payload.addProperty("iat", now.getEpochSecond());
        payload.addProperty("exp", now.plusSeconds(60*60).getEpochSecond());
        payload.addProperty(CLAIM_MESSAGE_TYPE, MESSAGE_TYPE_LINKING_RESPONSE);

        JsonArray contentItems = new JsonArray();
        payload.add(CLAIM_LINKING_CONTENT_ITEMS, contentItems);

        for (Object resource : resources) {
            JsonObject resourceLink = new JsonObject();
            resourceLink.addProperty("type", "ltiResourceLink");
            resourceLink.addProperty("url", launchUrl);
            JsonObject custom = new JsonObject();
            resourceLink.add("custom", custom);
            resourceProvider.configureDeepLinkResource(resource, resourceLink, custom);
            contentItems.add(resourceLink);

            if (resource != null) {
                // create a line item for each resource
                JsonObject lineItem = new JsonObject();
                lineItem.addProperty("scoreMaximum", SCORE_MAXIMUM);
                resourceLink.add("lineItem", lineItem);
            }
        }

        return new Request(platform.getObject(), state.returnUrl, payload);
    }

    public <R> Request giveScore(R resourceResponse) {

        ResourceState state = getState(ResourceState.ATTRIBUTE);
        if (state.lineItemUrl == null) {
            throw new IllegalStateException("no line item url present");
        }

        IModel<LtiPlatform> platform = siteService.getPlatformByIssuerClientIdDeploymentId(state.iss, state.aud, state.deploymentId);

        JsonObject payload = new JsonObject();
        payload.addProperty("timestamp", Instant.now().toString());

        User user = sessionService.getUser();
        if (user == null || user.getLtiId() == null) {
            throw new IllegalStateException("no lti userId");
        }
        payload.addProperty("userId", user.getLtiId());

        Score score = resourceProvider.getScore(resourceResponse);
        payload.addProperty("scoreMaximum", SCORE_MAXIMUM);
        payload.addProperty("scoreGiven", score.scoreGiven);
        payload.addProperty("comment", score.comment);
        payload.addProperty("activityProgress", score.activityProgress.name());
        payload.addProperty("gradingProgress", score.gradingProgress.name());

        return new Request(platform.getObject(), state.lineItemUrl + SCORE_SUFFIX, payload);
    }

    private <T extends Serializable> T getState(MetaDataKey<T> key) {
        T state = CwmSession.get().getMetaData(key);
        if (state == null) {
            throw new IllegalStateException("no state found");
        }
        return state;
    }

    private static class DeepLinkingState implements Serializable {
        private static final MetaDataKey<DeepLinkingState> ATTRIBUTE = new MetaDataKey<DeepLinkingState>() {
        };

        String iss;
        String aud;
        String deploymentId;
        String data;
        String returnUrl;
    }

    private static class ResourceState implements Serializable {
        private static final MetaDataKey<ResourceState> ATTRIBUTE = new MetaDataKey<ResourceState>() {
        };

        String iss;
        String aud;
        public String deploymentId;
        String lineItemUrl;
    }
}
