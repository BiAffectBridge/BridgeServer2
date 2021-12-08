package org.sagebionetworks.bridge.validators;

import static org.mockito.Mockito.when;
import static org.sagebionetworks.bridge.BridgeConstants.BRIDGE_EVENT_ID_ERROR;
import static org.sagebionetworks.bridge.BridgeConstants.BRIDGE_RELAXED_ID_ERROR;
import static org.sagebionetworks.bridge.TestConstants.EMAIL;
import static org.sagebionetworks.bridge.TestConstants.PHONE;
import static org.sagebionetworks.bridge.TestConstants.SCHEDULE_GUID;
import static org.sagebionetworks.bridge.TestConstants.TEST_APP_ID;
import static org.sagebionetworks.bridge.TestUtils.assertValidatorMessage;
import static org.sagebionetworks.bridge.validators.ValidatorUtilsTest.generateStringOfLength;
import static org.sagebionetworks.bridge.validators.ValidatorUtilsTest.getExcessivelyLargeClientData;
import static org.sagebionetworks.bridge.validators.ValidatorUtilsTest.getInvalidStringLengthMessage;
import static org.sagebionetworks.bridge.models.activities.ActivityEventUpdateType.FUTURE_ONLY;
import static org.sagebionetworks.bridge.models.activities.ActivityEventUpdateType.MUTABLE;
import static org.sagebionetworks.bridge.models.studies.ContactRole.TECHNICAL_SUPPORT;
import static org.sagebionetworks.bridge.models.studies.IrbDecisionType.APPROVED;
import static org.sagebionetworks.bridge.models.studies.IrbDecisionType.EXEMPT;
import static org.sagebionetworks.bridge.models.studies.StudyPhase.DESIGN;
import static org.sagebionetworks.bridge.validators.StudyValidator.*;
import static org.sagebionetworks.bridge.validators.Validate.CANNOT_BE_BLANK;
import static org.sagebionetworks.bridge.validators.Validate.CANNOT_BE_NULL;
import static org.sagebionetworks.bridge.validators.Validate.INVALID_EMAIL_ERROR;
import static org.sagebionetworks.bridge.validators.Validate.INVALID_PHONE_ERROR;
import static org.sagebionetworks.bridge.validators.Validate.TIME_ZONE_ERROR;
import static org.sagebionetworks.bridge.validators.Validate.entityThrowingException;
import static org.sagebionetworks.bridge.validators.ValidatorUtils.TEXT_SIZE;

import com.google.common.collect.ImmutableList;

import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.bridge.models.schedules2.Schedule2;
import org.sagebionetworks.bridge.models.schedules2.Session;
import org.sagebionetworks.bridge.models.studies.Address;
import org.sagebionetworks.bridge.services.Schedule2Service;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.sagebionetworks.bridge.models.accounts.Phone;
import org.sagebionetworks.bridge.models.studies.Contact;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.studies.StudyCustomEvent;

public class StudyValidatorTest {
    
    private static final LocalDate DECISION_ON = DateTime.now().toLocalDate();
    private static final LocalDate EXPIRES_ON = DateTime.now().plusDays(10).toLocalDate();
    
    private Study study;
    
    private StudyValidator validator;
    
    @Mock
    private Schedule2Service schedule2Service;
    
    @BeforeMethod
    public void before() {
        MockitoAnnotations.initMocks(this);
        
        validator = new StudyValidator(schedule2Service);
    }
    
    @Test
    public void valid() {
        study = createStudy();
        entityThrowingException(validator, study);
    }
    
    @Test
    public void idIsRequired() {
        study = createStudy();
        study.setIdentifier(null);
        assertValidatorMessage(validator, study, IDENTIFIER_FIELD, CANNOT_BE_BLANK);
    }
    
    @Test
    public void invalidIdentifier() {
        study = createStudy();
        study.setIdentifier("id not valid");
        
        assertValidatorMessage(validator, study, IDENTIFIER_FIELD, BRIDGE_EVENT_ID_ERROR);
    }

    @Test
    public void nameIsRequired() {
        study = createStudy();
        study.setName(null);
        assertValidatorMessage(validator, study, NAME_FIELD, CANNOT_BE_BLANK);
    }
    
    @Test
    public void phaseRequired() {
        study = createStudy();
        study.setPhase(null);
        assertValidatorMessage(validator, study, PHASE_FIELD, CANNOT_BE_NULL);
    }

    @Test
    public void appIdIsRequired() {
        study = createStudy();
        study.setAppId(null);
        assertValidatorMessage(validator, study, APP_ID_FIELD, CANNOT_BE_BLANK);
    }

    @Test
    public void studyTimeZoneNullOK() {
        study = createStudy();
        study.setStudyTimeZone(null);
        entityThrowingException(validator, study);
    }

    @Test
    public void studyTimeZoneInvalid() {
        study = createStudy();
        study.setStudyTimeZone("America/Aspen");
        assertValidatorMessage(validator, study, STUDY_TIME_ZONE_FIELD, TIME_ZONE_ERROR);
    }
    
    @Test
    public void adherenceThresholdPercentageNullOK() {
        study = createStudy();
        study.setAdherenceThresholdPercentage(null);
        entityThrowingException(validator, study);
    }
    
    @Test
    public void adherenceThresholdPercentageLessThanZero() {
        study = createStudy();
        study.setAdherenceThresholdPercentage(-1);
        assertValidatorMessage(validator, study, ADHERENCE_THRESHOLD_PERCENTAGE_FIELD, "must be from 0-100%");
    }
    
    @Test
    public void adherenceThresholdPercentageMoreThan100() {
        study = createStudy();
        study.setAdherenceThresholdPercentage(101);
        assertValidatorMessage(validator, study, ADHERENCE_THRESHOLD_PERCENTAGE_FIELD, "must be from 0-100%");
    }

    @Test
    public void contactNameNull() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setName(null);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + NAME_FIELD, CANNOT_BE_BLANK);
    }
    
    @Test
    public void contactNameBlank() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setName("");
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + NAME_FIELD, CANNOT_BE_BLANK);
    }
    
    @Test
    public void contactRoleNull() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setRole(null);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + ROLE_FIELD, CANNOT_BE_NULL);
    }
    
    @Test
    public void contactInvalidEmail() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setEmail("junk");
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + EMAIL_FIELD, INVALID_EMAIL_ERROR);
    }
    
    @Test
    public void contactInvalidPhone() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setPhone(new Phone("333333", "Portual"));
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + PHONE_FIELD, INVALID_PHONE_ERROR);
    }
    
    @Test
    public void irbDecisionOnRequired() {
        study = createStudy();
        study.setIrbDecisionType(APPROVED);
        study.setIrbExpiresOn(EXPIRES_ON);
        
        assertValidatorMessage(validator, study, IRB_DECISION_ON_FIELD, CANNOT_BE_NULL);
    }
    
    @Test
    public void irbDecisionTypeRequired() {
        study = createStudy();
        study.setIrbDecisionOn(DECISION_ON);
        study.setIrbExpiresOn(EXPIRES_ON);
        
        assertValidatorMessage(validator, study, IRB_DECISION_TYPE_FIELD, CANNOT_BE_NULL);
    }
    
    @Test
    public void irbExpiresOnRequired() {
        study = createStudy();
        study.setIrbDecisionOn(DECISION_ON);
        study.setIrbDecisionType(APPROVED);
        
        assertValidatorMessage(validator, study, IRB_EXPIRES_ON_FIELD, CANNOT_BE_NULL);
    }
    
    @Test
    public void irbExemptionDoesNotRequireExpiration() {
        study = createStudy();
        study.setIrbDecisionType(EXEMPT);
        study.setIrbDecisionOn(DECISION_ON);
        study.setIrbExpiresOn(null);
        entityThrowingException(validator, study);
    }
    
    @Test
    public void nullContactsOK() {
        study = createStudy();
        study.setContacts(null);
        
        entityThrowingException(validator, study);
    }
    
    @Test
    public void nullContactEmailOK() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setEmail(null);
        study.setContacts(ImmutableList.of(c1));
        
        entityThrowingException(validator, study);
    }

    @Test
    public void nullContactPhoneOK() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setPhone(null);
        study.setContacts(ImmutableList.of(c1));
        
        entityThrowingException(validator, study);
    }
    
    @Test
    public void customEvents_eventIdBank() {
        StudyCustomEvent event = new StudyCustomEvent("", MUTABLE);
        study = createStudy();
        study.getCustomEvents().add(event);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD + "[0].eventId", CANNOT_BE_BLANK);
    }
    
    @Test
    public void customEvents_eventIdNull() {
        StudyCustomEvent event = new StudyCustomEvent(null, MUTABLE);
        study = createStudy();
        study.getCustomEvents().add(event);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD + "[0].eventId", CANNOT_BE_BLANK);
    }
    
    @Test
    public void customEvents_eventIdInvalid() {
        StudyCustomEvent event = new StudyCustomEvent("a:b:c", MUTABLE);
        study = createStudy();
        study.getCustomEvents().add(event);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD + "[0].eventId", BRIDGE_RELAXED_ID_ERROR);
    }
    
    @Test
    public void customEvents_updateTypeNull() {
        StudyCustomEvent event = new StudyCustomEvent("event", null);
        study = createStudy();
        study.getCustomEvents().add(event);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD + "[0].updateType", CANNOT_BE_NULL);
    }
    
    @Test
    public void customEvents_entryNull() {
        study = createStudy();
        study.getCustomEvents().add(null);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD + "[0]", CANNOT_BE_NULL);
    }
    
    @Test
    public void customEvents_duplicated() {
        StudyCustomEvent event = new StudyCustomEvent("event", FUTURE_ONLY);
        study = createStudy();
        study.getCustomEvents().add(event);
        study.getCustomEvents().add(event);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD, "cannot contain duplicate event IDs");
    }
    
    @Test
    public void customEvents_missingEventIdInSchedule() {
        study = createStudy();
        study.setScheduleGuid(SCHEDULE_GUID);
    
        Schedule2 schedule = new Schedule2();
        Session session = new Session();
        session.setStartEventIds(ImmutableList.of("custom-aaa","bbb","custom-ccc"));
        schedule.setSessions(ImmutableList.of(session));
        
        StudyCustomEvent studyCustomEvent = new StudyCustomEvent();
        studyCustomEvent.setEventId("custom-aaa");
        
        study.setCustomEvents(ImmutableList.of(studyCustomEvent));
        
        when(schedule2Service.getSchedule(study.getAppId(), SCHEDULE_GUID)).thenReturn(schedule);
        
        assertValidatorMessage(validator, study, CUSTOM_EVENTS_FIELD, "cannot remove custom events currently used in a schedule");
    }
    
    @Test
    public void contactWithAddressOK() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        entityThrowingException(validator, study);
    }
    
    @Test
    public void stringLengthValidation_identifier() {
        study = createStudy();
        study.setIdentifier(generateStringOfLength(256));
        assertValidatorMessage(validator, study, IDENTIFIER_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_name() {
        study = createStudy();
        study.setName(generateStringOfLength(256));
        assertValidatorMessage(validator, study, NAME_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_details() {
        study = createStudy();
        study.setDetails(generateStringOfLength(511));
        assertValidatorMessage(validator, study, DETAILS_FIELD, getInvalidStringLengthMessage(510));
    }
    
    @Test
    public void stringLengthValidation_studyLogoUrl() {
        study = createStudy();
        study.setStudyLogoUrl(generateStringOfLength(256));
        assertValidatorMessage(validator, study, STUDY_LOGO_URL_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_institutionId() {
        study = createStudy();
        study.setInstitutionId(generateStringOfLength(256));
        assertValidatorMessage(validator, study, INSTITUTION_ID_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_irbProtocolId() {
        study = createStudy();
        study.setIrbProtocolId(generateStringOfLength(256));
        assertValidatorMessage(validator, study, IRB_PROTOCOL_ID_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_irbName() {
        study = createStudy();
        study.setIrbName(generateStringOfLength(61));
        assertValidatorMessage(validator, study, IRB_NAME_FIELD, getInvalidStringLengthMessage(60));
    }
    
    @Test
    public void stringLengthValidation_irbProtocolName() {
        study = createStudy();
        study.setIrbProtocolName(generateStringOfLength(513));
        assertValidatorMessage(validator, study, IRB_PROTOCOL_NAME_FIELD, getInvalidStringLengthMessage(512));
    }
    
    @Test
    public void stringLengthValidation_keywords() {
        study = createStudy();
        study.setKeywords(generateStringOfLength(256));
        assertValidatorMessage(validator, study, KEYWORDS_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactName() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setName(generateStringOfLength(256));
        study.setContacts(ImmutableList.of(c1));
    
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + NAME_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactPosition() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setPosition(generateStringOfLength(256));
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + POSITION_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactAffiliation() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setAffiliation(generateStringOfLength(256));
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + AFFILIATION_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactJurisdiction() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setJurisdiction(generateStringOfLength(256));
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + JURISDICTION_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactEmail() {
        study = createStudy();
        Contact c1 = createContact();
        c1.setEmail(generateStringOfLength(256));
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study, CONTACTS_FIELD + "[0]." + EMAIL_FIELD, getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactPlaceName() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setPlaceName(generateStringOfLength(256));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));

        assertValidatorMessage(validator, study, 
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + PLACE_NAME_FIELD, 
                getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactStreet() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setStreet(generateStringOfLength(256));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study,
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + STREET_FIELD,
                getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactDivision() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setDivision(generateStringOfLength(256));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study,
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + DIVISION_FIELD,
                getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactMailRouting() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setMailRouting(generateStringOfLength(256));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study,
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + MAIL_ROUTING_FIELD,
                getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactCity() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setCity(generateStringOfLength(256));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study,
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + CITY_FIELD,
                getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_contactPostalCode() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setPostalCode(generateStringOfLength(51));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study,
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + POSTAL_CODE_FIELD,
                getInvalidStringLengthMessage(50));
    }
    
    @Test
    public void stringLengthValidation_contactCountry() {
        study = createStudy();
        Contact c1 = createContact();
        Address a1 = createAddress();
        a1.setCountry(generateStringOfLength(256));
        c1.setAddress(a1);
        study.setContacts(ImmutableList.of(c1));
        
        assertValidatorMessage(validator, study,
                CONTACTS_FIELD + "[0]." + ADDRESS_FIELD + "." + COUNTRY_FIELD,
                getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_studyDesignType() {
        study = createStudy();
        String designType = generateStringOfLength(256);
        study.setStudyDesignTypes(ImmutableSet.of(designType));
        assertValidatorMessage(validator, study, "studyDesignTypes["+designType+"]", getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void stringLengthValidation_disease() {
        study = createStudy();
        String disease = generateStringOfLength(256);
        study.setDiseases(ImmutableSet.of(disease));
        assertValidatorMessage(validator, study, "diseases["+disease+"]", getInvalidStringLengthMessage(255));
    }
    
    @Test
    public void jsonLengthValidation_clientData() {
        study = createStudy();
        study.setClientData(getExcessivelyLargeClientData());
        assertValidatorMessage(validator, study, "clientData", getInvalidStringLengthMessage(TEXT_SIZE));
    }
    
    private Study createStudy() {
        Study study = Study.create();
        study.setIdentifier("id");
        study.setAppId(TEST_APP_ID);
        study.setName("name");
        study.setPhase(DESIGN);
        study.setStudyTimeZone("America/Los_Angeles");
        study.setAdherenceThresholdPercentage(80);
        return study;
    }
    
    private Contact createContact() {
        Contact contact = new Contact();
        contact.setName("Tim Powers");
        contact.setRole(TECHNICAL_SUPPORT);
        contact.setEmail(EMAIL);
        contact.setPhone(PHONE);
        return contact;
    }
    
    private Address createAddress() {
        Address address = new Address();
        address.setPlaceName("place name");
        address.setStreet("street");
        address.setDivision("division");
        address.setMailRouting("mail routing");
        address.setCity("city");
        address.setPostalCode("postal code");
        address.setCountry("country");
        return address;
    }
    
}
