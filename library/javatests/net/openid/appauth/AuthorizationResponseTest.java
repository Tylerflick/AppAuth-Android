/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static net.openid.appauth.TestValues.TEST_ACCESS_TOKEN;
import static net.openid.appauth.TestValues.TEST_AUTH_CODE;
import static net.openid.appauth.TestValues.TEST_ID_TOKEN;
import static net.openid.appauth.TestValues.TEST_STATE;
import static net.openid.appauth.TestValues.getTestAuthRequest;
import static net.openid.appauth.TestValues.getTestAuthRequestBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=16)
public class AuthorizationResponseTest {

    // the test is asserted to be running at time 23
    private static final Long TEST_START_TIME = 23L;
    // expiration time, in seconds
    private static final Long TEST_EXPIRES_IN = 78L;
    private static final Long TEST_TOKEN_EXPIRE_TIME = 78023L;


    private static final Uri TEST_URI = new Uri.Builder()
            .appendQueryParameter(AuthorizationResponse.KEY_STATE, TEST_STATE)
            .appendQueryParameter(AuthorizationResponse.KEY_AUTHORIZATION_CODE, TEST_AUTH_CODE)
            .appendQueryParameter(AuthorizationResponse.KEY_ACCESS_TOKEN, TEST_ACCESS_TOKEN)
            .appendQueryParameter(AuthorizationResponse.KEY_TOKEN_TYPE,
                    AuthorizationResponse.TOKEN_TYPE_BEARER)
            .appendQueryParameter(AuthorizationResponse.KEY_ID_TOKEN, TEST_ID_TOKEN)
            .appendQueryParameter(AuthorizationResponse.KEY_EXPIRES_IN,
                    TEST_EXPIRES_IN.toString())
            .build();

    private AuthorizationResponse.Builder mAuthorizationResponseBuilder;
    private AuthorizationResponse mAuthorizationResponse;

    TestClock mClock;

    @Before
    public void setUp() {
        mClock = new TestClock(TEST_START_TIME);
        mAuthorizationResponseBuilder = new AuthorizationResponse.Builder(getTestAuthRequest())
                .setState(TEST_STATE)
                .setAuthorizationCode(TEST_AUTH_CODE)
                .setAccessToken(TEST_ACCESS_TOKEN)
                .setTokenType(AuthorizationResponse.TOKEN_TYPE_BEARER)
                .setIdToken(TEST_ID_TOKEN)
                .setAccessTokenExpirationTime(TEST_TOKEN_EXPIRE_TIME);

        mAuthorizationResponse = mAuthorizationResponseBuilder.build();
    }

    @Test
    public void testBuilder() {
        checkExpectedFields(mAuthorizationResponseBuilder.build());
    }

    @Test
    public void testBuildFromUri() {
        AuthorizationRequest authRequest = getTestAuthRequestBuilder()
                .setState(TEST_STATE)
                .build();
        AuthorizationResponse authResponse = new AuthorizationResponse.Builder(authRequest)
                .fromUri(new UriParser(TEST_URI), mClock)
                .build();
        checkExpectedFields(authResponse);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuild_setAdditionalParams_withBuiltInParam() {
        mAuthorizationResponseBuilder.setAdditionalParameters(
                Collections.singletonMap(AuthorizationResponse.KEY_SCOPE, "scope"));
    }

    @Test
    public void testExpiresIn() {
        AuthorizationResponse authResponse = mAuthorizationResponseBuilder
                .setAccessTokenExpiresIn(TEST_EXPIRES_IN, mClock)
                .build();
        assertEquals(TEST_TOKEN_EXPIRE_TIME, authResponse.accessTokenExpirationTime);
    }

    @Test
    public void testHasExpired() {
        mClock.currentTime.set(TEST_START_TIME + 1);
        assertFalse(mAuthorizationResponse.hasAccessTokenExpired(mClock));
        mClock.currentTime.set(TEST_TOKEN_EXPIRE_TIME - 1);
        assertFalse(mAuthorizationResponse.hasAccessTokenExpired(mClock));
        mClock.currentTime.set(TEST_TOKEN_EXPIRE_TIME + 1);
        assertTrue(mAuthorizationResponse.hasAccessTokenExpired(mClock));
    }

    @Test
    public void testSerialization() throws Exception {
        String json = mAuthorizationResponse.jsonSerializeString();
        AuthorizationResponse authResponse = AuthorizationResponse.jsonDeserialize(json);
        checkExpectedFields(authResponse);
    }

    private void checkExpectedFields(AuthorizationResponse authResponse) {
        assertEquals("state does not match",
                TEST_STATE, authResponse.state);
        assertEquals("authorization code does not match",
                TEST_AUTH_CODE, authResponse.authorizationCode);
        assertEquals("access token does not match",
                TEST_ACCESS_TOKEN, authResponse.accessToken);
        assertEquals("token type does not match",
                AuthorizationResponse.TOKEN_TYPE_BEARER, authResponse.tokenType);
        assertEquals("id token does not match",
                TEST_ID_TOKEN, authResponse.idToken);
        assertEquals("access token expiration time does not match",
                TEST_TOKEN_EXPIRE_TIME, authResponse.accessTokenExpirationTime);
    }
}
