package org.sagebionetworks.bridge.hibernate;

import static java.lang.Boolean.TRUE;
import static org.sagebionetworks.bridge.models.studies.EnrollmentFilter.ENROLLED;
import static org.sagebionetworks.bridge.models.studies.EnrollmentFilter.WITHDRAWN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.fluent.Request;

import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.RequestContext;
import org.sagebionetworks.bridge.models.studies.EnrollmentFilter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * A helper class to manage construction of HQL strings.
 */
class QueryBuilder {
    
    private final List<String> phrases = new ArrayList<>();
    private final Map<String,Object> params = new HashMap<>();
    
    public void append(String phrase) {
        phrases.add(phrase);
    }
    public void append(String phrase, String key, Object value) {
        phrases.add(phrase);
        params.put(key, value);
    }
    public void append(String phrase, String key1, Object value1, String key2, Object value2) {
        phrases.add(phrase);
        params.put(key1, value1);
        params.put(key2, value2);
    }
    public void dataGroups(Set<String> dataGroups, String operator) {
        if (!BridgeUtils.isEmpty(dataGroups)) {
            int i = 0;
            List<String> clauses = new ArrayList<>();
            for (String oneDataGroup : dataGroups) {
                String varName = operator.replace(" ", "") + (++i);
                clauses.add(":"+varName+" "+operator+" elements(acct.dataGroups)");
                params.put(varName, oneDataGroup);
            }
            phrases.add("AND (" + Joiner.on(" AND ").join(clauses) + ")");
        }
    }
    public void adminOnly(Boolean isAdmin) {
        if (isAdmin != null) {
            if (TRUE.equals(isAdmin)) {
                phrases.add("AND size(acct.roles) > 0");
            } else {
                phrases.add("AND size(acct.roles) = 0");
            }
        }
    }
    public void enrolledInStudy(Set<String> callerStudies, String studyId) {
        if (studyId != null) {
            phrases.add("AND enrollment.studyId IN (:studies)");
            if (callerStudies.contains(studyId)) {
                params.put("studies", ImmutableSet.of(studyId));
            } else {
                // this effectively means no results will be returned.
                params.put("studies", ImmutableSet.of());
            }
        }
    }
    public void orgMembership(String orgMembership) {
        if (orgMembership != null) {
            if ("<none>".equals(orgMembership.toLowerCase())) {
                phrases.add("AND acct.orgMembership IS NULL");
            } else {
                append("AND acct.orgMembership = :orgId", "orgId", orgMembership);
            }
        }
    }
    public void enrollment(EnrollmentFilter filter) {
        if (filter != null) {
            if (filter == ENROLLED) {
                phrases.add("AND withdrawnOn IS NULL");
            } else if (filter == WITHDRAWN) {
                phrases.add("AND withdrawnOn IS NOT NULL");
            }
        }
    }
    
    public String getQuery() {
        return BridgeUtils.SPACE_JOINER.join(phrases);
    }
    public Map<String,Object> getParameters() {
        return params;
    }
}
