package org.sagebionetworks.bridge.models.organizations;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.DateTime;

import org.sagebionetworks.bridge.models.BridgeEntity;

@JsonDeserialize(as=HibernateOrganization.class)
public interface Organization extends BridgeEntity {
    
    static Organization create() {
        return new HibernateOrganization();
    }
    
    String getAppId();
    void setAppId(String appId);
    
    String getIdentifier();
    void setIdentifier(String identifier);
    
    String getName();
    void setName(String name);
    
    String getDescription();
    void setDescription(String description);
    
    String getSynapseProjectId();
    void setSynapseProjectId(String synapseProjectId);

    String getSynapseDataAccessTeamId();
    void setSynapseDataAccessTeamId(String synapseDataAccessTeamId);
    
    DateTime getCreatedOn();
    void setCreatedOn(DateTime createdOn);
    
    DateTime getModifiedOn();
    void setModifiedOn(DateTime modifiedOn);
    
    Long getVersion();
    void setVersion(Long version);
}
