package com.amplifyframework.datastore.debug;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DataStoreDebugger {
    private static DataStoreDebugger debugger;
    private static final String CHANNEL_ID = "DataStoreDebuggerChannel";
    private static final int NOTIFICATION_ID = 8216;
    private LinkedHashMap<String, ModelElement> modelsMap  = new LinkedHashMap<>();
    private EventState syncEngineState;
    private List<Consumer<List<ModelElement>>> listeners = new ArrayList<>();

    private DataStoreDebugger() { }

    public static synchronized DataStoreDebugger instance() {
        if (debugger == null) {
            debugger = new DataStoreDebugger();
        }
        return debugger;
    }

    public void showNotification(Context context) {
        createNotificationChannel(context);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, getNotificationBuilder(context).build());
    }

    private NotificationCompat.Builder getNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_amplify)
                .setContentTitle("DataStore Debugger")
                .setContentText("Click here!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(getPendingIntent(context));
    }

    private PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, DataStoreDebuggerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void setSyncEngineState(EventState syncEngineState) {
        this.syncEngineState = syncEngineState;

        notifyListeners();
    }

    public ModelElement getElement(String modelName) {
        ModelElement element = modelsMap.get(modelName);
        if(element == null) {
            element = new ModelElement(modelName);
            modelsMap.put(modelName, element);
        }
        return element;
    }

    public void startEvent(String modelName, EventType eventType) {
        getElement(modelName).startEvent(eventType);
        notifyListeners();
    }

    public void finishEvent(String modelName) {
        getElement(modelName).finishEvent();
        notifyListeners();
    }

    public void errorEvent(String modelName, DataStoreException dataStoreException) {
        getElement(modelName).errorEvent(dataStoreException);
        notifyListeners();
    }

    private void notifyListeners() {
        for(Consumer<List<ModelElement>> listener : listeners) {
            listener.accept(new ArrayList<>(modelsMap.values()));
        }
    }

    public void listen(Consumer<List<ModelElement>> listener) {
        listeners.add(listener);
        notifyListeners();
    }
}
