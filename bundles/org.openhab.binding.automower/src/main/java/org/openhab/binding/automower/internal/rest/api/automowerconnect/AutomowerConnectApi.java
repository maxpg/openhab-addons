/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.automower.internal.rest.api.automowerconnect;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.automower.internal.rest.api.HusqvarnaApi;
import org.openhab.binding.automower.internal.rest.api.automowerconnect.dto.MowerCommandRequest;
import org.openhab.binding.automower.internal.rest.api.automowerconnect.dto.MowerListResult;
import org.openhab.binding.automower.internal.rest.api.automowerconnect.dto.MowerResult;
import org.openhab.binding.automower.internal.rest.exceptions.AutomowerCommunicationException;
import org.openhab.binding.automower.internal.rest.exceptions.UnauthorizedException;

import com.google.gson.JsonSyntaxException;

/**
 * Allows access to the AutomowerConnectApi
 *
 * @author Markus Pfleger - Initial contribution
 */
@NonNullByDefault
public class AutomowerConnectApi extends HusqvarnaApi {

    public AutomowerConnectApi(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public String getBaseUrl() {
        return "https://api.amc.husqvarna.dev/v1";
    }

    public MowerListResult getMowers(String appKey, String token) throws AutomowerCommunicationException {
        final Request request = getHttpClient().newRequest(getBaseUrl() + "/mowers");
        request.method(HttpMethod.GET);

        request.header("Authorization-Provider", "husqvarna");
        request.header("Authorization", "Bearer " + token);
        request.header("X-Api-Key", appKey);
        request.header("Content-Type", "application/vnd.api+json");

        ContentResponse response;
        try {
            response = request.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new AutomowerCommunicationException(e);
        }

        return parseResponse(response, MowerListResult.class);
    }

    public MowerResult getMower(String appKey, String token, String mowerId) throws AutomowerCommunicationException {
        final Request request = getHttpClient().newRequest(getBaseUrl() + "/mowers/" + mowerId);
        request.method(HttpMethod.GET);

        request.header("Authorization-Provider", "husqvarna");
        request.header("Authorization", "Bearer " + token);
        request.header("X-Api-Key", appKey);
        request.header("Content-Type", "application/vnd.api+json");

        ContentResponse response;
        try {
            response = request.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new AutomowerCommunicationException(e);
        }

        return parseResponse(response, MowerResult.class);
    }

    public void sendCommand(String appKey, String token, String id, MowerCommandRequest command)
            throws AutomowerCommunicationException {
        final Request request = getHttpClient().newRequest(getBaseUrl() + "/mowers/" + id + "/actions");
        request.method(HttpMethod.POST);

        request.header("Authorization-Provider", "husqvarna");
        request.header("Authorization", "Bearer " + token);
        request.header("X-Api-Key", appKey);
        request.header("Content-Type", "application/vnd.api+json");

        request.content(new StringContentProvider(gson.toJson(command)));

        ContentResponse response;
        try {
            response = request.send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new AutomowerCommunicationException(e);
        }

        checkForError(response, response.getStatus());
    }

    private <T> T parseResponse(ContentResponse response, Class<T> type) throws AutomowerCommunicationException {
        int statusCode = response.getStatus();

        checkForError(response, statusCode);

        try {
            return gson.fromJson(response.getContentAsString(), type);
        } catch (JsonSyntaxException e) {
            throw new AutomowerCommunicationException(e);
        }
    }

    private void checkForError(ContentResponse response, int statusCode) throws AutomowerCommunicationException {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        switch (statusCode) {
            case HttpStatus.SC_NOT_FOUND:
                throw new AutomowerCommunicationException(statusCode, "Target '" + response.getRequest().getURI()
                        + "' seems to be not available: " + response.getContentAsString());

            case HttpStatus.SC_FORBIDDEN:
            case HttpStatus.SC_UNAUTHORIZED:
                throw new UnauthorizedException(statusCode, response.getContentAsString());

            default:
                throw new AutomowerCommunicationException(statusCode, response.getContentAsString());
        }
    }
}
