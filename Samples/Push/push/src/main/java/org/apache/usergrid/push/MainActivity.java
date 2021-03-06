package org.apache.usergrid.push;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.firebase.iid.FirebaseInstanceId;

import org.apache.usergrid.android.UsergridAsync;
import org.apache.usergrid.android.UsergridSharedDevice;
import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridClientConfig;
import org.apache.usergrid.java.client.UsergridEnums;
import org.apache.usergrid.java.client.UsergridRequest;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public static String ORG_ID = "rjwalsh";
    public static String APP_ID = "sdk.demo";
    public static String BASE_URL = "https://apibaas-trial.apigee.net";

    public static String NOTIFIER_ID = "fcmAndroidPush";
    public static String FCM_TOKEN = "";

    public static boolean USERGRID_PREFS_NEEDS_REFRESH = false;
    private static final String USERGRID_PREFS_FILE_NAME = "usergrid_prefs.xml";

    public static UsergridClient initUsergridInstance() {
        return Usergrid.initSharedInstance(ORG_ID,APP_ID,BASE_URL);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUsergridInstance();
        retrieveSavedPrefs();

        final ImageButton infoButton = (ImageButton) findViewById(R.id.infoButton);
        if( infoButton != null ) {
            final Intent settingsActivity = new Intent(this, SettingsActivity.class);
            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.startActivity(settingsActivity);
                }
            });
        }

        final Button pushToThisDeviceButton = (Button) findViewById(R.id.pushToThisDevice);
        if( pushToThisDeviceButton != null ) {
            pushToThisDeviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.sendPush(UsergridSharedDevice.getSharedDeviceUUID(MainActivity.this),"Push To This Device");
                }
            });
        }
        final Button pushToAllDevicesButton = (Button) findViewById(R.id.pushToAllDevices);
        if( pushToAllDevicesButton != null ) {
            pushToAllDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.sendPush("*","Push To All Devices");
                }
            });
        }
    }

    @Override
    protected void onResume() {
        if( USERGRID_PREFS_NEEDS_REFRESH ) {
            Usergrid.setConfig(new UsergridClientConfig(ORG_ID,APP_ID,BASE_URL));
            if( FCM_TOKEN != null && !FCM_TOKEN.isEmpty() ) {
                UsergridAsync.applyPushToken(this, FCM_TOKEN, MainActivity.NOTIFIER_ID, new UsergridResponseCallback() {
                    @Override
                    public void onResponse(@NonNull UsergridResponse response) { }
                });
            }
            this.savePrefs();
            USERGRID_PREFS_NEEDS_REFRESH = false;
        } else {
            try {
                FCM_TOKEN = FirebaseInstanceId.getInstance().getToken();
                if( FCM_TOKEN != null ) {
                    UsergridAsync.applyPushToken(this, FCM_TOKEN, MainActivity.NOTIFIER_ID, new UsergridResponseCallback() {
                        @Override
                        public void onResponse(@NotNull UsergridResponse response) { }
                    });
                }
            } catch (Exception ignored) { }
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        this.savePrefs();
        super.onDestroy();
    }

    public static void registerPush(@NonNull final Context context, @NonNull final String registrationId) {
        initUsergridInstance();
        MainActivity.FCM_TOKEN = registrationId;
        UsergridAsync.applyPushToken(context, registrationId, MainActivity.NOTIFIER_ID, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                if( !response.ok() ) {
                    System.out.print("Error Description :" + response.getResponseError().toString());
                }
            }
        });
    }

    public void sendPush(@NonNull final String deviceId, @NonNull final String message) {
        HashMap<String,String> notificationMap = new HashMap<>();
        notificationMap.put(MainActivity.NOTIFIER_ID,message);
        HashMap<String,HashMap<String,String>> payloadMap = new HashMap<>();
        payloadMap.put("payloads",notificationMap);

        UsergridRequest notificationRequest = new UsergridRequest(UsergridEnums.UsergridHttpMethod.POST,UsergridRequest.APPLICATION_JSON_MEDIA_TYPE,Usergrid.clientAppUrl(),null,payloadMap,Usergrid.authForRequests(),"devices", deviceId, "notifications");
        UsergridAsync.sendRequest(notificationRequest, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                System.out.print("Push request completed successfully :" + response.ok());
                if(!response.ok() && response.getResponseError() != null) {
                    System.out.print("Error Description :" + response.getResponseError().toString());
                }
            }
        });
    }

    public void savePrefs() {
        SharedPreferences prefs = this.getSharedPreferences(USERGRID_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ORG_ID", ORG_ID);
        editor.putString("APP_ID", APP_ID);
        editor.putString("BASE_URL", BASE_URL);
        editor.putString("NOTIFIER_ID", NOTIFIER_ID);
        editor.apply();
    }

    public void retrieveSavedPrefs() {
        SharedPreferences prefs = this.getSharedPreferences(USERGRID_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        ORG_ID = prefs.getString("ORG_ID", ORG_ID);
        APP_ID = prefs.getString("APP_ID", APP_ID);
        BASE_URL = prefs.getString("BASE_URL",BASE_URL);
        NOTIFIER_ID = prefs.getString("NOTIFIER_ID",NOTIFIER_ID);
    }
}
