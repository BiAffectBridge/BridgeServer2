package org.sagebionetworks.bridge.spring.controllers;

import static org.sagebionetworks.bridge.TestConstants.TEST_APP_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.bridge.TestConstants.GUID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.sagebionetworks.bridge.exceptions.BadRequestException;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.sagebionetworks.bridge.models.studies.DemographicUser;
import org.sagebionetworks.bridge.services.AccountService;
import org.sagebionetworks.bridge.services.DemographicService;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import static org.sagebionetworks.bridge.TestUtils.assertCreate;
import static org.sagebionetworks.bridge.TestUtils.assertCrossOrigin;
import static org.sagebionetworks.bridge.TestUtils.assertDelete;
import static org.sagebionetworks.bridge.TestUtils.assertGet;
import static org.sagebionetworks.bridge.TestUtils.assertPost;
import static org.sagebionetworks.bridge.TestUtils.mockRequestBody;
import org.sagebionetworks.bridge.models.accounts.Account;

public class DemographicControllerTest {
    @Spy
    @InjectMocks
    DemographicController controller = new DemographicController();

    @Mock
    DemographicService demographicService;

    @Mock
    AccountService accountService;

    @Mock
    HttpServletRequest mockRequest;

    @Mock
    HttpServletResponse mockResponse;

    UserSession session;

    @BeforeMethod
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);

        session = new UserSession();
        session.setAppId(TEST_APP_ID);
        doReturn(session).when(controller).getAdministrativeSession();
        doReturn(mockRequest).when(controller).request();
        doReturn(mockResponse).when(controller).response();
    }

    @Test
    public void verifyAnnotations() throws Exception {
        assertCrossOrigin(DemographicController.class);
        assertPost(DemographicController.class, "saveDemographicUser");
        assertDelete(DemographicController.class, "deleteDemographic");
        assertDelete(DemographicController.class, "deleteDemographicUser");
        assertGet(DemographicController.class, "getDemographicUser");
        assertGet(DemographicController.class, "getDemographicUsers");
    }

    @Test
    public void saveDemographicUser() throws MismatchedInputException {
        DemographicUser demographicUser = new DemographicUser();
        doReturn(demographicUser).when(controller).parseJson(DemographicUser.class);
        doNothing().when(controller).checkAccountExistsInStudy(any(), any(), any());

        controller.saveDemographicUser(Optional.of("study1"), "user1");

        verify(controller).parseJson(DemographicUser.class);
        verify(demographicService).saveDemographicUser(demographicUser);
        assertEquals(demographicUser.getAppId(), TEST_APP_ID);
        assertEquals(demographicUser.getStudyId(), "study1");
        assertEquals(demographicUser.getUserId(), "user1");
    }

    @Test
    public void saveDemographicUserNullStudy() throws MismatchedInputException {
        DemographicUser demographicUser = new DemographicUser();
        doReturn(demographicUser).when(controller).parseJson(DemographicUser.class);
        doNothing().when(controller).checkAccountExistsInStudy(any(), any(), any());

        controller.saveDemographicUser(Optional.empty(), "user1");

        verify(controller).parseJson(DemographicUser.class);
        verify(demographicService).saveDemographicUser(demographicUser);
        assertEquals(demographicUser.getAppId(), TEST_APP_ID);
        assertEquals(demographicUser.getStudyId(), null);
        assertEquals(demographicUser.getUserId(), "user1");
    }

    @Test
    public void saveDemographicUserNull() throws MismatchedInputException {
        doReturn(null).when(controller).parseJson(DemographicUser.class);
        doNothing().when(controller).checkAccountExistsInStudy(any(), any(), any());

        try {
            controller.saveDemographicUser(Optional.of("study1"), "user1");
            fail("should have an exception");
        } catch (BadRequestException e) {
        }

        // can't use expectedExceptions because we need to test this after
        verify(controller).parseJson(DemographicUser.class);
    }

    @Test(expectedExceptions = { MismatchedInputException.class })
    public void saveDemographicUserInvalid() throws MismatchedInputException {
        doAnswer((invocation) -> {
            throw MismatchedInputException.from(new JsonFactory().createParser("[]"), DemographicUser.class,
                    "bad json");
        }).when(controller).parseJson(DemographicUser.class);
        doNothing().when(controller).checkAccountExistsInStudy(any(), any(), any());

        controller.saveDemographicUser(Optional.empty(), "user1");
    }
}
