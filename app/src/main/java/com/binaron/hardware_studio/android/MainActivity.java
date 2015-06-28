package com.binaron.hardware_studio.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Eric on 2015/6/23.
 */
public class MainActivity extends Activity {
    final static public UUID ArduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final static String LOG_TAG = "MainActivity";
    final static int REQUEST_ENABLE_BT = 1;
    final static int STOP = 48;
    final static int FORWARD = 49;
    final static int BACK = 50;
    final static int LEFT = 51;
    final static int RIGHT = 52;

    private int lastCommand = STOP;
    private int command = STOP;
    private boolean go = false;
    private TextView information;
    private ImageButton goButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private SensorManager sensorManager;
    private Sensor sensor;
    private OutputStream outputStream;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_activity_main);
        information = (TextView) findViewById(R.id.information);
        goButton = (ImageButton) findViewById(R.id.go);
        goButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        go = true;
                        //Log.d("MotionEvent", "ACTION_DOWN");
                        break;
                    case MotionEvent.ACTION_UP:
                        if(bluetoothSocket.isConnected()) {
                            try {
                                command = STOP;
                                outputStream.write(command);
                                lastCommand = command;
                                Log.d("OutputStream", "send command : " + command);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        go = false;
                        //Log.d("MotionEvent", "ACTION_UP");
                        break;
                }
                return false;
            }
        });

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

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    @Override
    protected  void onResume(){
        connect();
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onStop(){
        sensorManager.unregisterListener(sensorEventListener);
        disconnect();
        super.onStop();
    }

    private void connect(){
        try {
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice("98:D3:31:B1:37:04");
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(ArduinoUUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "connect success", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            Toast.makeText(this, "connect fail", Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, e.toString());
        }
    }

    private void disconnect(){
        try {
            if (bluetoothSocket.isConnected()) {
                outputStream.write(STOP);
                bluetoothSocket.close();
                Log.d(LOG_TAG, "connect close");
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, e.toString());
        }
    }

    final SensorEventListener sensorEventListener = new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event){
            if(event.sensor.getType() == Sensor.TYPE_GRAVITY){
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                information.setText(Float.toString(axisX)+"\n"+Float.toString(axisY)+"\n"+Float.toString(axisZ)+"\n");
                if(axisY < -5){
                    command = LEFT;
                }
                else if(axisY > 5){
                    command = RIGHT;
                }
                else if(axisZ > 6){
                    command = FORWARD;
                }
                else if(axisZ < 5){
                    command = BACK;
                }
                else {
                    command = STOP;
                }

                if(bluetoothSocket.isConnected()) {
                    try {
                        if (go) {
                            if(command != lastCommand) {
                                outputStream.write(command);
                                lastCommand = command;
                                Log.d("OutputStream", "send command : " + command);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor , int accuracy){
            //Log.i("Sensor", "onAccuracyChanged");
        }
    };

}
