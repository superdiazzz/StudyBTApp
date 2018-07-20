package com.example.dev3.testbtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final int REQ_BLUETOOTH_ENABLE = 1;


    private Button onOffBtn;
    private ListView lvDevice;
    private ListView lvPair;
    private ToggleButton tgBtn;

    private BluetoothAdapter btAdapter;
    private volatile boolean isBtOn = false;
    private ArrayList<BluetoothDevice> btDevices;
    private ArrayList<BluetoothDevice> btPairDevices;
    private DeviceListAdapter deviceListAdapter;
    private DevicePairListAdapter devicePairListAdapter;


    // discover device
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                // ambil device
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state){

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive: STATE TURINING OFF");
                        break;

                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: STATE ON");
                        break;


                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: STATE TURINING ON");
                        break;
                }
            }

        }
    };
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION_FOUND");

            if(action.equals(BluetoothDevice.ACTION_FOUND)){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btDevices.add(device);
                Log.d(TAG, "onReceive: name " + device.getName() + ", addr " + device.getAddress());

                deviceListAdapter.notifyDataSetChanged();
            }



        }
    };
    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){

                BluetoothDevice btDev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(btDev.getBondState() == BluetoothDevice.BOND_BONDED){

                    Log.d(TAG, "onReceive: BOND_BONDED");

                    btPairDevices.add(btDev);
                    devicePairListAdapter.notifyDataSetChanged();

                }else if(btDev.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "onReceive: BOND_BONDIND");

                }else if(btDev.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "onReceive: BOND_NONE");
                }

            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //onOffBtn = (Button) findViewById(R.id.on_off);
        tgBtn = findViewById(R.id.tgBtn);
        lvDevice = findViewById(R.id.lsDevice);
        lvPair = findViewById(R.id.pair_list);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btDevices = new ArrayList<>();
        btPairDevices = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(getApplicationContext(), R.layout.list_device, btDevices);
        devicePairListAdapter = new DevicePairListAdapter(getApplicationContext(), R.layout.list_pair_device, btPairDevices);

        lvDevice.setAdapter(deviceListAdapter);
        lvPair.setAdapter(devicePairListAdapter);




        IntentFilter intFilterBondstate = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, intFilterBondstate);


        tgBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){

                    if(btAdapter == null){
                        Toast.makeText(MainActivity.this, "This device not supported bluetooth", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Toast.makeText(MainActivity.this, "BT " + isChecked, Toast.LENGTH_SHORT).show();
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, REQ_BLUETOOTH_ENABLE);

                    IntentFilter btFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                    registerReceiver(mBroadcastReceiver, btFilter);


                }else{

                    btAdapter.cancelDiscovery();
                    btAdapter.disable();
                    //Toast.makeText(MainActivity.this, "BT " + isChecked, Toast.LENGTH_SHORT).show();

                }

            }
        });


        lvDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // when clicked try to stop discovery, it consume much memory
                btAdapter.cancelDiscovery();

                BluetoothDevice btDev = btDevices.get(position);
                Toast.makeText(MainActivity.this, "name " + btDev.getName(), Toast.LENGTH_SHORT).show();


                // create BOND (pair)
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                    Log.d(TAG, "trying to pair with " + btDev.getName());
                    btDev.createBond();

                }

            }
        });

    }

    public void discoveryClicked(View v){

        // looking for unpair devices

        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
            Log.d(TAG, "canceling discovery ");

            if(!deviceListAdapter.isEmpty()){
                deviceListAdapter.clear();
            }

            checkBTPermissions();

            btAdapter.startDiscovery();
            IntentFilter discoveryIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoveryIntent);

        }

        if(!btAdapter.isDiscovering()){

            checkBTPermissions();

            btAdapter.startDiscovery();
            IntentFilter discoveryIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoveryIntent);

        }

    }

    public void scanClicked(View v){

    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);




    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // destroy all broadcastreceiver
        if(mBroadcastReceiver != null){
            unregisterReceiver(mBroadcastReceiver);

        }
        if(mBroadcastReceiver3 != null){
            unregisterReceiver(mBroadcastReceiver3);

        }
        if(mBroadcastReceiver4 != null){
            unregisterReceiver(mBroadcastReceiver4);

        }

    }


    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     *
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

}
