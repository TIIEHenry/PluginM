package com.example.testplugin;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.reginald.pluginm.pluginbase.BasePluginActivity;

public class PluginActivityA extends BasePluginActivity {

    private static final String TAG = "PluginActivityA";

    private Button mBtn1;
    private Button mBtn2;
    private Button mBtn3;
    private Button mBtn4;
    private Button mBtn5;
    private Button mBtn6;
    private Button mBtn7;
    private Button mBtn8;
    private Button mBtn9;
    private Button mBtn10;

    private ServiceConnection mConn1 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() mConn1 ComponentName = " + name + ", IBinder = " + service);
            ITestBinder testString = ITestBinder.Stub.asInterface(service);
            try {
                Log.d(TAG, "onServiceConnected() test binder testString.getTestString() = " + testString.getTestString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected() mConn1 ComponentName = " + name);
        }
    };

    private ServiceConnection mConn2 = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() mConn2 ComponentName = " + name + ", IBinder = " + service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected() mConn2 ComponentName = " + name);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin);

        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn1.setText("start plugin activity main");
        mBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginMainActivity.class.getName());
                startActivity(intent);
            }
        });

        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn2.setText("start plugin service");
        mBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                startService(intent);
            }
        });

        mBtn3 = (Button) findViewById(R.id.btn3);
        mBtn3.setText("stop plugin service");
        mBtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                stopService(intent);
            }
        });

        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn4.setText("bind plugin service");
        mBtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                bindService(intent, mConn1, BIND_AUTO_CREATE);
            }
        });

        mBtn5 = (Button) findViewById(R.id.btn5);
        mBtn5.setText("unbind plugin service");
        mBtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConn1);
            }
        });

        mBtn6 = (Button) findViewById(R.id.btn6);
        mBtn6.setText("bind plugin service");
        mBtn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction("asdasd");
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginService.class.getName());
                bindService(intent, mConn2, BIND_AUTO_CREATE);
            }
        });

        mBtn7 = (Button) findViewById(R.id.btn7);
        mBtn7.setText("unbind plugin service");
        mBtn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConn2);
            }
        });

        mBtn8 = (Button) findViewById(R.id.btn8);
        mBtn8.setText("kill process");
        mBtn8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.os.Process.killProcess(Process.myPid());
            }
        });

        mBtn9 = (Button) findViewById(R.id.btn9);
        mBtn9.setText("start plugin serviceA");
        mBtn9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginServiceA.class.getName());
                startService(intent);
            }
        });

        mBtn10 = (Button) findViewById(R.id.btn10);
        mBtn10.setText("stop plugin serviceA");
        mBtn10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName(PluginUtils.PLUGIN_PACKAGE_NAME, PluginServiceA.class.getName());
                stopService(intent);
            }
        });



        showClassloader();
    }

    private void showClassloader() {
        ClassLoader classLoader = getClassLoader();
        Log.d(TAG, "classloader = " + classLoader);
        Log.d(TAG, "parent classloader = " + classLoader.getParent());
    }
}