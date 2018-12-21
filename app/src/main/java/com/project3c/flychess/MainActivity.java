package com.project3c.flychess;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.project3c.flychess.data.Map;
import com.project3c.flychess.data.NetPlayer;
import com.project3c.flychess.sqlite.GameDatabase;
import com.project3c.flychess.view.Tip;

import sensortag.Util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;

import local.server.LocalServer;

public class MainActivity extends Activity {
    public static String playerName = null;
    private static Context context;
    private static MainActivity instance;
    private ImageView local, localServer, server;
    static LocalServer l;
    private ImageView music;
    private ImageView about;
    private ImageView replay;
    private Tip tip;
    private Tip exitTip;
    private TextView[] views;
    private PopupWindow gameSet;

    private PopupWindow localChooser;
    private ImageView create;
    private ImageView join;

    private EditText editText;
    private ImageView ok;
    private TextView tName;

    private RelativeLayout inputName;

    private LinearLayout main;

    // sensortag part
    private static final String ARG_ADDRESS = "address";
    private String mAddress;
    private Calendar previousRead;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattService mMovService;
    private BluetoothGattCharacteristic mRead, mEnable, mPeriod;

    View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == local) {
                startLocalGame(4);
            } else if (v == localServer) {
                startLocalServerGame();
            } else {
                startServerGame();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getApplicationContext();
        instance = this;
        super.onCreate(savedInstanceState);

        // initialize bluetooth manager & adapter
        Intent intent = getIntent();
        mAddress = intent.getStringExtra(ARG_ADDRESS);
        BluetoothManager manager = (BluetoothManager) MainActivity.this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.game_mode);
        replay = (ImageView) findViewById(R.id.replay);
        replay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,ReplayActivity.class);
                startActivity(intent);
            }
        });

        editText = (EditText) findViewById(R.id.editText);
        editText.requestFocus();
        main = (LinearLayout) findViewById(R.id.main);
        main.setVisibility(View.INVISIBLE);
        tName = (TextView) findViewById(R.id.textView2);
        inputName = (RelativeLayout) findViewById(R.id.input_name);
        ok = (ImageView) findViewById(R.id.imageView4);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editText.getText().toString();
                if (name.equals("")||name.equals(" ")) {
                    Toast.makeText(getContext(),"你并没有输入", Toast.LENGTH_SHORT).show();
                } else {
                    tName.setText(name);
                    inputName.setVisibility(View.INVISIBLE);
                    LinearLayout linearLayout = (LinearLayout) inputName.getParent();
                    linearLayout.removeView(inputName);
                    main.setVisibility(View.VISIBLE);
                    playerName = name;
                }
            }
        });
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getSystemService("window")).getDefaultDisplay().getMetrics(dm);  // TODO why
        View.OnClickListener unl = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tip.dismiss();
            }
        };
        tip = new Tip(this, "code : like1\npicture : Hong\nAI : haha\nnet : FFlover", dm.widthPixels, dm.widthPixels / 2 + 260, unl, unl);
        exitTip = new Tip(this, "退出", dm.widthPixels, dm.widthPixels / 2, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitTip.dismiss();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitTip.dismiss();
                finish();
            }
        });
        gameSet = new PopupWindow(getLayoutInflater().inflate(R.layout.game_setting, null), dm.widthPixels, dm.widthPixels + 100);
        localChooser = new PopupWindow(getLayoutInflater().inflate(R.layout.local_server_join_or_create, null),
                dm.widthPixels, dm.widthPixels / 2);
        create = (ImageView) localChooser.getContentView().findViewById(R.id.create);
        join = (ImageView) localChooser.getContentView().findViewById(R.id.join);
        views = new TextView[9];
        views[0] = (TextView) gameSet.getContentView().findViewById(R.id.bot1);
        final Drawable back = getResources().getDrawable(R.drawable.ok);
        views[1] = (TextView) gameSet.getContentView().findViewById(R.id.open1);
        views[2] = (TextView) gameSet.getContentView().findViewById(R.id.no1);

        views[3] = (TextView) gameSet.getContentView().findViewById(R.id.bot2);
        views[4] = (TextView) gameSet.getContentView().findViewById(R.id.open2);
        views[5] = (TextView) gameSet.getContentView().findViewById(R.id.no2);

        views[6] = (TextView) gameSet.getContentView().findViewById(R.id.bot3);
        views[7] = (TextView) gameSet.getContentView().findViewById(R.id.open3);
        views[8] = (TextView) gameSet.getContentView().findViewById(R.id.no3);

        ImageView start = (ImageView) gameSet.getContentView().findViewById(R.id.start);

        View.OnClickListener setListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = v.getId();
                for (int j = i / 3 * 3; j < i / 3 * 3 + 3; j++) {
                    if (views[j] == v) {
                        views[j].setBackground(back);
                    } else {
                        views[j].setBackground(null);
                    }
                }
            }
        };
        for (int i = 0; i < views.length; i++) {
            views[i].setId(i);
            if (i % 3 != 1)
                views[i].setBackground(null);
            views[i].setOnClickListener(setListener);
        }
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameSet.dismiss();
                int bots = 0, players = 0;
                for (int i = 0; i < views.length; i++) {
                    if (i % 3 == 0 && views[i].getBackground() != null)
                        bots++;
                    else if (i % 3 == 1 && views[i].getBackground() != null)
                        players++;
                }
                Intent i = new Intent();
                i.setClass(getContext(), GameActivity.class);
                i.putExtra("mode", 0);
                i.putExtra("players", players + 1);
                i.putExtra("bot", bots);
                startActivity(i);
            }
        });
        music = (ImageView) findViewById(R.id.music);
        about = (ImageView) findViewById(R.id.about);
        local = (ImageView) findViewById(R.id.local);
        localServer = (ImageView) findViewById(R.id.local_server);
        server = (ImageView) findViewById(R.id.server);
        local.setOnClickListener(listener);
        localServer.setOnClickListener(listener);
        server.setOnClickListener(listener);
        music.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                if (Map.isMusic()) {
                    Map.noMusic();
                    music.setBackground(getResources().getDrawable(R.drawable.nomusic));
                } else {
                    Map.openMusic();
                    music.setBackground(null);
                }
            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tip.isShowing()) {
                    tip.dismiss();
                } else {
                    tip.show(about.getRootView());
                }
            }
        });
    }

    /**
     * Called when the activity is visible to the user and actively running.
     */
    protected void onResume() {
        super.onResume();
        connectDevice(mAddress);
    }

    private void startLocalGame(int players) {  // TODO
        Drawable back = getResources().getDrawable(R.drawable.ok);
        Log.i("MainActivity", "start local game");
        for (int i = 0; i < views.length; i++) {
            if (i % 3 == 1)
                views[i].setBackground(back);
            else
                views[i].setBackground(null);
        }
        gameSet.showAtLocation(about.getRootView(), Gravity.CENTER, 0, 0);
    }

    private void startLocalServerGame() {
        localChooser.showAtLocation(about.getRootView(), Gravity.CENTER, 0, 0);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LocalServer.getLocalNetAddress() == null) {
                    Toast.makeText(MainActivity.this, "你并没有加入任何WIFI，请和你的好友加入同一个WIFI。移动热点通常是一个好选择", Toast.LENGTH_SHORT).show();
                    return;
                }
                localChooser.dismiss();
                Intent i = new Intent();
                if (v == create) {
                    createLocalServer(i,MainActivity.this);
                }else {
                    i.setClass(MainActivity.this, LocalServerGameActivity.class);
                }
                startActivity(i);
            }
        };
        create.setOnClickListener(listener);
        join.setOnClickListener(listener);
        /*Intent i = new Intent();
        i.setClass(this, LocalServerGameActivity.class);
        startActivity(i);*/
    }

    public void createLocalServer(Intent i,Context c) {
        l = new LocalServer();
        l.start();
        i.setClass(c, RoomActivity.class);
        RoomActivity.setLocalServer(new com.project3c.flychess.server.LocalServer(l.getLocalAddress(),4,1,""));
    }

    private void startServerGame() {
        Intent i = new Intent();
        i.setClass(this,ServerGameActivity.class);
        startActivity(i);
        //
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (localChooser.isShowing()) {
                localChooser.dismiss();
                return true;
            }
            if (tip.isShowing()) {
                tip.dismiss();
                return true;
            }
            if (exitTip.isShowing()) {
                exitTip.dismiss();
                return true;
            }
            if (gameSet.isShowing()) {
                gameSet.dismiss();
                return true;
            }
            exitTip.show(about.getRootView());
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        System.out.println("SensorTagActivity destroyed");
        instance = null;
        super.onDestroy();
    }

    public static Context getContext() {
        return context;
    }

    private boolean openWIFIhotPot() {
        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiConfiguration apConfig = new WifiConfiguration();
        apConfig.SSID = "like1";
        apConfig.preSharedKey = "123456789";
        apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        Method method = null;
        try {
            method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enabled = true;
            final Boolean invoke = (Boolean) method.invoke(wifiManager, apConfig, enabled);
            return invoke;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Activity getInstance() {
        return instance;
    }



    /**
     * Creates a GATT connection to the given device.
     *
     * @param address String containing the address of the device
     */
    private void connectDevice(String address) {
        if (!mBluetoothAdapter.isEnabled()) {
//            Toast.makeText(getActivity(), R.string.state_off, Toast.LENGTH_SHORT).show();
//            getActivity().finish();
            Toast.makeText(MainActivity.this, R.string.state_off, Toast.LENGTH_SHORT).show();
            MainActivity.this.finish();
        }
//        mListener.onShowProgress();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        mGatt = device.connectGatt(getActivity(), false, mCallback);
        mGatt = device.connectGatt(MainActivity.this, false, mCallback);
    }

    private BluetoothGattCallback mCallback = new BluetoothGattCallback() {
        double result[];
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault());

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    // as soon as we're connected, discover services
                    mGatt.discoverServices();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // as soon as services are discovered, acquire characteristic and try enabling
            mMovService = mGatt.getService(UUID.fromString("F000AA80-0451-4000-B000-000000000000"));
            mEnable = mMovService.getCharacteristic(UUID.fromString("F000AA82-0451-4000-B000-000000000000"));
            if (mEnable == null) {
//                Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
//                getActivity().finish();
                Toast.makeText(MainActivity.this, R.string.service_not_found, Toast.LENGTH_LONG).show();
                MainActivity.this.finish();
            }
            /*
             * Bits starting with the least significant bit (the rightmost one)
             * 0       Gyroscope z axis enable
             * 1       Gyroscope y axis enable
             * 2       Gyroscope x axis enable
             * 3       Accelerometer z axis enable
             * 4       Accelerometer y axis enable
             * 5       Accelerometer x axis enable
             * 6       Magnetometer enable (all axes)
             * 7       Wake-On-Motion Enable
             * 8:9	    Accelerometer range (0=2G, 1=4G, 2=8G, 3=16G)
             * 10:15   Not used
             */
            mEnable.setValue(0b1001111111, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            mGatt.writeCharacteristic(mEnable);
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         *
         * <p>If this callback is invoked while a reliable write transaction is
         * in progress, the value of the characteristic represents the value
         * reported by the remote device. An application should compare this
         * value to the desired value to be written. If the values don't match,
         * the application must abort the reliable write transaction.
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#writeCharacteristic}
         * @param characteristic Characteristic that was written to the associated
         *                       remote device.
         * @param status The result of the write operation
         *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (characteristic == mEnable) {
                // if enable was successful, set the sensor period to the lowest value
                mPeriod = mMovService.getCharacteristic(UUID.fromString("F000AA83-0451-4000-B000-000000000000"));
                if (mPeriod == null) {
//                    Toast.makeText(getActivity(), R.string.service_not_found, Toast.LENGTH_LONG).show();
//                    getActivity().finish();
                    Toast.makeText(MainActivity.this, R.string.service_not_found, Toast.LENGTH_LONG).show();
                    MainActivity.this.finish();
                }
                mPeriod.setValue(0x0A, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                mGatt.writeCharacteristic(mPeriod);
            } else if (characteristic == mPeriod) {
                // if setting sensor period was successful, start polling for sensor values
                mRead = mMovService.getCharacteristic(UUID.fromString("F000AA81-0451-4000-B000-000000000000"));
                if (mRead == null) {
//                    Toast.makeText(getActivity(), R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
//                    getActivity().finish();
                    Toast.makeText(MainActivity.this, R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
                    MainActivity.this.finish();
                }
                previousRead = Calendar.getInstance();
                mGatt.readCharacteristic(mRead);
                deviceConnected();
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated
         *                       remote device.
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         *               was completed successfully.
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            // convert raw byte array to G unit values for xyz axes
            result = Util.convertAccel(characteristic.getValue());  // TODO figure out how this work
            Log.i("Acceleration x", Double.toString(result[0]));
            Log.i("Acceleration y", Double.toString(result[1]));
            Log.i("Acceleration z", Double.toString(result[2]));
            previousRead = Calendar.getInstance();
            mGatt.readCharacteristic(mRead);
        }
    };


    /**
     * Called when the device has been fully connected.
     */
    private void deviceConnected() {
        // start connection watcher thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasConnection = true;
                while (hasConnection) {
                    long diff = Calendar.getInstance().getTimeInMillis() - previousRead.getTimeInMillis();
                    if (diff > 2000) {  // no reacts in 2 seconds -> lose connection
                        hasConnection = false;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Called when the device should be disconnected.
     */
    private void deviceDisconnected() {
        if (mGatt != null) mGatt.disconnect();
    }
}
