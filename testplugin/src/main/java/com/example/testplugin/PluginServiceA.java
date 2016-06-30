package com.example.testplugin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by lxy on 16-6-30.
 */
public class PluginServiceA extends Service {

    private static final String TAG = "PluginServiceA";

    private IBinder myBinder = new ITestBinder.Stub() {

        @Override
        public String getTestString() throws RemoteException {
            return "Hi, I'm ITestBinder, from process " + android.os.Process.myPid();
        }
    };

    public PluginServiceA() {
        Log.d(TAG,"PluginServiceA()");
    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "service created!", Toast.LENGTH_LONG).show();
        Log.d(TAG,"onCreate()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand()");

        if (intent != null && "killself".equals(intent.getAction())) {
            Log.d(TAG,"onStartCommand() stopself!");
            stopSelf();
        }

        return super.onStartCommand(intent,flags,startId);
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind()");
        return myBinder;//new Binder();
    }

    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind()");
        return false;
    }

    public void onRebind(Intent intent) {
        Log.d(TAG,"onRebind()");
    }
}