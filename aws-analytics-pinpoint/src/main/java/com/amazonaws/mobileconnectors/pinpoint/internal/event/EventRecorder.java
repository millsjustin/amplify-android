/**
 * Copyright 2016-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.mobileconnectors.pinpoint.internal.event;

import android.database.Cursor;
import android.net.Uri;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.logging.Log;
import com.amazonaws.logging.LogFactory;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.pinpoint.internal.core.PinpointContext;
import com.amazonaws.mobileconnectors.pinpoint.internal.core.system.AndroidAppDetails;
import com.amazonaws.mobileconnectors.pinpoint.internal.core.util.StringUtil;
import com.amazonaws.mobileconnectors.pinpoint.targeting.endpointProfile.EndpointProfile;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.services.pinpoint.model.EndpointDemographic;
import software.amazon.awssdk.services.pinpoint.model.EndpointItemResponse;
import software.amazon.awssdk.services.pinpoint.model.EndpointLocation;
import software.amazon.awssdk.services.pinpoint.model.EndpointUser;
import software.amazon.awssdk.services.pinpoint.model.Event;
import software.amazon.awssdk.services.pinpoint.model.EventItemResponse;
import software.amazon.awssdk.services.pinpoint.model.EventsBatch;
import software.amazon.awssdk.services.pinpoint.model.EventsRequest;
import software.amazon.awssdk.services.pinpoint.model.EventsResponse;
import software.amazon.awssdk.services.pinpoint.model.PublicEndpoint;
import software.amazon.awssdk.services.pinpoint.model.PutEventsRequest;
import software.amazon.awssdk.services.pinpoint.model.PutEventsResponse;
import software.amazon.awssdk.services.pinpoint.model.Session;
import com.amazonaws.util.DateUtils;
import com.amazonaws.util.VersionInfoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides methods to record events and submit events to Pinpoint.
 */
public class EventRecorder {

    static final String KEY_MAX_SUBMISSION_SIZE = "maxSubmissionSize";
    static final long DEFAULT_MAX_SUBMISSION_SIZE = 1024 * 100;
    static final String KEY_MAX_PENDING_SIZE = "maxPendingSize";
    static final long DEFAULT_MAX_PENDING_SIZE = 5 * 1024 * 1024;
    static final String DATABASE_ID_KEY = "databaseId";
    static final String EVENT_ID = "event_id";
    static final String KEY_MAX_SUBMISSIONS_ALLOWED = "maxSubmissionAllowed";
    static final int DEFAULT_MAX_SUBMISSIONS_ALLOWED = 3;
    static final int SERVICE_DEFINED_MAX_EVENTS_PER_BATCH = 100;
    private static final String USER_AGENT = PinpointManager.class.getName() + "/" + VersionInfoUtils.getVersion();
    private static int clippedEventLength = 10;
    private final static int MAX_EVENT_OPERATIONS = 1000;
    private static final long MINIMUM_PENDING_SIZE = 16 * 1024;
    private static final Log log = LogFactory.getLog(EventRecorder.class);
    private final PinpointDBUtil dbUtil;
    private final ExecutorService submissionRunnableQueue;
    private final PinpointContext pinpointContext;

    EventRecorder(final PinpointContext pinpointContext,
                  final PinpointDBUtil dbUtil,
                  final ExecutorService submissionRunnableQueue) {
        this.pinpointContext = pinpointContext;
        this.dbUtil = dbUtil;
        this.submissionRunnableQueue = submissionRunnableQueue;
    }

    /**
     * Constructs a new EventRecorder specifying the client to use.
     *
     * @param pinpointContext The pinpoint pinpointContext
     */
    public static EventRecorder newInstance(final PinpointContext pinpointContext) {
        return newInstance(pinpointContext, new PinpointDBUtil(pinpointContext.getApplicationContext().getApplicationContext()));
    }

    /**
     * Constructs a new EventRecorder specifying the client to use.
     *
     * @param pinpointContext The pinpoint pinpointContext
     * @param dbUtil The reference to the database
     * @return
     */
    public static EventRecorder newInstance(final PinpointContext pinpointContext, final PinpointDBUtil dbUtil) {
        final ExecutorService submissionRunnableQueue = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(
                        MAX_EVENT_OPERATIONS),
                new ThreadPoolExecutor.DiscardPolicy());
        return new EventRecorder(pinpointContext, dbUtil, submissionRunnableQueue);
    }

    /**
     * Sets clipped event length.
     *
     * @param clippedEventLength the clipped event length
     */
    public static void setClippedEventLength(final int clippedEventLength) {
        EventRecorder.clippedEventLength = clippedEventLength;
    }

    /**
     * Closes the database.
     */
    public void closeDB() {
        dbUtil.closeDB();
    }

    /**
     * Records an {@link AnalyticsEvent}.
     *
     * @param event the analytics event
     * @return the URI of the event recorded in the local database
     */
    public Uri recordEvent(final AnalyticsEvent event) {
        if (event == null) {
            log.warn("Event cannot be null. Pass in a valid non-null event.");
            return null;
        }

        log.info(String.format("Event Recorded to database with EventType: %s",
                StringUtil.clipString(event.getEventType(), clippedEventLength, true)));

        long maxPendingSize = pinpointContext.getConfiguration().optLong(KEY_MAX_PENDING_SIZE, DEFAULT_MAX_PENDING_SIZE);
        if (maxPendingSize < MINIMUM_PENDING_SIZE) {
            maxPendingSize = MINIMUM_PENDING_SIZE;
        }

        final Uri uri = this.dbUtil.saveEvent(event);
        if (uri != null) {
            while (this.dbUtil.getTotalSize() > maxPendingSize) {
                Cursor cursor = null;
                try {
                    cursor = this.dbUtil.queryOldestEvents(5);
                    while (this.dbUtil.getTotalSize() > maxPendingSize && cursor.moveToNext()) {
                        this.dbUtil.deleteEvent(
                                cursor.getInt(EventTable.COLUMN_INDEX.ID.getValue()),
                                cursor.getInt(EventTable.COLUMN_INDEX.SIZE.getValue()));
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }

            return uri;
        } else {
            log.warn(String.format("Event: '%s' failed to record to local database.",
                    StringUtil.clipString(event.getEventType(), clippedEventLength, true)));
            return null;
        }
    }

    private static final int JSON_COLUMN_INDEX = EventTable.COLUMN_INDEX.JSON.getValue();
    private static final int ID_COLUMN_INDEX = EventTable.COLUMN_INDEX.ID.getValue();
    private static final int SIZE_COLUMN_INDEX = EventTable.COLUMN_INDEX.SIZE.getValue();

    JSONObject readEventFromCursor(final Cursor cursor, final HashMap<Integer, Integer> idsAndSizeToDelete) {
        Integer rowId = null;
        Integer size = null;
        try {
            if (cursor.isNull(ID_COLUMN_INDEX)) {
                log.error("Column 'ID' for event was NULL.");
                return null;
            } else {
                rowId = cursor.getInt(ID_COLUMN_INDEX);
            }

            if (cursor.isNull(SIZE_COLUMN_INDEX)) {
                log.error("Column 'SIZE' for event was NULL.");
            } else {
                size = cursor.getInt(SIZE_COLUMN_INDEX);
            }

            JSONObject jsonObject = null;
            if (cursor.isNull(JSON_COLUMN_INDEX)) {
                log.error(String.format(Locale.US,
                        "Event from DB with ID=%d and SiZE=%d contained a NULL message.", rowId, size));
            } else {
                final String message = cursor.getString(JSON_COLUMN_INDEX);
                try {
                    jsonObject = new JSONObject(message);
                    //link event with databaseId
                    jsonObject.put(DATABASE_ID_KEY,rowId);
                } catch (final JSONException e) {
                    log.error(String.format(Locale.US,
                            "Unable to deserialize event JSON for event with ID=%d.", rowId));
                }

                if (size != null && message.length() != size) {
                    log.warn(String.format(Locale.US,
                            "Message with ID=%d has a size mismatch. DBMsgSize=%d DBSizeCol=%d",
                            rowId, message.length(), size));
                    // In this case we had a size in the DB, but it didn't match the size of the message in the DB.
                    // We set the size as null so the total size will end up recalculated from the remaining
                    // items in the database after this item is removed.
                    size = null;
                }
            }

            return jsonObject;
        } catch (final Exception ex) {
            log.error("Failed accessing cursor to get next event.", ex);
        } finally {
            // if the row Id is not null then this item needs to be deleted from the database regardless of whether
            // the message was valid json or not, since we don't want to leave a corrupted item in the DB.
            if (rowId != null && idsAndSizeToDelete != null) {
                idsAndSizeToDelete.put(rowId, size);
            }
        }
        return null;
    }

    public void submitEvents() {
        submissionRunnableQueue.execute(new Runnable() {
            @Override
            public void run() {
                processEvents();
            }
        });
    }

    /**
     * Reads events of maximum of KEY_MAX_SUBMISSION_SIZE size.
     * The default max request size is DEFAULT_MAX_SUBMISSION_SIZE.
     *
     * @param cursor the cursor to the database to read events from
     * @param idsAndSizeToDelete map of id and size of the event
     * @return an array of the events.
     */
    JSONArray getBatchOfEvents(final Cursor cursor,
                                       final HashMap<Integer, Integer> idsAndSizeToDelete) {
        final JSONArray eventArray = new JSONArray();
        long currentRequestSize = 0;
        long eventLength;
        final long maxRequestSize = pinpointContext
                .getConfiguration()
                .optLong(KEY_MAX_SUBMISSION_SIZE, DEFAULT_MAX_SUBMISSION_SIZE);

        do {
            JSONObject json = readEventFromCursor(cursor, idsAndSizeToDelete);
            if (json != null) {
                eventLength = json.length();
                currentRequestSize += eventLength;
                eventArray.put(json);
            }
            if (currentRequestSize > maxRequestSize
                    || eventArray.length() >= SERVICE_DEFINED_MAX_EVENTS_PER_BATCH) {
                break;
            }
        } while (cursor.moveToNext());

        return eventArray;
    }

    public List<JSONObject> getAllEvents() {
        final List<JSONObject> events = new ArrayList<JSONObject>();
        Cursor cursor = null;
        try {
            cursor = dbUtil.queryAllEvents();
            while (cursor.moveToNext()) {
                JSONObject jsonEvent = readEventFromCursor(cursor, null);
                if (jsonEvent != null) {
                    events.add(jsonEvent);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }

    void processEvents() {
        final long start = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

        Cursor cursor = null;

        try {
            cursor = dbUtil.queryAllEvents();

            if (!cursor.moveToFirst()) {
                // if the cursor is empty there is nothing to do.
                log.info("No events available to submit.");
                return;
            }

            int submissions = 0;
            final long maxSubmissionsAllowed = pinpointContext
                    .getConfiguration()
                    .optInt(KEY_MAX_SUBMISSIONS_ALLOWED, DEFAULT_MAX_SUBMISSIONS_ALLOWED);

            do {
                final HashMap<Integer, Integer> batchIdsAndSizeToDelete = new HashMap<Integer, Integer>();
                final JSONArray events = this.getBatchOfEvents(cursor, batchIdsAndSizeToDelete);

                // submitEventsAndEndpoint will submit the events and add the successfully submitted events
                // into the SUCCESSFUL_EVENT_IDS and the failed events into the FAILED_EVENT_IDS map
                // respectively.
                // submitEventsAndEndpoint mutates the batchIdsAndSizeToDelete map. In cases where we
                // want to keep the events in the local database, batchIdsAndSizeToDelete is cleared
                // so we do not delete them.
                if (batchIdsAndSizeToDelete.size() > 0) {
                    submitEventsAndEndpoint(events, batchIdsAndSizeToDelete);
                    submissions++;
                }

                // Delete events from the local database. At this point batchIdsAndSizeToDelete
                // reflects the set of events that can be deleted from the local database.
                for (Integer id : batchIdsAndSizeToDelete.keySet()) {
                    try {
                        dbUtil.deleteEvent(id, batchIdsAndSizeToDelete.get(id));
                    } catch (final IllegalArgumentException exc) {
                        log.error("Failed to delete event: " + id, exc);
                    }
                }
                if (submissions >= maxSubmissionsAllowed) {
                    break;
                }
            } while (cursor.moveToNext());

            log.info(String.format(Locale.US, "Time of attemptDelivery: %d",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) - start));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void submitEventsAndEndpoint(final JSONArray eventArray,
                                         final HashMap<Integer, Integer> batchIdsAndSizeToProcess) {
        submitEventsAndEndpoint(eventArray,
                batchIdsAndSizeToProcess,
                pinpointContext.getTargetingClient().currentEndpoint());
    }

    private void submitEventsAndEndpoint(final JSONArray eventArray,
                                         final HashMap<Integer, Integer> batchIdsAndSizeToDelete,
                                         EndpointProfile endpoint) {

        if (endpoint == null) {
            log.warn("Endpoint profile is null, failed to submit events.");
            batchIdsAndSizeToDelete.clear();
            return;
        }

        // package them into an putEvents request
        PutEventsRequest request = this.createRecordEventsRequest(eventArray, endpoint);
        //TODO: figure out how to add user agent in the Java SDK
//        request.getRequestClientOptions().appendUserAgent(USER_AGENT);

        try {
            //making putEvents request
            PutEventsResponse resultResponse = pinpointContext.getPinpointServiceClient().putEvents(request);

            //process endpoint response.
            processEndpointResponse(endpoint, resultResponse);

            //request accepted, checking each event item in the response.
            processEventsResponse(eventArray, endpoint, resultResponse, batchIdsAndSizeToDelete);

            log.info(String.format(Locale.getDefault(), "Successful submission of %d events.",
                    batchIdsAndSizeToDelete.size()));
        } catch (final AmazonServiceException amazonServiceException) {
            // This is service level exception, we also have item level exception.
            log.error("AmazonServiceException occurred during send of put event ", amazonServiceException);
            final String errorCode = amazonServiceException.getErrorCode();

            // If the error is not a retryable error, delete the events from the local database.
            // Else if the error is a retryable error, keep the events in the local database.
            if (isRetryable(errorCode)) {
                log.error(
                        String.format("AmazonServiceException: Unable to successfully deliver events to server. " +
                                        "Events will be saved, error is likely recoverable. " +
                                        "Response Status code: %s, Response Error Code: %s",
                                amazonServiceException.getStatusCode(), amazonServiceException.getErrorCode()),
                        amazonServiceException);
                batchIdsAndSizeToDelete.clear();
            } else {
                log.error(
                        String.format(Locale.getDefault(), "Failed to submit events to EventService: statusCode: " +
                                amazonServiceException.getStatusCode() + " errorCode: ", errorCode),
                        amazonServiceException);
                log.error(
                        String.format(Locale.getDefault(), "Failed submission of %d events, events will be " +
                                "removed from the local database. ", eventArray.length()),
                        amazonServiceException);
            }
        } catch (final AmazonClientException amazonClientException) {
            // When the AmazonClientException is retryable, keep the events
            // in the local database.
            // For all other client exceptions occurred during submit events,
            // log the exception and delete the events in the local database.
            if (isClientExceptionRetryable(amazonClientException)) {
                log.error("AmazonClientException: Unable to successfully deliver events to server. " +
                        "Events will be saved, error likely recoverable." +
                        amazonClientException.getMessage(), amazonClientException);
                batchIdsAndSizeToDelete.clear();
            } else {
                log.error(
                        String.format(Locale.getDefault(), "AmazonClientException: Failed submission of %d events, events will be " +
                        "removed from the local database. ", eventArray.length()),
                        amazonClientException);
            }
        }
    }

    private void processEndpointResponse(EndpointProfile endpoint, PutEventsResponse resultResponse) {
        final EndpointItemResponse endpointItemResponse = resultResponse
                .eventsResponse()
                .results()
                .get(endpoint.getEndpointId())
                .endpointItemResponse();
        if(202 == endpointItemResponse.statusCode()) {
            log.info("EndpointProfile updated successfully.");
        } else {
            log.error("AmazonServiceException occurred during endpoint update: " +
                    endpointItemResponse.message());
        }
    }

    private void processEventsResponse(final JSONArray eventArray,
                                       EndpointProfile endpointProfile,
                                       final PutEventsResponse resultResponse,
                                       final Map<Integer, Integer> batchIdsAndSizeToDelete) {
        String eventId;
        EventItemResponse responseMessage;

        for(int i = 0; i < eventArray.length(); i++) {
            try {
                eventId = eventArray.getJSONObject(i).getString(EVENT_ID);
                responseMessage = resultResponse
                        .eventsResponse()
                        .results()
                        .get(endpointProfile.getEndpointId())
                        .eventsItemResponse()
                        .get(eventId);
                // If the event is Accepted by Pinpoint OR if a retryable error occurred
                // while submitting, remove the event from batchIdsAndSizeToDelete
                // so the event does not get deleted from the local database.
                if (responseMessage.message().equalsIgnoreCase("Accepted")) {
                    log.info(String.format("Successful submit event with event id %s", eventId));
                } else if (isRetryable(responseMessage.message())) {
                    log.warn(String.format("Unable to successfully deliver event to server. " +
                            "Event will be saved. Event id %s", eventId));
                    batchIdsAndSizeToDelete.remove(eventArray.getJSONObject(i).getInt(DATABASE_ID_KEY));
                } else {
                    // Item level exception, not retryable, so the event will be removed
                    // from the local database.
                    log.error(
                            String.format("Failed to submitEvents to EventService: statusCode: %s Status Message: %s",
                                    responseMessage.statusCode(), responseMessage.message()));
                }
            } catch (JSONException e) {
                log.error("Failed to get event id while processing event item response.", e);
            }
        }
    }

    private boolean isRetryable(String responseCode) {
        if (responseCode.equalsIgnoreCase("ValidationException") ||
            responseCode.equalsIgnoreCase("SerializationException") ||
            responseCode.equalsIgnoreCase("BadRequestException")) {
            return false;
        }
        return true;
    }

    private boolean isClientExceptionRetryable(AmazonClientException amazonClientException) {
        return amazonClientException.getCause() != null &&
                (amazonClientException.getCause() instanceof UnknownHostException ||
                 amazonClientException.getCause() instanceof SocketException);
    }

    /**
     * @param events array of events
     * @param endpointProfile endpoint profile for the device endpoint
     *
     * @return the request to put event
     */
    private PutEventsRequest createRecordEventsRequest(final JSONArray events,
                                                       final EndpointProfile endpointProfile) {

        final PutEventsRequest.Builder putRequestBuilder = PutEventsRequest.builder()
                .applicationId(endpointProfile.getApplicationId());
        final String endpointId = endpointProfile.getEndpointId();
        final Map<String, EventsBatch> eventsBatchMap = new HashMap<String, EventsBatch>();
        final EventsBatch.Builder eventsBatchBuilder = EventsBatch.builder();
        final PublicEndpoint.Builder endpointBuilder = PublicEndpoint.builder();
        final Map<String,Event> eventsMap = new HashMap<String, Event>();

        // build endpoint payload
        buildEndpointPayload(endpointProfile, endpointBuilder);

        for (int i = 0; i < events.length(); i++) {
            JSONObject eventJSON = null;
            AnalyticsEvent internalEvent = null;
            try {
                eventJSON = events.getJSONObject(i);
                internalEvent = AnalyticsEvent.translateToEvent(eventJSON);
            } catch (final JSONException jsonException) {
                // Do not log JSONException due to potentially sensitive information
                log.error("Stored event was invalid JSON.", jsonException);
                continue;
            }

            // build event payload
            final Event.Builder eventBuilder = Event.builder();
            buildEventPayload(internalEvent, eventBuilder);
            eventsMap.put(internalEvent.getEventId(), eventBuilder.build());
        }

        // build request payload, could also build with only endpoint payload
        buildRequestPayload(putRequestBuilder,
                endpointId,
                eventsBatchMap,
                eventsBatchBuilder,
                endpointBuilder.build(),
                eventsMap);

        return putRequestBuilder.build();
    }

    private void buildRequestPayload(PutEventsRequest.Builder putEventsBuilder,
                                     String endpointId,
                                     Map<String, EventsBatch> eventsBatchMap,
                                     EventsBatch.Builder eventsBatchBuilder,
                                     PublicEndpoint endpoint,
                                     Map<String,Event> eventsMap) {
        eventsBatchBuilder
                .endpoint(endpoint)
                .events(eventsMap);
        eventsBatchMap.put(endpointId, eventsBatchBuilder.build());

        final EventsRequest eventsRequest = EventsRequest.builder()
                .batchItem(eventsBatchMap).build();
        putEventsBuilder.eventsRequest(eventsRequest);
    }

    private void buildEndpointPayload(EndpointProfile endpointProfile,
                                      PublicEndpoint.Builder endpoint) {
        final EndpointDemographic demographic = EndpointDemographic.builder()
                .appVersion(endpointProfile.getDemographic().getAppVersion())
                .locale(endpointProfile.getDemographic().getLocale().toString())
                .timezone(endpointProfile.getDemographic().getTimezone())
                .make(endpointProfile.getDemographic().getMake())
                .model(endpointProfile.getDemographic().getModel())
                .platform(endpointProfile.getDemographic().getPlatform())
                .platformVersion(endpointProfile.getDemographic().getPlatformVersion())
                .build();

        final EndpointLocation location = EndpointLocation.builder()
                .latitude(endpointProfile.getLocation().getLatitude())
                .longitude(endpointProfile.getLocation().getLongitude())
                .postalCode(endpointProfile.getLocation().getPostalCode())
                .city(endpointProfile.getLocation().getCity())
                .region(endpointProfile.getLocation().getRegion())
                .country(endpointProfile.getLocation().getCountry())
                .build();

        final EndpointUser user;
        if (endpointProfile.getUser().getUserId() == null) {
            user = null;
        } else {
            user = EndpointUser.builder()
                    .userId(endpointProfile.getUser().getUserId())
                    .userAttributes(endpointProfile.getUser().getUserAttributes())
                    .build();
        }

        endpoint.channelType(endpointProfile.getChannelType())
                .address(endpointProfile.getAddress())
                .location(location)
                .demographic(demographic)
                .effectiveDate(DateUtils.formatISO8601Date(
                        new Date(endpointProfile.getEffectiveDate())))
                .optOut(endpointProfile.getOptOut())
                .attributes(endpointProfile.getAllAttributes())
                .metrics(endpointProfile.getAllMetrics())
                .user(user);
    }

    void buildEventPayload(AnalyticsEvent internalEvent,
                           Event.Builder eventBuilder) {
        final Session.Builder sessionBuilder = Session.builder();

        sessionBuilder.id(internalEvent.getSession().getSessionId());
        sessionBuilder.startTimestamp(DateUtils.formatISO8601Date(new Date(internalEvent.getSession().getSessionStart())));
        if (internalEvent.getSession().getSessionStop() != null &&
                internalEvent.getSession().getSessionStop() != 0L) {
            sessionBuilder.stopTimestamp(DateUtils.formatISO8601Date(new Date(internalEvent.getSession().getSessionStop())));
        }
        if (internalEvent.getSession().getSessionDuration() != null &&
                internalEvent.getSession().getSessionDuration() != 0L) {
            sessionBuilder.duration(internalEvent.getSession().getSessionDuration().intValue());
        }

        final AndroidAppDetails appDetails = internalEvent.getAppDetails();
        eventBuilder.appPackageName(appDetails.packageName())
                .appTitle(appDetails.getAppTitle())
                .appVersionCode(appDetails.versionCode())
                .attributes(internalEvent.getAllAttributes())
                .clientSdkVersion(internalEvent.getSdkVersion())
                .eventType(internalEvent.getEventType())
                .metrics(internalEvent.getAllMetrics())
                .sdkName(internalEvent.getSdkName())
                .session(sessionBuilder.build())
                .timestamp(DateUtils.formatISO8601Date(new Date(internalEvent.getEventTimestamp())));
    }
}
