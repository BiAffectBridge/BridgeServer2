package org.sagebionetworks.bridge.spring.controllers;

import static org.sagebionetworks.bridge.BridgeConstants.API_DEFAULT_PAGE_SIZE;
import static org.sagebionetworks.bridge.Roles.ADMIN;
import static org.sagebionetworks.bridge.Roles.DEVELOPER;
import static org.sagebionetworks.bridge.Roles.RESEARCHER;
import static org.sagebionetworks.bridge.Roles.SUPERADMIN;
import static org.sagebionetworks.bridge.TestConstants.TEST_APP_ID;
import static org.sagebionetworks.bridge.TestUtils.assertCreate;
import static org.sagebionetworks.bridge.TestUtils.assertCrossOrigin;
import static org.sagebionetworks.bridge.TestUtils.assertDelete;
import static org.sagebionetworks.bridge.TestUtils.assertGet;
import static org.sagebionetworks.bridge.TestUtils.assertPost;
import static org.sagebionetworks.bridge.TestUtils.mockRequestBody;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.sagebionetworks.bridge.models.PagedResourceList;
import org.sagebionetworks.bridge.models.StatusMessage;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.sagebionetworks.bridge.models.organizations.Organization;
import org.sagebionetworks.bridge.services.OrganizationService;

public class OrganizationControllerTest extends Mockito {
    
    private static final String IDENTIFIER = "anIdentifier";

    @Mock
    OrganizationService mockService;
    
    @Mock
    HttpServletRequest mockRequest;

    @InjectMocks
    @Spy
    OrganizationController controller;
    
    @Captor
    ArgumentCaptor<Organization> orgCaptor; 

    UserSession session;
    
    @BeforeMethod
    public void beforeMethod() { 
        MockitoAnnotations.initMocks(this);
        
        session = new UserSession();
        session.setAppId(TEST_APP_ID);
        
        doReturn(mockRequest).when(controller).request();
    }
    
    @Test
    public void verifyAnnotations() throws Exception {
        assertCrossOrigin(OrganizationController.class);
        assertGet(OrganizationController.class, "getOrganizations");
        assertCreate(OrganizationController.class, "createOrganization");
        assertPost(OrganizationController.class, "updateOrganization");
        assertGet(OrganizationController.class, "getOrganization");
        assertDelete(OrganizationController.class, "deleteOrganization");
    }
    
    @Test
    public void getOrganizations() {
        doReturn(session).when(controller).getAuthenticatedSession(ADMIN, RESEARCHER, DEVELOPER);
        
        PagedResourceList<Organization> page = new PagedResourceList<>(ImmutableList.of(), 10);
        when(mockService.getOrganizations(TEST_APP_ID, 150, 50)).thenReturn(page);
        
        PagedResourceList<Organization> retValue = controller.getOrganizations("150", "50");
        assertSame(retValue, page);
        
        verify(mockService).getOrganizations(TEST_APP_ID, 150, 50);
    }
    
    @Test
    public void getOrganizationsWithDefaults() {
        doReturn(session).when(controller).getAuthenticatedSession(ADMIN, RESEARCHER, DEVELOPER);
        
        controller.getOrganizations(null, null);
        
        verify(mockService).getOrganizations(TEST_APP_ID, 0, API_DEFAULT_PAGE_SIZE);
    }
    
    @Test
    public void createOrganization() throws Exception {
        doReturn(session).when(controller).getAuthenticatedSession(SUPERADMIN);
        
        Organization org = Organization.create();
        org.setName("This is my organization");
        mockRequestBody(mockRequest, org);
        
        when(mockService.createOrganization(any()))
            .thenAnswer(answer -> answer.getArgument(0));
        
        Organization retValue = controller.createOrganization();
        assertEquals(retValue.getAppId(), TEST_APP_ID);
        assertEquals(retValue.getName(), "This is my organization");
        
        verify(mockService).createOrganization(orgCaptor.capture());
        assertEquals(orgCaptor.getValue().getAppId(), TEST_APP_ID);
        assertEquals(orgCaptor.getValue().getName(), "This is my organization");
    }
    
    @Test
    public void updateOrganization() throws Exception {
        doReturn(session).when(controller).getAuthenticatedSession(SUPERADMIN);
        
        Organization org = Organization.create();
        org.setName("This is my organization");
        mockRequestBody(mockRequest, org);
        
        when(mockService.updateOrganization(any()))
            .thenAnswer(answer -> answer.getArgument(0));
        
        Organization retValue = controller.updateOrganization(IDENTIFIER);
        assertEquals(retValue.getAppId(), TEST_APP_ID);
        assertEquals(retValue.getName(), "This is my organization");
        assertEquals(retValue.getIdentifier(), IDENTIFIER);
        
        verify(mockService).updateOrganization(orgCaptor.capture());
        assertEquals(orgCaptor.getValue().getAppId(), TEST_APP_ID);
        assertEquals(orgCaptor.getValue().getName(), "This is my organization");
        assertEquals(orgCaptor.getValue().getIdentifier(), IDENTIFIER);
    }
    
    @Test
    public void getOrganization() {
        doReturn(session).when(controller).getAuthenticatedSession(SUPERADMIN);
        
        Organization org = Organization.create();
        when(mockService.getOrganization(TEST_APP_ID, IDENTIFIER)).thenReturn(org);
        
        Organization retValue = controller.getOrganization(IDENTIFIER);
        assertSame(retValue, org);
    }
    
    @Test
    public void deleteOrganization() {
        doReturn(session).when(controller).getAuthenticatedSession(SUPERADMIN);

        StatusMessage message = controller.deleteOrganization(IDENTIFIER);
        assertEquals(message.getMessage(), "Organization deleted.");
        
        verify(mockService).deleteOrganization(TEST_APP_ID, IDENTIFIER);
    }
}
