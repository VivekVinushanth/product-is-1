/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.identity.scenarios.test.scim2;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.identity.scenarios.commons.SCIM2CommonClient;
import org.wso2.identity.scenarios.commons.ScenarioTestBase;
import org.wso2.identity.scenarios.commons.util.Constants;
import org.wso2.identity.scenarios.commons.util.SCIMProvisioningUtil;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.wso2.identity.scenarios.commons.util.Constants.IS_HTTPS_URL;
import static org.wso2.identity.scenarios.commons.util.IdentityScenarioUtil.getJSONFromResponse;

public class ProvisionExistingUserTestCase extends ScenarioTestBase {

    private String userId;
    private CloseableHttpClient client;
    private String USERNAME = "scim2user";
    private String PASSWORD = "scim2pwd";
    private String userNameResponse;
    HttpResponse response, errorResponse;
    JSONObject rootObject;
    private SCIM2CommonClient scim2Client;


    @BeforeClass(alwaysRun = true)
    public void testInit() throws Exception {

        client = HttpClients.createDefault();
        super.init();
        scim2Client = new SCIM2CommonClient(getDeploymentProperty(IS_HTTPS_URL));
        cleanUpUser();
        scim2CreateUser();
    }

    private void cleanUpUser() {

        try {
            HttpResponse user = scim2Client.filterUserByAttribute(
                    client, "username", "Eq", SCIMConstants.USERNAME, ADMIN_USERNAME, ADMIN_PASSWORD);
            assertEquals(user.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Failed to retrieve the user");
            JSONObject list = getJSONFromResponse(user);
            if (list.get("totalResults").toString().equals("1")) {
                JSONArray resourcesArray = (JSONArray) list.get("Resources");
                JSONObject userObject = (JSONObject) resourcesArray.get(0);
                String userIdentifier = userObject.get(SCIMConstants.ID_ATTRIBUTE).toString();
                assertNotNull(userIdentifier);
                SCIMProvisioningUtil.deleteUser(backendURL, userIdentifier, Constants.SCIMEndpoints.SCIM2_ENDPOINT,
                        Constants.SCIMEndpoints.SCIM_ENDPOINT_USER, ADMIN_USERNAME, ADMIN_PASSWORD);
            } // it is already cleared.
        } catch (Exception e) {
            fail("Failed when trying to delete existing user.");
        }
    }

    public void scim2CreateUser() throws Exception {

        rootObject = new JSONObject();
        JSONArray schemas = new JSONArray();
        rootObject.put(SCIMConstants.SCHEMAS_ATTRIBUTE, schemas);
        JSONObject names = new JSONObject();
        names.put(SCIMConstants.GIVEN_NAME_ATTRIBUTE, SCIMConstants.GIVEN_NAME_CLAIM_VALUE);
        rootObject.put(SCIMConstants.NAME_ATTRIBUTE, names);
        rootObject.put(SCIMConstants.USER_NAME_ATTRIBUTE, SCIMConstants.USERNAME);
        rootObject.put(SCIMConstants.PASSWORD_ATTRIBUTE, SCIMConstants.PASSWORD);

        response = SCIMProvisioningUtil.provisionUserSCIM(backendURL, rootObject, Constants.SCIMEndpoints.SCIM2_ENDPOINT, Constants.SCIMEndpoints.SCIM_ENDPOINT_USER, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_CREATED, "User has not been created successfully");

        userNameResponse = rootObject.get(SCIMConstants.USER_NAME_ATTRIBUTE).toString();
        assertEquals(userNameResponse, SCIMConstants.USERNAME, "username not found");
    }

    @Test(description = "1.1.2.1.2.4")
    public void testSCIM2CreateExistingUser() throws Exception {

        errorResponse = SCIMProvisioningUtil.provisionUserSCIM(backendURL, this.rootObject, Constants.SCIMEndpoints.SCIM2_ENDPOINT, Constants.SCIMEndpoints.SCIM_ENDPOINT_USER, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(errorResponse.getStatusLine().getStatusCode(), HttpStatus.SC_CONFLICT, "User has not been created successfully");

        Object responseObj = JSONValue.parse(EntityUtils.toString(errorResponse.getEntity()));
        assertTrue(
                responseObj.toString().contains("User with the name: " + USERNAME + " already exists in the system"));
    }

    @AfterClass(alwaysRun = true)
    private void cleanUp() throws Exception {

        JSONObject responseObj = getJSONFromResponse(this.response);
        userId = responseObj.get(SCIMConstants.ID_ATTRIBUTE).toString();

        response = SCIMProvisioningUtil.deleteUser(backendURL, userId, Constants.SCIMEndpoints.SCIM2_ENDPOINT, Constants.SCIMEndpoints.SCIM_ENDPOINT_USER, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_NO_CONTENT, "User has not been deleted successfully");
    }

}
