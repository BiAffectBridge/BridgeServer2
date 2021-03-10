package org.sagebionetworks.bridge.hibernate;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import com.amazonaws.util.Throwables;
import com.google.common.collect.ImmutableMap;

import org.hibernate.NonUniqueObjectException;
import org.springframework.stereotype.Component;

import org.sagebionetworks.bridge.BridgeUtils;
import org.sagebionetworks.bridge.exceptions.ConcurrentModificationException;
import org.sagebionetworks.bridge.exceptions.ConstraintViolationException;

/**
 * Maintaining an exception converter for each Hibernate model is difficult to maintain 
 * because a new table with new foreign-key constraints needs to update the exception handling 
 * for all foreign key models. There is a method to the madness of foreign key constraint 
 * exceptions, such that we can write one converter that will work for all Hibernate + MySQL 
 * persisted models. This is the start of that effort and I will replace other persistence
 * converters over time.
 * 
 * Note that exceptions from the execution of raw SQL queries vs. HQL queries are reported 
 * differently.
 */
@Component
public class MySQLHibernatePersistenceExceptionConverter implements PersistenceExceptionConverter {

    static final String FK_CONSTRAINT_MSG = "This %s cannot be deleted or updated because it is referenced by %s.";
    static final String UNIQUE_CONSTRAINT_MSG = "Cannot update this %s because it has duplicate %s";
    static final String NON_UNIQUE_MSG = "Another %s has already used a value which must be unique: %s";
    
    // All MySQL constraint violations take this format:
    //      javax.persistence.PersistenceException which wraps
    //          org.hibernate.exception.ConstraintViolationException which wraps
    //              java.sql.SQLIntegrityConstraintViolationException
    // And the SQL-specific error message indicates something about the type of constraint and thus, the 
    // appropriate error message. We look for the following keys:
    
    /**
     * Foreign key constraints. The name of the constraint is used to inform the caller about what object is 
     * using the target entity. Constraints that cascade deletes do not need to be added to this map (updates
     * seem to always be handled by Hibernate when they are needed).
     */
    private static final Map<String,String> FOREIGN_KEY_CONSTRAINTS = new ImmutableMap.Builder<String,String>()
            .put("AssessmentRef-Assessment-Constraint", "a scheduling session")
            .put("Schedule-Organization-Constraint", "a schedule")
            .build();
    
    /**
     * Unique key constraints. The name of the constraint is used to inform the caller about the fields that are 
     * being duplicated.
     */
    private static final Map<String,String> UNIQUE_KEY_CONSTRAINTS = new ImmutableMap.Builder<String,String>()
            .put("PRIMARY", "primary keys")
            .put("TimeWindow-guid-sessionGuid-idx", "time window GUIDs")
            .put("Session-guid-scheduleGuid-idx", "session GUIDs")
            .build();

    @Override
    public RuntimeException convert(PersistenceException exception, Object entity) {
        String name = BridgeUtils.getTypeName(entity.getClass());
        
        if (exception instanceof OptimisticLockException) {
            return new ConcurrentModificationException( name + " has the wrong version number; it may have been saved in the background.");
        }
        if (exception instanceof NonUniqueObjectException) {
            Serializable identifier = ((NonUniqueObjectException)exception).getIdentifier();
            return new ConstraintViolationException.Builder()
                    .withMessage(String.format(NON_UNIQUE_MSG, name.toLowerCase(), identifier)).build();
        }

        Throwable throwable = Throwables.getRootCause(exception);
        if (throwable instanceof java.sql.SQLIntegrityConstraintViolationException) {
            // SQLIntegrityConstraintViolation can contain different constraint violations, 
            // including foreign key and duplicate entry violations
            String rawMessage = throwable.getMessage();
            String displayMessage = "Cannot update or delete this item because it is in use.";
            
            if (rawMessage.contains("Duplicate entry")) {
                displayMessage = selectMsg(displayMessage, rawMessage, UNIQUE_KEY_CONSTRAINTS, UNIQUE_CONSTRAINT_MSG, name);
            } else if (rawMessage.contains("a foreign key constraint fails")) {
                displayMessage = selectMsg(displayMessage, rawMessage, FOREIGN_KEY_CONSTRAINTS, FK_CONSTRAINT_MSG, name);
            }
            return new ConstraintViolationException.Builder().withMessage(displayMessage).build();
        }
        return exception;
    }
    
    private String selectMsg(String displayMessage, String rawMessage, Map<String,String> constraintNames, String message, String name) {
        for (Map.Entry<String, String> entry : constraintNames.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (rawMessage.contains(key)) {
                return String.format(message, name.toLowerCase(), value); 
            }
        }
        return displayMessage;
    }
    
}
