package com.project3c.flychess;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.project3c.flychess.brodcastreciver.WifiStateReciver;
import com.project3c.flychess.data.Cmd;
import com.project3c.flychess.data.LocalServerMap;
import com.project3c.flychess.data.Map;
import com.project3c.flychess.data.NetPlayer;
import com.project3c.flychess.data.Player;
import com.project3c.flychess.listener.DiceClickListener;
import com.project3c.flychess.view.PathNodeView;
import com.project3c.flychess.view.Tip;

import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import sensortag.Util;

/**
 * Created by like1 on 2017/5/1.
 */

public class GameActivity extends Activity {
    private Player my_player;
    private int order;
    private static int thld = 120;
    private int ran;

    private RelativeLayout relativeLayout;
    private Map map = null;
    private ImageView dice;
    public static ImageView[] flags;
    public static GameThread gameThread;
    private TextView[] names;
    private Tip tip;
    private Tip deputeTip, winTip;
    private TextView depute;
    private ImageView[] bot;
    private RelativeLayout first;
    private NetPlayer netPlayer;
    private TextView roomID;
    private TextView term;
    private DisplayMetrics dm;

    private static final String ARG_ADDRESS = "address";
    private String mAddress;
    private Calendar previousRead;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private BluetoothGattService mMovService;
    private BluetoothGattCharacteristic mRead, mEnable, mPeriod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("GameActivity", "create");
        netPlayer = RoomActivity.netPlayer;
        names = new TextView[4];
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        bot = new ImageView[4];

        depute = (TextView) findViewById(R.id.depute);
        bot[0] = (ImageView) findViewById(R.id.bot0);
        bot[1] = (ImageView) findViewById(R.id.bot1);
        bot[2] = (ImageView) findViewById(R.id.bot2);
        bot[3] = (ImageView) findViewById(R.id.bot3);
        flags = new ImageView[4];
        roomID = (TextView) findViewById(R.id.roomID);
        flags[0] = (ImageView) findViewById(R.id.pointer_0);
        flags[1] = (ImageView) findViewById(R.id.pointer_1);
        flags[2] = (ImageView) findViewById(R.id.pointer_2);
        flags[3] = (ImageView) findViewById(R.id.pointer_3);
        dm = new DisplayMetrics();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels / 15 - 2;
        tip = new Tip(this, "退出游戏", dm.widthPixels, dm.widthPixels / 2, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tip.dismiss();
                map.setPause(false);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tip.dismiss();
                map.setPause(false);
                finish();
            }
        });
        deputeTip = new Tip(this, "托管后将由Bot代替你。你将无法赶走Bot哦", dm.widthPixels, dm.widthPixels / 2, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deputeTip.dismiss();
                depute.setVisibility(View.VISIBLE);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deputeTip.dismiss();
                netPlayer.depute();
                findViewById(R.id.deputeOn).setVisibility(View.VISIBLE);
            }
        });
        relativeLayout = (RelativeLayout) findViewById(R.id.activity_main);
        first = (RelativeLayout) findViewById(R.id.tip);
        first.setVisibility(View.INVISIBLE);
        ViewGroup.LayoutParams layoutParams = null;
        dice = (ImageView) findViewById(R.id.dice);
        PathNodeView[] comViews = new PathNodeView[52];
        PathNodeView[] priViews = new PathNodeView[21];
        PathNodeView[] homeViews = new PathNodeView[20];
        names[0] = (TextView) findViewById(R.id.user0);
        names[1] = (TextView) findViewById(R.id.user1);
        names[2] = (TextView) findViewById(R.id.user2);
        names[3] = (TextView) findViewById(R.id.user3);
        for (int i = 0; i < 52; i++) {
            layoutParams = ((PathNodeView) relativeLayout.getChildAt(i)).getLayoutParams();
            layoutParams.height = width;
            layoutParams.width = width;
            PathNodeView child = (PathNodeView) relativeLayout.getChildAt(i);
            child.setLayoutParams(layoutParams);
            if (i < 4) {
                layoutParams = flags[i].getLayoutParams();
                layoutParams.height = width;
                layoutParams.width = width;
                flags[i].setLayoutParams(layoutParams);
                flags[i].setImageDrawable(null);
            }
            int j = i % 4;
            child.setImageDrawable(null);
            switch (j) {
                case 0:
                    child.setBackground(getResources().getDrawable(R.drawable.blackpos));
                    break;
                case 1:
                    child.setBackground(getResources().getDrawable(R.drawable.redpos));
                    break;
                case 2:
                    child.setBackground(getResources().getDrawable(R.drawable.rangepos));
                    break;
                case 3:
                    child.setBackground(getResources().getDrawable(R.drawable.greenpos));
                    break;
            }
            comViews[i] = child;
        }
        for (int i = 52; i < 73; i++) {
            PathNodeView child = (PathNodeView) relativeLayout.getChildAt(i);
            priViews[i - 52] = child;
            if (i == 72) break;
            layoutParams = ((PathNodeView) relativeLayout.getChildAt(i)).getLayoutParams();
            layoutParams.height = width;
            layoutParams.width = width;
            child.setLayoutParams(layoutParams);
            child.setImageDrawable(null);
        }
        for (int i = 73; i < 93; i++) {
            PathNodeView child = (PathNodeView) relativeLayout.getChildAt(i);
            homeViews[i - 73] = child;
            layoutParams = ((PathNodeView) relativeLayout.getChildAt(i)).getLayoutParams();
            layoutParams.height = width;
            layoutParams.width = width;
            child.setLayoutParams(layoutParams);
            child.setImageDrawable(null);
        }
        Intent i = getIntent();
        //map = new LocalServerMap(0,4,comViews,priViews,homeViews,getResources());
        int players = i.getIntExtra("players", 0);
        String[] snames = i.getStringArrayExtra("names");
        int mode = i.getIntExtra("mode", 0);
        int bots = i.getIntExtra("bot", 0);

        // get sensortag data
        mAddress = i.getStringExtra("address");
        Log.i("GameActivity", mAddress);
        assert mAddress != null;
        BluetoothManager manager = (BluetoothManager) GameActivity.this.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        if (mode == 0) {
            map = new Map(this, null, players, bots, comViews, priViews, homeViews, getResources(), names);
            map.startGame();
            roomID.setText("");
        } else if (mode == 1) {
            depute.setVisibility(View.VISIBLE);
            map = new LocalServerMap(this, netPlayer, players, bots, comViews, priViews, homeViews, getResources(), names, snames);
            if (netPlayer == null)
                roomID.setText("");
            else
                roomID.setText("房间ID:" + RoomActivity.getRoomID());
            depute.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (netPlayer != null) {
                        deputeTip.show(v.getRootView());
                        v.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
//        dice.setOnClickListener(new DiceClickListener(netPlayer));
        new Thread(new Mythread()).start();
    }

    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (order == 1) {
                order = 0;
                dice.setEnabled(false);
                if ((my_player = Map.getInstance().getCurPlayer()) != null) {
                    if (netPlayer != null) {
                        if (my_player.getUid() != netPlayer.getUid()) {
                            System.out.println("not your turn");
                            return;
                        }
                    }
                    if (!my_player.canTouch()) {
                        dice.setEnabled(true);
                        return;
                    }
                    if (!my_player.isCanDice() || !my_player.isFlyed()) {
                        if (!my_player.isFlyed()) {
                            System.out.println("not fly");
                        } else {
                            System.out.println("can not dice");
                        }
                        dice.setEnabled(true);
                        return;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            my_player.dice();
                        }
                    }).start();
                }
            }
            order = 0;
            super.handleMessage(msg);
        }
    };

    public class Mythread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(100);
                    if (mGatt != null && mRead != null) {
                        Log.d("Mythread", "trying to read");
                        mGatt.readCharacteristic(mRead);
                    }
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.exit();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (tip.isShowing()) {
                tip.dismiss();
                map.setPause(false);
            } else {
                tip.show(relativeLayout);
                map.setPause(true);
            }
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.setPause(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.setPause(false);
        Log.i("GameActivity", mAddress);
        connectDevice(mAddress);  // reconnect sensortag device
    }

    public ImageView getDice() {
        return dice;
    }

    public void showTip(String text) {
        System.out.println("show tip");
        first = (RelativeLayout) findViewById(R.id.tip);
        first.setVisibility(View.VISIBLE);
        TextView t = (TextView) first.findViewById(R.id.textView);
        t.setText(text);
        ImageView x = (ImageView) first.findViewById(R.id.imageView4);
        ImageView ok = (ImageView) first.findViewById(R.id.linearLayout);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                first.setVisibility(View.INVISIBLE);
                map.schedule();
            }
        };
        x.setOnClickListener(listener);
        ok.setOnClickListener(listener);
    }

    public void showBotView(int uid) {
        bot[uid].setVisibility(View.VISIBLE);
    }

    public void replay() {
        depute.setVisibility(View.INVISIBLE);
        findViewById(R.id.deputeOn).setVisibility(View.VISIBLE);
    }

    public void showGameOver(String s) {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                winTip.dismiss();
            }
        };
        winTip = new Tip(this, s, dm.widthPixels, dm.widthPixels / 2 + 50, listener, listener, 0);
        winTip.show(depute.getRootView());
    }

    /**
     * Creates a GATT connection to the given device.
     *
     * @param address String containing the address of the device
     */
    private void connectDevice(String address) {
        Log.i("connect", address);
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(GameActivity.this, R.string.state_off, Toast.LENGTH_SHORT).show();
            GameActivity.this.finish();
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mGatt = device.connectGatt(GameActivity.this, false, mCallback);
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
                Toast.makeText(GameActivity.this, R.string.service_not_found, Toast.LENGTH_LONG).show();
                GameActivity.this.finish();
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
                    Toast.makeText(GameActivity.this, R.string.service_not_found, Toast.LENGTH_LONG).show();
                    GameActivity.this.finish();
                }
                mPeriod.setValue(0x0A, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                mGatt.writeCharacteristic(mPeriod);
            } else if (characteristic == mPeriod) {
                // if setting sensor period was successful, start polling for sensor values
                mRead = mMovService.getCharacteristic(UUID.fromString("F000AA81-0451-4000-B000-000000000000"));
                if (mRead == null) {
//                    Toast.makeText(getActivity(), R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
//                    getActivity().finish();
                    Toast.makeText(GameActivity.this, R.string.characteristic_not_found, Toast.LENGTH_LONG).show();
                    GameActivity.this.finish();
                }
                previousRead = Calendar.getInstance();
//                mGatt.readCharacteristic(mRead);
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
            if (result[0] < 0) result[0] = -0 - result[0];
            if (result[1] < 0) result[1] = -0 - result[1];
            if (result[2] < 0) result[2] = -0 - result[2];
            if (result[0] > thld || result[1] > thld || result[2] > thld) {
                order = 1;
                if (result[0] > result[1] && result[0] > result[2]) {
                    ran = (int) result[0];
                } else if (result[1] > result[2]) {
                    ran = (int) result[1];
                } else {
                    ran = (int) result[2];
                }
            }
            previousRead = Calendar.getInstance();
//            mGatt.readCharacteristic(mRead);
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
