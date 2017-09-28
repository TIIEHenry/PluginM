package com.reginald.pluginm.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by lxy on 17-9-28.
 */

public class ProcessHelper {

    public static int sPid;

    public static String sProcessName;

    public static void init(Context context) {
        sPid = Process.myPid();
        sProcessName = getProcessName(context, sPid);
        if (TextUtils.isEmpty(sProcessName)) {
            throw new IllegalStateException("CAN NOT get processName for pid " + sPid);
        }
    }

    public static String getProcessName(Context context, int pid) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> raps = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo rap : raps) {
            if (rap != null && rap.pid == pid) {
                return rap.processName;
            }
        }
        return null;
    }
}
