package com.example.munkit.mqttsend;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.view.View;
import android.widget.Button;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;





public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private MqttAndroidClient mqttAndroidClient;
    private final String serverUri = "tcp://iot.eclipse.org:1883";
    private final String clientId = "myAndClient";
    private static final String TAG = "Mymessage";
    private final String pubchannel = "RoboticArm/message";
    private final String subchannel = "RoboticArm/respond";
    //private boolean transition = true;
    private boolean terminate_tran = true;

    //sensor variable
    private float[] gravity = {0,0,-9.81f};

    private float[] linear_acceleration = new float[3];

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    //speed and distance calculation
    private long lasttime = System.currentTimeMillis();
    private float lastspeedX = 0;
    private float lastspeedZ = 0;
    private float lastspeedY = 0;
    private float accdisX = 0;
    private float accdisY = 0;
    private float accdisZ = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Log.i(TAG,"Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    //subscribeToTopic(subchannel);
                } else {
                    Log.i(TAG,"Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG,"The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.i(TAG,"Incoming message: " + new String(message.getPayload()));
                //set transition
                //transition = true;

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    //subscribeToTopic(subchannel);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG,"Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
        //accelerometer command
        lasttime = System.currentTimeMillis();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        //button
        final Button button1 = (Button)findViewById(R.id.button);

        button1.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        //click to stop publish message
                        terminate_tran = !terminate_tran;
                        if (terminate_tran) {
                            button1.setText("start");

                        }
                        else
                        {
                            button1.setText("stop");
                            accdisX = 0;
                            accdisY = 0;
                            accdisZ = 0;
                        }

                    }
                });

    }

    public void subscribeToTopic(String subscriptionTopic){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG,"Subscribed!");

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG,"Failed to subscribe");
                }
            });

        } catch (MqttException ex){
            Log.i(TAG,"Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(String Channel, String pubmessage){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(pubmessage.getBytes());
            mqttAndroidClient.publish(Channel, message);
            if(!mqttAndroidClient.isConnected()){
                Log.i(TAG,mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            Log.i(TAG,"Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //accelerometer event
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event)
    {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        final float alpha = 0.8f;
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];
            /*
            linear_acceleration[0] = event.values[0];
            linear_acceleration[1] = event.values[1];
            linear_acceleration[2] = event.values[2];
*/
            long curtime = System.currentTimeMillis();
            long difftime = curtime - lasttime;

            float velocityX = linear_acceleration[0]*difftime/1000 + lastspeedX;
            float velocityY = linear_acceleration[1]*difftime/1000 + lastspeedY;
            float velocityZ = linear_acceleration[2]*difftime/1000 + lastspeedZ;
            //calculate distance
            float disX = (velocityX*velocityX - lastspeedX*lastspeedX)/2/linear_acceleration[0];
            float disY = (velocityY*velocityY - lastspeedY*lastspeedY)/2/linear_acceleration[1];
            float disZ = (velocityZ*velocityZ - lastspeedZ*lastspeedZ)/2/linear_acceleration[2];
            //accumlate distance
            accdisX += disX;
            accdisY += disY;
            accdisZ += disZ;

            lasttime = curtime;
            lastspeedX = velocityX;
            lastspeedY = velocityY;
            lastspeedZ = velocityZ;
            //if ((transition == true) && (terminate_tran == false))
            if (!terminate_tran)
            {
                String message = Float.toString(accdisX) + " " +
                        Float.toString(accdisY) +" "+ Float.toString(accdisZ) + "\r\n";
                publishMessage(pubchannel,message);
                //transition = false;
            }
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.i(TAG, "onResume");
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        Log.i(TAG, "onPause");
    }
}
