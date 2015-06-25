package com.binaron.hardware_studio.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Eric on 2015/6/24.
 */
public class ConnectActivity extends Activity{
    final static private String LOG_TAG = "ConnectActivity";

    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private Button sendButton;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_connect);
        sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] message = {49};
                try {
                    outputStream.write(message);
                }
                catch (Exception e){
                    Log.d(LOG_TAG, e.toString());
                }
            }
        });

        Intent intent = getIntent();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
//        for (BluetoothDevice bd : pairedDevices){
//            if(bd.getAddress().equals(intent.getStringExtra("mac"))){
//                bluetoothDevice = bd;
//                break;
//            }
//        }
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(intent.getStringExtra("mac"));
        Log.d(LOG_TAG, intent.getStringExtra("mac"));

        try {
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(MainActivity.MY_UUID_INSECURE);
            bluetoothSocket.connect();
            Toast.makeText(this, "connect success", Toast.LENGTH_SHORT).show();
            outputStream = bluetoothSocket.getOutputStream();
        }
        catch (Exception e){
            Log.d(LOG_TAG, e.toString());
        }
    }
}
