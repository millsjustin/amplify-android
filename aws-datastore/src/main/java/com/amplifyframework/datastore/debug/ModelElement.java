package com.amplifyframework.datastore.debug;

import android.provider.CalendarContract;
import android.text.TextUtils;

import com.amplifyframework.datastore.DataStoreException;

import java.util.ArrayList;
import java.util.List;

public class ModelElement {
    private String modelName;
    private List<DataStoreEvent> events = new ArrayList<>();

    public ModelElement(String modelName) {
        this.modelName = modelName;
    }
    public String getModelName() {
        return modelName;
    }

    public String getTitle() {
        String title = modelName + " ";

        DataStoreEvent latestEvent = getEvents().get(getEvents().size()-1);
        title += latestEvent.getEventType();
        title +=  ": ";

        EventState modelState = getModelState();

        if(getEvents().size() == 1 && EventState.SUCCESS.equals(modelState)) {
            title += "PENDING_SYNC";
        } else {
            title += modelState;
        }

        if (EventState.ERROR.equals(modelState)) {
           title += ": ";
           title += getModelErrorDescription();
        }
        return title;
    }

    public void startEvent(EventType eventType) {
        events.add(new DataStoreEvent(eventType));
    }

    public void finishEvent() {
        DataStoreEvent lastEvent = events.get(events.size() - 1);
        lastEvent.finish();
    }

    public EventState getModelState() {
        boolean hasInProgressEvents = false;
        boolean hasErrorEvents = false;
        boolean hasSuccessEvents = false;
        for(DataStoreEvent event : events) {
            switch (event.getEventState()) {
                case IN_PROGRESS:
                    hasInProgressEvents = true;
                    break;
                case ERROR:
                    hasErrorEvents = true;
                    break;
                case SUCCESS:
                    hasSuccessEvents = true;
                    break;
                default:
                    break;
            }
        }
        if(hasInProgressEvents) {
            return EventState.IN_PROGRESS;
        } else if(hasErrorEvents) {
            return EventState.ERROR;
        } else if(hasSuccessEvents) {
            return EventState.SUCCESS;
        } else {
            return EventState.NOT_STARTED;
        }
    }

    public List<DataStoreEvent> getEvents() {
        return events;
    }

    public String getModelErrorDescription() {
        List<String> errors = new ArrayList<>();
        for(DataStoreEvent event : events) {
            if(EventState.ERROR.equals(event.getEventState())) {
                errors.add(event.getDataStoreException().getMessage());
            }
        }
        return TextUtils.join(", ", errors);
    }

    public void errorEvent(DataStoreException dataStoreException) {
        DataStoreEvent lastEvent = events.get(events.size() - 1);
        lastEvent.error(dataStoreException);
    }
}