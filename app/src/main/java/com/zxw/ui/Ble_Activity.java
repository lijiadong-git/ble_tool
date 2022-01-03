package com.zxw.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.zxw.R;
import com.zxw.cmd.CmdCenter;
import com.zxw.cmd.CmdEntry;
import com.zxw.service.BluetoothLeService;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * @author
 * @Description: TODO<Ble_Activity实现连接BLE, 发送和接受BLE的数据>
 * @data:
 * @version:
 */
public class Ble_Activity extends BasActivity {

    private final static String TAG = Ble_Activity.class.getSimpleName();

    // 蓝牙4.0的UUID,其中0000ffe1-0000-1000-8000-00805f9b34fb
    public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String EXTRAS_DEVICE_RSSI = "RSSI";

    // 蓝牙service,负责后台的蓝牙服务
    private static BluetoothLeService mBluetoothLeService;
    private static ICmdSender cmdSender;

    // 蓝牙地址
    private String mDeviceAddress = "";

    // 状态
    private String status = "disconnected";
    private TextView connect_state;

    /**
     * 广播接收器，负责接收BluetoothLeService类发送的数据
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();
            // 蓝牙连接状态
            switch (action) {

                case BluetoothLeService.ACTION_GATT_CONNECTED: {
                    // Gatt连接成功
                    status = "connected";
                    // 更新连接状态
                    updateConnectionState(status);
                    System.out.println("BroadcastReceiver :" + "device connected");
                    break;
                }

                case BluetoothLeService.ACTION_GATT_DISCONNECTED: {
                    // Gatt连接失败
                    status = "disconnected";
                    // 更新连接状态
                    updateConnectionState(status);
                    System.out.println("BroadcastReceiver :" + "device disconnected");
                    break;
                }

                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED: {
                    // 发现GATT服务器
                    // Show all the supported services and characteristics on the user interface.
                    System.out.println("BroadcastReceiver : device SERVICES_DISCOVERED");
                    break;
                }

                case BluetoothLeService.ACTION_DATA_AVAILABLE: {
                    // 有效数据, 处理接收的数据
                    try {
                        String data = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA);
                        if (data != null) {
                            handleReceivedData(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_activity);
        bindBluetoothServer();
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解除广播接收器
        unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
    }

    // Activity出来时候，绑定广播接收器，监听蓝牙连接服务传过来的事件
    @Override
    protected void onResume() {
        super.onResume();
        // 绑定广播接收器
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && !TextUtils.isEmpty(mDeviceAddress)) {
            //根据蓝牙地址，建立连接
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private void bindBluetoothServer() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return;
        }
        mDeviceAddress = bundle.getString(EXTRAS_DEVICE_ADDRESS);
        if (TextUtils.isEmpty(mDeviceAddress)) {
            return;
        }
        /* 启动蓝牙service */
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                    finish();
                    return;
                }
                // Automatically connects to the device upon successful start-up
                mBluetoothLeService.connect(mDeviceAddress);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mBluetoothLeService = null;
            }
        }, BIND_AUTO_CREATE);
    }

    private void init() {
        connect_state = findViewById(R.id.connect_state);
        connect_state.setText(status);

        cmdSender = (ICmdSender) (entry, callback) -> {
            if (null == mBluetoothLeService) {
                return;
            }
            if (!status.equals("connected")) {
                return;
            }
            CmdCenter.getInstance().subscribe(entry.getCmd(), callback, 8);
            mBluetoothLeService.sendPost(entry);
        };

        Fragment fragment = BaseFragment.newInstance(ModeFragment.class, cmdSender);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_content, fragment);
        transaction.commit();
    }

    /**
     * 意图过滤器
     */
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 更新连接状态
     */
    private void updateConnectionState(String status) {
        runOnUiThread(() -> {
            Log.d(TAG, "connect_state:" + status);
            connect_state.setText(status);
        });
    }

    /**
     * 接收到的数据处理
     */
    private void handleReceivedData(Intent intent) {
        try {
            CmdEntry entry = intent.getParcelableExtra("BLE_CMD_DATA");
            if (entry == null) {
                Log.d(TAG, "entry is null!!!!!!");
                return;
            }

            {
                Log.d(TAG, "entry cmd is " + entry.getCmd());
                byte[] data = entry.getDataBytes();
                String rev_string = new String(data, 0, data.length, "GB2312");
                Log.d(TAG, "-----------------------\n" +
                        " - cmd: " + entry.getCmd() +
                        " - data: " + rev_string +
                        "\n");
            }

            byte cmd = entry.getCmd();
            CmdCenter.getInstance().publish(cmd, entry);

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

