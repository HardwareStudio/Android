package com.binaron.hardware_studio.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Eric on 2015/6/23.
 */
public class MainActivity extends Activity{
    final static String LOG_TAG = "MainActivity";
    final static int REQUEST_ENABLE_BT = 1;
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    public static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private BluetoothAdapter bluetoothAdapter;
    private DeviceAdapter deviceAdapter;
    private int mState;
    private AcceptThread acceptThread;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_main);
        deviceAdapter = new DeviceAdapter(this, R.layout.layout_activity_main_row);
        mState = STATE_NONE;
        ListView listView = (ListView)findViewById(R.id.main_list_view);
        listView.setAdapter(deviceAdapter);

        // check if bluetooth is supported by the device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            Toast.makeText(this, "bluetooth is not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        // check if bluetooth is enable
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

//        bluetoothDevice = bluetoothAdapter.getRemoteDevice("98:D3:31:B1:37:04");
//        try {
//            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MainActivity.MY_UUID_SECURE);
//            bluetoothSocket.connect();
//            Toast.makeText(this, "connect success", Toast.LENGTH_SHORT).show();
//            outputStream = bluetoothSocket.getOutputStream();
//        }
//        catch (Exception e){
//            Log.d(LOG_TAG, e.toString());
//        }

        acceptThread = new AcceptThread(false);
        acceptThread.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
        // get bluetooth device
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceAdapter.clear();
        for (BluetoothDevice bluetoothDevice : pairedDevices){
            deviceAdapter.add(bluetoothDevice);
        }
    }

    class DeviceAdapter extends ArrayAdapter<BluetoothDevice>{

        class ViewHolder{
            public TextView name;
            public TextView mac;
        }

        public DeviceAdapter(Context context, int resource){
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_activity_main_row, null);
                viewHolder = new ViewHolder();
                viewHolder.name = (TextView)convertView.findViewById(R.id.main_list_item_name);
                viewHolder.mac = (TextView)convertView.findViewById(R.id.main_list_item_mac);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)convertView.getTag();
            }

            final BluetoothDevice device = getItem(position);
            viewHolder.name.setText(device.getName());
            viewHolder.mac.setText(device.getAddress());

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setClass(v.getContext(), ConnectActivity.class);
                    intent.putExtra("mac", device.getAddress());
                    v.getContext().startActivity(intent);
                }
            });

            return convertView;
        }
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                } else {
                    tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(LOG_TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread " + mSocketType);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                Log.d(LOG_TAG, "run");
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                    Log.d(LOG_TAG, "Socket Type: " + mSocketType + "accept() success");
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
//                            connected(socket, socket.getRemoteDevice(), mSocketType);
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(LOG_TAG, "Could not close unwanted socket", e);
                            }
                            break;
                    }
                }
            }
            Log.i(LOG_TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            Log.d(LOG_TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }
}
