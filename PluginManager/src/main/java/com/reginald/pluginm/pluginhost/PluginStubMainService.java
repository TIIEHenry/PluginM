package com.reginald.pluginm.pluginhost;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;

import com.android.common.ActivityThreadCompat;
import com.android.common.ContextCompat;
import com.reginald.pluginm.DexClassLoaderPluginManager;
import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.pluginbase.PluginContext;
import com.reginald.pluginm.reflect.FieldUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PluginStubMainService extends Service {

    public static final String INTENT_EXTRA_START_TYPE_KEY = "extra.plugin.stubservice.start.type";
    public static final String INTENT_EXTRA_START_TYPE_START = "extra.plugin.stubservice.start.type.start";
    public static final String INTENT_EXTRA_START_TYPE_STOP = "extra.plugin.stubservice.start.type.stop";
    public static final String INTENT_EXTRA_BIND_ACTION_PREFIX = "action.plugin.stubservice.bind";

    private static final String TAG = "PluginStubMainService";

    DexClassLoaderPluginManager mDexClassLoaderPluginManager;

    Map<ComponentName, ServiceRecord> mInstalledServices = new HashMap<>();

    public void onCreate() {
        super.onCreate();
        mDexClassLoaderPluginManager.getInstance(getApplicationContext());
        Log.d(TAG, "onCreate()");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        if (intent != null) {
            String commandType = intent.getStringExtra(INTENT_EXTRA_START_TYPE_KEY);
            ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
            if (targetComponent != null && commandType != null) {

                if (commandType.equals(INTENT_EXTRA_START_TYPE_START)) {

                    ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);

                    Log.d(TAG, "onStartCommand() call Service.onStartCommand() of " + pluginServiceRecord.service);
                    if (pluginServiceRecord != null) {
                        pluginServiceRecord.started = true;
                        pluginServiceRecord.service.onStartCommand(intent, flags, startId);
                    }

                } else if (commandType.equals(INTENT_EXTRA_START_TYPE_STOP)) {
                    ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(targetComponent);
                    if (pluginServiceRecord != null) {
                        pluginServiceRecord.started = false;
                        if (pluginServiceRecord.canStopped()) {
                            removePluginService(pluginServiceRecord);
                        }
                    }
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        synchronized (mInstalledServices) {
            for (ServiceRecord serviceRecord : mInstalledServices.values()) {

                Log.d(TAG, "onDestroy() try destory" + serviceRecord);

                if (serviceRecord != null) {
                    Log.d(TAG, "onDestroy() call Service.onDestroy() of " + serviceRecord.service);
                    serviceRecord.service.onDestroy();
                }
            }
            mInstalledServices.clear();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        showAction(intent);

        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        Intent pluginIntent = getPluginIntent(intent);
        if (targetComponent != null) {
            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);
            Log.d(TAG, "onBind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                BindRecord bindRecord = new BindRecord();
                bindRecord.iBinder = pluginServiceRecord.service.onBind(pluginIntent);
                bindRecord.needOnbind = false;
                bindRecord.needUnbind = true;
                pluginServiceRecord.addBindRecord(pluginIntent, bindRecord);
                pluginServiceRecord.bindCount++;
                Log.d(TAG, "onBind() return " + bindRecord.iBinder);
                return bindRecord.iBinder;
            }
        }

        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean res = true;
        Log.d(TAG, "onUnbind()");
        showAction(intent);

        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        Intent pluginIntent = getPluginIntent(intent);
        if (targetComponent != null) {
            ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(targetComponent);
            Log.d(TAG, "onUnbind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                BindRecord bindRecord = pluginServiceRecord.getBindRecord(pluginIntent);
                if (bindRecord != null) {
                    Log.d(TAG, "onUnbind() BindRecord = " + bindRecord);
                    if (pluginServiceRecord.bindCount > 0) {
                        if (bindRecord.needUnbind) {
                            bindRecord.needRebind = pluginServiceRecord.service.onUnbind(pluginIntent);
                        }
                        pluginServiceRecord.bindCount--;
                    }

                    if (pluginServiceRecord.canStopped()) {
                        removePluginService(pluginServiceRecord);
                    }
                } else {
                    Log.e(TAG, "onUnbind() can find BindRecord for " + pluginIntent);
                }
            }
        }
        Log.d(TAG, "onUnbind() return " + res);
        return res;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind()");
        showAction(intent);

        ComponentName targetComponent = intent.getParcelableExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        Intent pluginIntent = getPluginIntent(intent);
        if (targetComponent != null) {
            ServiceRecord pluginServiceRecord = fetchCachedOrCreateServiceRecord(targetComponent);
            Log.d(TAG, "onRebind() before pluginServiceRecord = " + pluginServiceRecord);
            if (pluginServiceRecord != null) {
                BindRecord bindRecord = pluginServiceRecord.getBindRecord(pluginIntent);

                if (bindRecord == null) {
                    Log.d(TAG, "onRebind() add BindRecord for " + pluginServiceRecord);
                    bindRecord = new BindRecord();
                    pluginServiceRecord.addBindRecord(pluginIntent, bindRecord);
                }

                Log.d(TAG, "onRebind() BindRecord = " + bindRecord);

                pluginServiceRecord.bindCount++;
                if (bindRecord.needRebind) {
                    pluginServiceRecord.service.onRebind(pluginIntent);
                    bindRecord.needUnbind = true;
                } else if (bindRecord.needOnbind) {
                    bindRecord.iBinder = pluginServiceRecord.service.onBind(pluginIntent);
                    bindRecord.needOnbind = false;
                    bindRecord.needUnbind = true;
                } else {
                    bindRecord.needUnbind = false;
                }

            }
        }
    }

    private Intent getPluginIntent(Intent intent) {
        Intent resIntent = new Intent(intent);
        String action = resIntent.getAction();
        if (action != null) {
            String[] actions = action.split(INTENT_EXTRA_BIND_ACTION_PREFIX);
            resIntent.setAction(actions[0]);
        }
        resIntent.removeExtra(DexClassLoaderPluginManager.EXTRA_INTENT_TARGET_COMPONENT);
        return resIntent;
    }

    private ServiceRecord fetchCachedOrCreateServiceRecord(ComponentName componentName) {
        ServiceRecord pluginServiceRecord = fetchCachedServiceRecord(componentName);
        if (pluginServiceRecord == null) {
            pluginServiceRecord = createPluginService(componentName);
        }

        return pluginServiceRecord;
    }

    private ServiceRecord fetchCachedServiceRecord(ComponentName componentName) {
        // install plugin if needed
        PluginInfo pluginInfo = DexClassLoaderPluginManager.getPluginInfo(componentName.getPackageName());
        if (pluginInfo == null) {
            mDexClassLoaderPluginManager.install(componentName.getPackageName(), false);
        }

        synchronized (mInstalledServices) {
            Log.d(TAG, "fetchCachedServiceRecord() return" + mInstalledServices.get(componentName));
            return mInstalledServices.get(componentName);
        }
    }

    private ServiceRecord createPluginService(ComponentName componentName) {
        ServiceRecord pluginServiceRecord = new ServiceRecord();
        pluginServiceRecord.componentName = componentName;
        PluginInfo pluginInfo = DexClassLoaderPluginManager.getPluginInfo(componentName.getPackageName());
        ServiceInfo serviceInfo = DexClassLoaderPluginManager.getServiceInfo(componentName.getPackageName(), componentName.getClassName());

        if (serviceInfo == null) {
            return null;
        }

        try {
            Class<?> serviceClass = pluginInfo.classLoader.loadClass(serviceInfo.name);
            pluginServiceRecord.service = (Service) serviceClass.newInstance();
            Log.d(TAG, "createPluginService() create service " + pluginServiceRecord.service);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (pluginServiceRecord.service != null) {
            try {
                Method attachMethod = android.app.Service.class
                        .getDeclaredMethod("attach",
                                Context.class, ActivityThreadCompat.getActivityThreadClass(), String.class,
                                IBinder.class, Application.class, Object.class);
                attachMethod.setAccessible(true);


                Context pluginServiceContext = new PluginContext(pluginInfo, getBaseContext());
                ContextCompat.setOuterContext(pluginServiceContext, pluginServiceRecord.service);

                attachMethod.invoke(pluginServiceRecord.service, pluginServiceContext, FieldUtils.readField(this, "mThread"), serviceInfo.name,
                        FieldUtils.readField(this, "mToken"), pluginInfo.application, FieldUtils.readField(this, "mActivityManager"));

                synchronized (mInstalledServices) {
                    mInstalledServices.put(componentName, pluginServiceRecord);
                }
                Log.d(TAG, "createPluginService() call Service.onCreate() of " + pluginServiceRecord.service);
                pluginServiceRecord.service.onCreate();

                return pluginServiceRecord;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    private boolean removePluginService(ServiceRecord serviceRecord) {
        serviceRecord.service.onDestroy();
        synchronized (mInstalledServices) {
            Log.d(TAG, "removePluginService() " + serviceRecord);
            boolean isSuc = mInstalledServices.remove(serviceRecord.componentName) != null;
//            stopStubServiceIfNeededLocked();
            return isSuc;
        }

    }

    private void stopStubServiceIfNeededLocked() {
        if (mInstalledServices.isEmpty()) {
            stopSelf();
            Log.d(TAG, "stopStubServiceIfNeededLocked() stop stub service!");
        }
    }

    public void showAction(Intent intent) {
        Log.d(TAG, "ACTION = " + intent.getAction());
    }

    public static String getPluginAppendAction(String packageName, String className) {
        //加上pid是因为不同pid的bind同样intent的service，只有第一个进程的bindService触发onRebind回调。
        return PluginStubMainService.INTENT_EXTRA_BIND_ACTION_PREFIX + android.os.Process.myPid() +
                packageName + "#" + className;
    }


    private static class ServiceRecord {
        public ComponentName componentName;

        public Service service;
        public boolean started;
        public int bindCount = 0;

        HashMap<Intent.FilterComparison, BindRecord> bindRecords = new HashMap<>();

        public boolean canStopped() {
            return service != null && bindCount == 0 && !started;
        }

        public BindRecord getBindRecord(Intent intent) {
            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
            return bindRecords.get(filterComparison);
        }

        public void addBindRecord(Intent intent, BindRecord bindRecord) {
            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
            bindRecords.put(filterComparison, bindRecord);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + String.format("[ service = %s, started = %b, bindCount = %d, bindRecords = %s]",
                    service, started, bindCount, bindRecords);
        }
    }


    private static class BindRecord {
        public Intent.FilterComparison intentFilter;
        public IBinder iBinder;
        public boolean needRebind;
        public boolean needUnbind;
        public boolean needOnbind = true;

        @Override
        public String toString() {
            return getClass().getSimpleName() + String.format("[ intentFilter = %s, iBinder = %s, needRebind = %b, needUnbind = %b, needOnbind = %b ]",
                    intentFilter, iBinder, needRebind, needUnbind, needOnbind);
        }
    }
}