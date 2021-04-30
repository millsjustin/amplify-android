package com.amplifyframework.datastore.debug;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Date;
import java.util.List;

public class DataStoreEvent {
    private long startTime;
    private long endTime;
    private final EventType eventType;
    private EventState eventState;
    private DataStoreException dataStoreException;

    public DataStoreEvent(EventType eventType) {
        this.eventType = eventType;
        this.startTime = new Date().getTime();
        this.eventState = EventState.IN_PROGRESS;
    }

    public void finish() {
        if(dataStoreException != null) {
            // If we've already reported an error, don't change to success.
            return;
        }
        endTime = new Date().getTime();
        eventState = EventState.SUCCESS;
    }

    public void error(DataStoreException dataStoreException) {
        endTime = new Date().getTime();
        eventState = EventState.ERROR;
        this.dataStoreException = dataStoreException;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public EventState getEventState() {
        return eventState;
    }

    public EventType getEventType() {
        return eventType;
    }

    public DataStoreException getDataStoreException() {
        return dataStoreException;
    }

    public String getDescription() {
        String description =  eventType.toString() + " ";
        description += getEventState() + ": ";
        if (dataStoreException != null) {
            if(dataStoreException instanceof DataStoreException.GraphQLResponseException) {
                DataStoreException.GraphQLResponseException exception = (DataStoreException.GraphQLResponseException) dataStoreException;
                List<GraphQLResponse.Error> errors = exception.getErrors();
                description += errors.get(0).getMessage();
                description += "\n";

            } else {
                description += dataStoreException.getMessage();
                description += "\n";
                if(dataStoreException.getCause() != null) {
                    description += dataStoreException.getCause().getMessage();
                    description += "\n";
                    if(dataStoreException.getCause().getCause() != null) {
                        description += dataStoreException.getCause().getCause().getMessage();
                        description += "\n";
                    }
                }
            }
        }
        if(endTime > startTime) {
            long diff = endTime - startTime;
            description += "\n";
            description += "Completed in ";
            description += diff;
            description += "ms";
        }
        return description;
    }
}
