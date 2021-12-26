package com.zxw.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zxw.R;
import com.zxw.service.BluetoothLeService;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static com.zxw.util.Util.bytesToHexString;
import static com.zxw.util.Util.hexString2ByteArray;

/**
 * @author
 * @Description: TODO<Ble_Activity实现连接BLE, 发送和接受BLE的数据>
 * @data:
 * @version:
 */
public class Ble_Activity extends BasActivity implements OnClickListener {

    private final static String TAG = Ble_Activity.class.getSimpleName();

    // 蓝牙4.0的UUID,其中0000ffe1-0000-1000-8000-00805f9b34fb
    public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String EXTRAS_DEVICE_RSSI = "RSSI";

    // 蓝牙service,负责后台的蓝牙服务
    private static BluetoothLeService mBluetoothLeService;
    private static BluetoothGattCharacteristic target_chara = null;
    public static byte[] revDataForCharacteristic;

    // 蓝牙地址
    private String mDeviceAddress = "";

    //
    private final Handler mHandler = new Handler();
    private String status = "disconnected";

    /* BluetoothLeService绑定的回调函数 */
    private String rev_str = "";
    private TextView rev_tv, connect_state;
    private EditText send_et, send_et_hex;
    private ScrollView rev_sv;
    private CheckBox sendCheckBox, receptionCheckBox;
    //
    private boolean sendHex = false;
    private boolean receptionHex = false;


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
                    // Show all the supported services and characteristics on the
                    // user interface.
                    // 获取设备的所有蓝牙服务
                    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                    System.out.println("BroadcastReceiver :" + "device SERVICES_DISCOVERED");
                    break;
                }

                case BluetoothLeService.ACTION_DATA_AVAILABLE: {
                    // 有效数据, 处理接收的数据
                    try {
                        String data = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA);
                        if (data != null) {
                            displayData(data, intent);
                            System.out.println("BroadcastReceiver onData:" + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
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
        rev_sv = this.findViewById(R.id.rev_sv);
        rev_tv = this.findViewById(R.id.rev_tv);
        connect_state = this.findViewById(R.id.connect_state);
        // 发送按钮
        send_et = this.findViewById(R.id.send_et);
        send_et_hex = findViewById(R.id.send_et_hex);
        sendCheckBox = findViewById(R.id.button_group_send);
        receptionCheckBox = findViewById(R.id.button_group_reception);
        connect_state.setText(status);

        Button send_btn = this.findViewById(R.id.send_btn);
        send_btn.setOnClickListener(this);
        receptionCheckBox.setOnClickListener(this);
        sendCheckBox.setOnClickListener(this);
        receptionHex = false;
        sendHex = false;
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
     *  更新连接状态
     */
    private void updateConnectionState(String status) {
        runOnUiThread(() -> {
            Log.d(TAG, "connect_state:" + status);
            connect_state.setText(status);
        });
    }

    /**
     * 接收到的数据在scrollview上显示
     */
    private void displayData(String rev_string, Intent intent) {
        try {
            byte[] data = intent.getByteArrayExtra("BLE_BYTE_DATA");
            if (data == null) {
                Log.d(TAG, "data is null!!!!!!");
                return;
            }
            if (receptionHex) {
                rev_string = bytesToHexString(data);
            } else {
                rev_string = new String(data, 0, data.length, "GB2312");//GB2312编码
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        rev_str += rev_string;

        // 更新UI
        runOnUiThread(() -> {
            rev_tv.setText(rev_str);
            rev_sv.scrollTo(0, rev_tv.getMeasuredHeight());
            System.out.println("rev:" + rev_str);
        });
    }

    /**
     * 处理蓝牙服务
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            // 获取服务列表
            String uuid = gattService.getUuid().toString();
            System.out.println("Service uuid:" + uuid);

            // 从当前循环所指向的服务中读取特征值列表
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            // 对于当前循环所指向的服务中的每一个特征值
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                uuid = gattCharacteristic.getUuid().toString();

                if (gattCharacteristic.getUuid().toString().equals(HEART_RATE_MEASUREMENT)) {

                    // 测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                    mHandler.postDelayed(() -> {
                        System.out.println("readCharacteristic");
                        mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                        // TODO Auto-generated method stub
                        mBluetoothLeService.readCharacteristic(gattCharacteristic);
                    }, 200);

                    System.out.println("Client uuid:" + uuid);

                    // 接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    // mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    target_chara = gattCharacteristic;

                    // 设置数据内容
                    // 往蓝牙模块写入数据
                    // mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                }

                List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    System.out.println("---descriptor UUID:"
                            + descriptor.getUuid());
                    // 获取特征值的描述
                    mBluetoothLeService.getCharacteristicDescriptor(descriptor);
                    // mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    // true);
                }
            }
        }
    }

    /**
     * 将数据分包
     **/
    public int[] dataSeparate(int len) {
        int[] lens = new int[2];
        lens[0] = len / 20;
        lens[1] = len % 20;
        return lens;
    }

    /*
     * 发送按键的响应事件，主要发送文本框的数据
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.send_btn:
                new sendDataThread();
                break;

            case R.id.button_group_reception:
                receptionHex = receptionCheckBox.isChecked();
                break;

            case R.id.button_group_send:
                sendHex = sendCheckBox.isChecked();
                initEdit(sendHex);
                break;

            default:
                Log.e(TAG, "no one handle this ---");
        }
    }

    private void initEdit(boolean isHex) {
        if (isHex) {
            Toast.makeText(mBluetoothLeService, "只能输入0到F的字符", Toast.LENGTH_SHORT).show();
            send_et.setVisibility(View.GONE);
            send_et_hex.setVisibility(View.VISIBLE);
            send_et_hex.setText("");
            send_et_hex.setFocusable(true);
            send_et_hex.setFocusableInTouchMode(true);
            send_et_hex.requestFocus();
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            send_et_hex.setSelection(send_et_hex.getText().toString().length());
        } else {
            send_et.setVisibility(View.VISIBLE);
            send_et_hex.setVisibility(View.GONE);
            send_et.setText("");
            send_et.setFocusable(true);
            send_et.setFocusableInTouchMode(true);
            send_et.requestFocus();
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            send_et.setSelection(send_et.getText().toString().length());
        }
    }

    /*
     * 数据发送线程
     *
     * */
    private class sendDataThread implements Runnable {

        public sendDataThread() {
            super();
            new Thread(this).start();
        }

        @Override
        public void run() {

            // TODO Auto-generated method stub
            byte[] buff = null;
            try {
                if (!sendHex)
                    buff = send_et.getText().toString().getBytes("GB2312");
                else {
                    buff = hexString2ByteArray(send_et_hex.getText().toString());
                }
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int[] sendDatalens = dataSeparate(buff.length);
            Log.d("AppRunArrayLenght", "buff.length:" + buff.length);

            int length = 0;
            for (int i = 0; i < sendDatalens[0]; i++) {
                byte[] dataFor20 = new byte[20];
                for (int j = 0; j < 20; j++) {
                    dataFor20[j] = buff[i * 20 + j];
                    ++length;
                }
                System.out.println("here1");
                System.out.println("here1:" + new String(dataFor20));
                Log.d("AppRunArrayLenght", "超出20");
                //target_chara.setValue(dataFor20);
                mBluetoothLeService.writeCharacteristic(dataFor20);
            }

            if (sendDatalens[1] != 0) {
                System.out.println("here2");
                byte[] lastData = new byte[buff.length % 20];
                for (int i = 0; i < sendDatalens[1]; i++) {
                    lastData[i] = buff[sendDatalens[0] * 20 + i];
                    ++length;
                }
                String str = null;
                try {
                    str = new String(lastData, 0, sendDatalens[1], "GB2312");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (target_chara == null) {
                    runOnUiThread(() -> Toast.makeText(Ble_Activity.this, "没建立连接，请检查设备...", Toast.LENGTH_SHORT).show());
                    return;
                }
                Log.d("AppRunArrayLenght", "总发送长: " + length);
                for (byte lastDatum : lastData) {
                    Log.d("AppRunArrayLenght", "lastDatum= " + lastDatum);
                }
                //target_chara.setValue(lastData);//   --->此行出空指针错误):
                mBluetoothLeService.writeCharacteristic(lastData);
                mBluetoothLeService.startSend(target_chara);
            }
        }
    }
}

