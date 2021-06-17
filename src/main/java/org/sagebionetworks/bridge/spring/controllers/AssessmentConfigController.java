package org.sagebionetworks.bridge.spring.controllers;

import static org.sagebionetworks.bridge.BridgeConstants.SHARED_ASSESSMENTS_ERROR;
import static org.sagebionetworks.bridge.BridgeConstants.SHARED_APP_ID;
import static org.sagebionetworks.bridge.BridgeConstants.UPDATES_TYPEREF;
import static org.sagebionetworks.bridge.Roles.DEVELOPER;
import static org.sagebionetworks.bridge.Roles.STUDY_DESIGNER;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import org.sagebionetworks.bridge.exceptions.UnauthorizedException;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.sagebionetworks.bridge.models.assessments.config.AssessmentConfig;
import org.sagebionetworks.bridge.services.AssessmentConfigService;

@CrossOrigin
@RestController
public class AssessmentConfigController extends BaseController {
    
    private AssessmentConfigService service;
    
    @Autowired
    final void setAssessmentConfigService(AssessmentConfigService service) {
        this.service = service;
    }
    
    @GetMapping("/v1/assessments/{guid}/config")
    public AssessmentConfig getAssessmentConfig(@PathVariable String guid) {
        UserSession session = getAuthenticatedSession();
        String appId = session.getAppId();
        String ownerId = session.getParticipant().getOrgMembership();
        
        return service.getAssessmentConfig(appId, ownerId, guid);
    }
    
    @PostMapping("/v1/assessments/{guid}/config")
    public AssessmentConfig updateAssessmentConfig(@PathVariable String guid) {
        UserSession session = getAuthenticatedSession(DEVELOPER);
        String appId = session.getAppId();
        String ownerId = session.getParticipant().getOrgMembership();
        
        if (SHARED_APP_ID.equals(appId)) {
            throw new UnauthorizedException(SHARED_ASSESSMENTS_ERROR);
        }
        AssessmentConfig config = parseJson(AssessmentConfig.class);
        
        return service.updateAssessmentConfig(appId, ownerId, guid, config);
    }
    
    @PostMapping("/v1/assessments/{guid}/config/customize")
    public AssessmentConfig customizeAssessmentConfig(@PathVariable String guid) throws Exception {
        UserSession session = getAuthenticatedSession(DEVELOPER, STUDY_DESIGNER);
        String appId = session.getAppId();
        String ownerId = session.getParticipant().getOrgMembership();
        
        if (SHARED_APP_ID.equals(appId)) {
            throw new UnauthorizedException(SHARED_ASSESSMENTS_ERROR);
        }
        Map<String, Map<String, JsonNode>> updates = parseJson(UPDATES_TYPEREF);
        return service.customizeAssessmentConfig(appId, ownerId, guid, updates);
    }
    
}
