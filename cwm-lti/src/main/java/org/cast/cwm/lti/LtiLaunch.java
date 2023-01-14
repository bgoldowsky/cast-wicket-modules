package org.cast.cwm.lti;

import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebResponse;
import org.cast.cwm.lti.service.IJwtValidationService;
import org.cast.cwm.lti.service.ILtiService;

import javax.inject.Inject;

/**
 * Handler for LTI launches.
 */
@Slf4j
public class LtiLaunch implements IRequestHandler {

    @Inject
    private IJwtValidationService validationService;

    @Inject
    private ILtiService ltiService;

    private final String idToken;
    private final String state;

    LtiLaunch() {
        Injector.get().inject(this);

        IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

        idToken = params.getParameterValue("id_token").toString();
        state = params.getParameterValue("state").toString();

        Injector.get().inject(this);
    }

    @Override
    public void respond(IRequestCycle requestCycle) {
        WebResponse response = (WebResponse)requestCycle.getResponse();

        IJwtValidationService.Result result;
        try {
            result = validationService.validate(idToken);
        } catch (Exception e) {
            log.info("invalid token {}", e.getMessage());
            response.sendError(401, "invalid token");
            return;
        }

        if (log.isInfoEnabled()) {
            log.info(new GsonBuilder().setPrettyPrinting().create().toJson(result.payload));
        }

        LtiInitiation.checkNonce(result.payload.get("nonce").getAsString());

        String redirect;
        try {
            redirect = ltiService.onLaunch(result.platform, result.payload);
        } catch (Exception e) {
            log.info("invalid payload {}", e.getMessage(), e);
            response.sendError(400, "invalid payload");
            return;
        }

        response.sendRedirect(redirect);
    }
}