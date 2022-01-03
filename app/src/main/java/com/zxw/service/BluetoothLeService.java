package com.zxw.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.zxw.toolkit.UIHandler;
import com.zxw.cmd.CmdEntry;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


/**
 * @author
 * @Description: TODO<蓝牙服务 ， 负责在后台实现蓝牙的连接 ， 数据的发送接受>
 * @data:··
 * @version:
 */
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public final static String ACTION_GATT_CONNECTED = "com.zxw_ble.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.zxw_ble.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.zxw_ble.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.zxw_ble.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.zxw_ble.bluetooth.le.EXTRA_DATA";
    public static final String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static int SEND_PACK_LEN = 64;

    // state
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mConnectionState = STATE_DISCONNECTED;

    private static BluetoothGattCharacteristic target_chara = null;
    //
    private final IBinder mBinder = new LocalBinder();
    public boolean mSendState = true;

    // 蓝牙相关类
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private HandlerThread mSendThread;
    private Handler mSendHandler;

    private final ByteBuffer mReceiveBuffer = ByteBuffer.allocate(100 * 1024);


    /**
     *  连接远程设备的回调函数
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction = "";
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                mConnectionState = STATE_CONNECTED;
                intentAction = ACTION_GATT_CONNECTED;

                // start a send thread
                mSendThread = new HandlerThread("BluetoothSendThread");
                mSendThread.start();
                mSendHandler = new Handler(mSendThread.getLooper());

                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                mConnectionState = STATE_DISCONNECTED;
                intentAction = ACTION_GATT_DISCONNECTED;

                // stop
                mSendThread = null;
                mSendHandler = null;
            }

            /* 通过广播更新连接状态 */
            if (!TextUtils.isEmpty(intentAction)) {
                broadcastUpdate(intentAction);
            }
        }

        /**
         * 重写onServicesDiscovered，发现蓝牙服务
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 发现到服务
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d(TAG, "-- onServicesDiscovered called --");

                // Loops through available GATT Services.
                List<BluetoothGattService> supportedGattServices = getSupportedGattServices();
                for (BluetoothGattService gattService : supportedGattServices) {

                    // 从当前循环所指向的服务中读取特征值列表
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                    // Loops through available Characteristics.
                    // 对于当前循环所指向的服务中的每一个特征值
                    for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                        String uuid = gattCharacteristic.getUuid().toString();
                        if (TextUtils.equals(uuid, HEART_RATE_MEASUREMENT)) {

                            // 读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                            UIHandler.postDelayed(() -> {
                                setCharacteristicNotification(gattCharacteristic, true);
                                readCharacteristic(gattCharacteristic);
                            }, 200);

                            target_chara = gattCharacteristic;
                        }

                        List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            Log.d(TAG, "---descriptor UUID:" + descriptor.getUuid());
                            getCharacteristicDescriptor(descriptor);
                        }
                    }
                }
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * 特征值的读写
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "--onCharacteristicRead called--");
            }
        }

        /**
         * 特征值的改变
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged ++++++++++++++++");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        /**
         * 特征值的写
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "发送完成");
                mSendState = true;
            }
        }

        /**
         * 读描述值
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            // super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "----onDescriptorRead status: " + status);
            byte[] desc = descriptor.getValue();
            if (desc != null) {
                Log.d(TAG, "----onDescriptorRead value: " + new String(desc));
            }
        }

        /**
         * 写描述值
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            // TODO Auto-generated method stub
            // super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "--onDescriptorWrite--: " + status);
        }

        /**
         * 读写蓝牙信号值
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            // super.onReadRemoteRssi(gatt, rssi, status);
            Log.d(TAG, "--onReadRemoteRssi--: " + status);
            broadcastUpdate(ACTION_DATA_AVAILABLE, rssi);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            // TODO Auto-generated method stub
            // super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "--onReliableWriteCompleted--: " + status);
        }
    };

    // 广播意图
    private void broadcastUpdate(final String action, int rssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
        sendBroadcast(intent);
    }

    // 广播意图
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * 将蓝牙接收到的数据广播出去
     */
    public void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

        // 从特征值获取数据
        final byte[] data = characteristic.getValue();
        if (data == null || data.length == 0) {
            return;
        }

        mReceiveBuffer.put(data);

        // 检测是否收到一个合法的包，并处理
        checkReceiveData();
    }

    private void checkReceiveData() {

        int dataLen = mReceiveBuffer.position();
        if (dataLen < 6) {
            // 数据不足
            return;
        }

        // 1、 找到包头
        int idx = -1;
        for (int i = 0; i < dataLen - 6; i ++) {
            if (mReceiveBuffer.get(i) == 'Y'
                    && mReceiveBuffer.get(i+1) == 'H'
                    && mReceiveBuffer.get(i+2) == 'Z'
                    && mReceiveBuffer.get(i+3) == 'X') {
                idx = i;
                break;
            }
        }

        // 没有找到合法的数据头
        // 丢掉数据，保留最后5个字节
        if (idx == -1) {
            byte[] remain = new byte[5];
            mReceiveBuffer.position(dataLen - 5);
            mReceiveBuffer.get(remain, 0, 5);
            mReceiveBuffer.position(0);
            mReceiveBuffer.put(remain);
            return;
        }

        // 打印输出包头
        byte[] header = new byte[6];
        System.arraycopy(mReceiveBuffer.array(), idx , header, 0, 6);
        Log.d(TAG, "checkReceiveData --- " + new String(header));

        // 找到合法数据头，检查是否有足够的数据
        byte cmd = mReceiveBuffer.get(idx+4);
        byte len = mReceiveBuffer.get(idx+5);
        if (dataLen < idx + 6 + len) {
            // 数据不足，等待写
            Log.d(TAG, "checkReceiveData --- 数据不足.");
            return;
        }
        byte[] cmdData = new byte[len];
        mReceiveBuffer.position(idx + 6);
        mReceiveBuffer.get(cmdData, 0, len);

        // 丢弃已用数据
        if (dataLen > idx + 6 + len) {
            int remainLen = dataLen - (idx + 6 + len);
            byte[] remain = new byte[remainLen];
            mReceiveBuffer.position(idx + 6 + len);
            mReceiveBuffer.get(remain, 0, remainLen);
            mReceiveBuffer.position(0);
            mReceiveBuffer.put(remain);
        }
        else {
            mReceiveBuffer.position(0);
        }

        // 广播一次合法命令回复
         CmdEntry entry = new CmdEntry(cmd, cmdData);
         Intent intent = new Intent(ACTION_DATA_AVAILABLE);
         intent.putExtra(EXTRA_DATA, "CmdEntry");
         intent.putExtra("BLE_CMD_DATA", entry);
         sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    /* service 中蓝牙初始化 */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // get BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    // 连接远程蓝牙
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.e(TAG,"BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG,"Trying to use an existing mBluetoothGatt for connection.");
            // 连接蓝牙
            if (!mBluetoothGatt.connect()) {
                return false;
            }
            mConnectionState = STATE_CONNECTING;
            return true;
        }

        /* 获取远端的蓝牙设备 */
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        /* 调用device中的connectGatt连接到远程设备 */
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);

        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        System.out.println("device.getBondState==" + device.getBondState());
        return true;
    }

    /**
     * @param
     * @return void
     * @throws
     * @Title: disconnect
     * @Description: TODO(取消蓝牙连接)
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * @param
     * @return void
     * @throws
     * @Title: close
     * @Description: TODO(关闭所有蓝牙连接)
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    /*
     * 取消连接
     *
     * */

    /**
     * @param @param characteristic（要读的特征值）
     * @return void    返回类型
     * @throws
     * @Title: readCharacteristic
     * @Description: TODO(读取特征值)
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);

    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public boolean sendPost(CmdEntry entry) {
        if (entry == null || mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if (null == target_chara) {
            Log.e(TAG, "target_chara not initialized");
            return false;
        }
        if (mConnectionState != STATE_CONNECTED) {
            Log.e(TAG, "BluetoothGatt not connect");
            return false;
        }
        if (mSendThread == null || mSendHandler == null) {
            Log.e(TAG, "BluetoothGatt not start to send");
            return false;
        }
        return mSendHandler.post(() -> {
            if (mConnectionState != STATE_CONNECTED) {
                return ;
            }
            byte[] bytes = entry.getCmdBytes();
            int sendPos = 0;
            while (sendPos < bytes.length) {
                int sendLen = bytes.length - sendPos;
                if (sendLen > SEND_PACK_LEN) {
                    sendLen = SEND_PACK_LEN;
                }
                byte[] sendBytes = Arrays.copyOfRange(bytes, sendPos, sendPos + sendLen);
                try {
                    if (mSendState) {
                        target_chara.setValue(sendBytes);
                        mSendState = false;
                        mBluetoothGatt.writeCharacteristic(target_chara);
                        sendPos += sendLen;
                    } else {
                        Log.d("AppRun" + getClass().getSimpleName(), "等待中..");
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    // 读取RSSi
    public void readRssi() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readRemoteRssi();
    }

    /**
     * @param @param characteristic（特征值）
     * @param @param enabled （使能）
     * @return void
     * @throws
     * @Title: setCharacteristicNotification
     * @Description: TODO(设置特征值通变化通知)
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor clientConfig = characteristic
                .getDescriptor(UUID
                        .fromString("00002902-0000-1000-8000-00805f9b34fb"));

        if (enabled) {
            clientConfig
                    .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig
                    .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        mBluetoothGatt.writeDescriptor(clientConfig);

        Log.e(TAG, "setCharacteristicNotification");
    }

    /**
     * @param @param 无
     * @return void
     * @throws
     * @Title: getCharacteristicDescriptor
     * @Description: TODO(得到特征值下的描述值)
     */
    public void getCharacteristicDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readDescriptor(descriptor);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *            Characteristic to act on.
     * @param enabled
     *            If true, enable notification. False otherwise.
     */

    /**
     * @param @return 无
     * @return List<BluetoothGattService>
     * @throws
     * @Title: getSupportedGattServices
     * @Description: TODO(得到蓝牙的所有服务)
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}
