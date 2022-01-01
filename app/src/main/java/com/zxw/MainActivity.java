package com.zxw;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zxw.toolkit.BluetoothMessage;
import com.zxw.toolkit.ParseLeAdvData;
import com.zxw.toolkit.UIHandler;
import com.zxw.ui.BasActivity;
import com.zxw.ui.Ble_Activity;
import com.zxw.ui.DebugActivity;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * @author
 * @Description: TODO<MainActivity类实现打开蓝牙 、 扫描蓝牙>
 * @data:
 * @version:
 */
public class MainActivity extends BasActivity implements OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION_ACCESS = 1000;

    private long mCreateTime;

    // 扫描蓝牙按钮
    private Button scan_btn;
    // 蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter;

    // 1以上的扫描回调
    private ScanCallback mScanCallback;

    // 扫描装置
    private BluetoothLeScanner mBluetoothLeScanner;

    // 蓝牙信号强度
    private ArrayList<Integer> rssis;

    // 自定义Adapter
    LeDeviceListAdapter mleDeviceListAdapter;

    // listView 显示扫描到的蓝牙信息
    ListView listView;

    // 描述扫描蓝牙的状态
    private boolean mScanning;
    private boolean scan_flag;
    private Handler mHandler;
    int REQUEST_ENABLE_BT = 1;

    // 蓝牙扫描时间
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCreateTime = System.currentTimeMillis();

        // 初始化控件
        init();

        // 申请位置权限
        initPermissions();
    }


    /**
     * @param
     * @return void
     * @throws
     * @Title: init
     * @Description: TODO(初始化UI控件)
     */
    private void init() {
        scan_btn = findViewById(R.id.scan_dev_btn);
        scan_btn.setOnClickListener(this);
        listView = findViewById(R.id.lv);

        // 自定义适配器
        mleDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mleDeviceListAdapter);

        /* listView 点击函数 */
        listView.setOnItemClickListener((arg0, v, position, id) -> {
            // TODO Auto-generated method stub
            final BluetoothMessage bluetoothMessage = mleDeviceListAdapter.getDevice(position);
            if (bluetoothMessage == null) {
                return;
            }

            final Intent intent = new Intent(MainActivity.this, Ble_Activity.class);
            intent.putExtra(Ble_Activity.EXTRAS_DEVICE_NAME, bluetoothMessage.getName() != null ? bluetoothMessage.getName() : bluetoothMessage.getDevice().getName());
            intent.putExtra(Ble_Activity.EXTRAS_DEVICE_ADDRESS, bluetoothMessage.getDevice().getAddress());
            intent.putExtra(Ble_Activity.EXTRAS_DEVICE_RSSI, rssis.get(position).toString());

            if (mScanning) {
                /* 停止扫描设备 */
                mBluetoothLeScanner.stopScan(mScanCallback);
                mScanning = false;
            }

            try {
                // 启动Ble_Activity
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: handle exception
            }
        });

        scan_btn.setOnLongClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DebugActivity.class));
            return true;
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (MY_PERMISSIONS_REQUEST_LOCATION_ACCESS == requestCode) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                delayShowView();
                return;
            }

            if (grantResults.length > 0 && grantResults[0] == PERMISSION_DENIED) {
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
                if (!showRationale) {
                    // user also CHECKED "never ask again"
                    // you can either enable some fall back,
                    // disable features of your app
                    // or open another dialog explaining
                    // again the permission and directing to
                    // the app setting
                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setIcon(R.mipmap.ic_launcher)
                            .setTitle("请开启定位权限")
                            .setPositiveButton(R.string.ok, (dialog, which) -> initPermissions())
                            .create();
                    alertDialog.setCancelable(false);
                    alertDialog.show();
                    return;
                }
            }
            finish();
        }
    }

    /**
     * 权限申请
     */
    private void initPermissions() {
        int value = ContextCompat.checkSelfPermission(this, Manifest.permission_group.LOCATION);
        if (PERMISSION_GRANTED == value) {
            delayShowView();
            return;
        }
        // 获取wifi连接需要定位权限, 没有获取权限
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
        }, MY_PERMISSIONS_REQUEST_LOCATION_ACCESS);
    }

    // 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
    public static boolean isOpenGPS(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    private void onHasPermission() {

        // 初始化蓝牙
        init_ble();

        // 设置回调
        setScanCallBack();

        scan_flag = true;

        // 淡入淡出
        View cover = findViewById(R.id.image_cover);
        cover.animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }
            @Override
            public void onAnimationEnd(Animator animation) {
                cover.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                cover.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }


    private void delayShowView() {
        long delay = (System.currentTimeMillis() - mCreateTime) > 1000 ? 0 : 1000;
        UIHandler.postDelayed(this::onHasPermission, delay);
    }


    /**
     * @param
     * @return void
     * @throws
     * @Title: init_ble
     * @Description: TODO(初始化蓝牙)
     */
    private void init_ble() {
        // 手机硬件支持蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes Bluetooth adapter.
        // 获取手机本地的蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // 打开蓝牙权限
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    /*
     * 按钮响应事件
     */
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        if (!isOpenGPS(this)) {
            new AlertDialog
                    .Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setTitle("提示")
                    .setMessage("请前往打开手机的位置权限!")
                    .setCancelable(false)
                    .setPositiveButton("确定", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 10);
                    }).show();
            return;
        }

        if (scan_flag) {
            mleDeviceListAdapter = new LeDeviceListAdapter();
            listView.setAdapter(mleDeviceListAdapter);
            scanLeDevice(true);
        } else {
            scanLeDevice(false);
            scan_btn.setText("扫描设备");
        }
    }


    /**
     * @param enable (扫描使能，true:扫描开始,false:扫描停止)
     * @return void
     * @throws
     * @Title: scanLeDevice
     * @Description: TODO(扫描蓝牙设备)
     */
    private void scanLeDevice(final boolean enable) {

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        if (mBluetoothLeScanner == null) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            UIHandler.postDelayed(() -> {
                mScanning = false;
                scan_flag = true;
                scan_btn.setText("扫描设备");
                Log.i("SCAN", "stop.....................");
                mBluetoothLeScanner.stopScan(mScanCallback);
            }, SCAN_PERIOD);

            /* 开始扫描蓝牙设备，带mLeScanCallback 回调函数 */
            Log.i("SCAN", "begin.....................");
            mScanning = true;
            scan_flag = false;
            scan_btn.setText("停止扫描");
            mBluetoothLeScanner.startScan(mScanCallback);

        } else {
            Log.i("Stop", "stoping................");
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            scan_flag = true;
        }
    }


    private void setScanCallBack() {
        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                final BluetoothDevice device = result.getDevice();
                final BluetoothMessage bluetoothMessage = new BluetoothMessage(device);
                if (null != device && null != result.getScanRecord()) {
                    try {
                        if (device.getName() != null) {
                            byte[] name = ParseLeAdvData.adv_report_parse(ParseLeAdvData.BLE_GAP_AD_TYPE_COMPLETE_LOCAL_NAME, result.getScanRecord().getBytes());
                            if (name != null)
                                bluetoothMessage.setName(new String(name, "GBK"));
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(() -> {
                    /* 讲扫描到设备的信息输出到listview的适配器 */
                    mleDeviceListAdapter.addDevice(bluetoothMessage, result.getRssi());
                    mleDeviceListAdapter.notifyDataSetChanged();
                });

            }

            @Override
            public void onScanFailed(final int errorCode) {
                super.onScanFailed(errorCode);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "扫描出错:" + errorCode, Toast.LENGTH_SHORT).show());
            }
        };
    }


    /**
     * @author
     * @Description: TODO<自定义适配器Adapter, 作为listview的适配器>
     * @data:
     * @version:
     */
    private class LeDeviceListAdapter extends BaseAdapter {

        private ArrayList<BluetoothMessage> mLeDevices;

        public LeDeviceListAdapter() {
            super();
            rssis = new ArrayList<>();
            mLeDevices = new ArrayList<>();
        }

        public void addDevice(BluetoothMessage device, int rssi) {

            for (BluetoothMessage mLeDevice : mLeDevices) {
                if (mLeDevice.getDevice().getAddress().equals(device.getDevice().getAddress())) {
                    return;
                }
            }
            mLeDevices.add(device);
            rssis.add(rssi);
        }

        public BluetoothMessage getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            rssis.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        /**
         * 重写getview
         **/
        @SuppressLint({"ViewHolder", "SetTextI18n"})
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            // General ListView optimization code.
            // 加载list view每一项的视图
            view = getLayoutInflater().inflate(R.layout.listitem, null);

            // 初始化三个text view显示蓝牙信息
            TextView deviceAddress = view.findViewById(R.id.tv_deviceAddr);
            TextView deviceName = view.findViewById(R.id.tv_deviceName);
            TextView rssi = view.findViewById(R.id.tv_rssi);

            BluetoothMessage bluetoothMessage = mLeDevices.get(i);
            deviceAddress.setText(bluetoothMessage.getDevice().getAddress());
            deviceName.setText(bluetoothMessage.getName() != null ? bluetoothMessage.getName() : bluetoothMessage.getDevice().getName());
            rssi.setText("" + rssis.get(i));
            return view;
        }
    }
}