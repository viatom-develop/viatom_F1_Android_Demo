package com.vtrump.vscaledemo;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.vtrump.vtble.Scale.ScaleUserInfo;
import com.vtrump.vtble.VTCallback.VTHRDataCallback;
import com.vtrump.vtble.VTDevice;
import com.vtrump.vtble.VTDeviceManager;
import com.vtrump.vtble.VTDeviceScale;
import com.vtrump.vtble.VTModelIdentifier;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements VTDeviceManager.VTDeviceManagerListener {
    private static final String TAG = "MainActivity";

    private VTDeviceManager mBleManager;
    private JSONObject userJson;
    private Button mScanButton;
    private ProgressBar mProgressBar;
    private TextView mContent;
    private StringBuffer sbText;
    private VTDeviceScale mDevice;

    Handler h = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Logger configuration, which is only used to type logs, is not associated with the SDK
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)
                .methodCount(0)
                .methodOffset(7)
                .tag(TAG)
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));
        // Dynamic permissions
        RxPermissions.getInstance(this)
                .request(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {

                    }
                });
        sbText = new StringBuffer();

        // bleManager Initialize
        mBleManager = VTDeviceManager.getInstance();
        // Set your key. Please contact relevant personnel for specific parameters
        mBleManager.setKey("7KRV5SEERY5GDV8J");
        mBleManager.setDeviceManagerListener(this);
        boolean isInitSuccess = mBleManager.startBle(this);

        // Construct a valid user
        userJson = new JSONObject();
        try {
            userJson.put("age", 27);
            userJson.put("height", 185);
            // male:0; female:1; Male athletes:2; Female athletes:3
            userJson.put("gender", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mScanButton = findViewById(R.id.scan);
        mProgressBar = findViewById(R.id.progress_bar);
        mContent = findViewById(R.id.content);
        mContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Set the list of devices to be scanned, please contact relevant personnel for details
                ArrayList<VTModelIdentifier> list = new ArrayList<>();
                list.add(new VTModelIdentifier(Byte.decode("0x03"), Byte.decode("0x03"), Byte.decode("0x0e"), Byte.decode("0x41")));
                mBleManager.disconnectAll();
                mBleManager.startScan(30, list);
                mProgressBar.setVisibility(View.VISIBLE);
                sbText.delete(0, sbText.length());
                status("Start scanning");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBleManager != null) {
            mBleManager.releaseBleManager();
        }
    }

    private void status(String text) {
        sbText.append(text);
        sbText.append("\n");
        mContent.setText(sbText);
    }

    /**
     * Data correction
     */
    private VTDeviceScale.VTDeviceScaleListener listener = new VTDeviceScale.VTDeviceScaleListener() {
        @Override
        public void onDataAvailable(final String res) {
            super.onDataAvailable(res);
            try {
                JSONObject result = new JSONObject(res);
                if ((int) result.get("code") == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status("Received weight data:\n" + res);
                        }
                    });
                    mDevice.setmUserInfo(userJson);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status("Physical data received:\n" + res);
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "onDataAvailable: " + res);
            Logger.json(res);
        }

        @Override
        public void onRssiReceived(int i) {
            super.onRssiReceived(i);
            Log.d(TAG, "onRssiReceived: " + i);
        }
    };

    // All of the following functions are Bluetooth and device status callbacks
    @Override
    public void onInited() {
        Log.d(TAG, "onInited: ");
        status("Finished initializing");
    }

    @Override
    public void onDeviceDiscovered(VTDevice vtDevice,int rssi) {
        Log.d(TAG, "onDeviceDiscovered: ");
    }

    @Override
    public void onDeviceConnected(VTDevice vtDevice) {
        Log.d(TAG, "onDeviceConnected: ");
        status("connected");
    }

    @Override
    public void onDeviceDisconnected(VTDevice vtDevice) {
        Log.d(TAG, "onDeviceDisconnected: ");
        status("disconnected");
    }

    @Override
    public void onDeviceServiceDiscovered(VTDevice vtDevice) {
        Log.d(TAG, "onDeviceServiceDiscovered: ");
        this.mDevice = (VTDeviceScale) vtDevice;
        this.mDevice.setScaleDataListener(listener);
        mProgressBar.setVisibility(View.INVISIBLE);
//        testHR();
    }

    @Override
    public void onScanStop() {
        Log.d(TAG, "onScanStop: ");
        status("Stopped scanning");
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onScanTimeOut() {
    }

    @Override
    public void onDeviceAdvDiscovered(VTDevice vtDevice) {
        Log.d(TAG, "onDeviceAdvDiscovered: ");
        this.mDevice = (VTDeviceScale) vtDevice;
        this.mDevice.setScaleDataListener(listener);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    //============================================================
    //      The following functions belong to [special support, customized services], please consult relevant personnel for details
    //============================================================

    /**
     * 【If your scale supports heart rate】
     * Heart rate according to support, in onDeviceServiceDiscovered () call
     */
    private void testHR() {
        if (mDevice.isSupportHR()) {
            // Set the heart rate detection time in unit seconds
            mDevice.enableHRData(30, new VTHRDataCallback() {
                @Override
                public void onHRDataAvailable(final String hrData) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // For data interpretation, refer to the integration documentation
                            Log.d(TAG, "onHRDataAvailable: " + hrData);
                        }
                    });
                }
            });
        }
    }

}
