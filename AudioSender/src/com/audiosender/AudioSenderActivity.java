package com.audiosender;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import com.audiosender.AudioReceiverService;
import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
public class AudioSenderActivity extends Activity {

private EditText target;
private TextView streamingLabel;
private Button startButton,stopButton;

public byte[] buffer;
public static DatagramSocket socket;
private int port=50005;         //which port??
AudioRecord recorder=null;

//Audio Configuration. 
int sampleRate = 8000;      //How much will be ideal?
int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;    
int audioFormat = AudioFormat.ENCODING_PCM_16BIT;       
int minBufSize =0;

private boolean status = true;




@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    target = (EditText) findViewById (R.id.target_IP);
    streamingLabel = (TextView) findViewById(R.id.streaming_label);
    startButton = (Button) findViewById (R.id.start_button);
    stopButton = (Button) findViewById (R.id.stop_button);

    streamingLabel.setText("Press Start! to begin");

    startButton.setOnClickListener (startListener);
    stopButton.setOnClickListener (stopListener);
}

private final OnClickListener stopListener = new OnClickListener() {

    public void onClick(View arg0) {
                status = false;
                recorder.release();
                Log.d("VS","Recorder released");
    }

};

private final OnClickListener startListener = new OnClickListener() {

    public void onClick(View arg0) {
                status = true;
                startStreaming();           
    }

};

public void startStreaming() {

	Log.d("service", "onClick: starting srvice");
    startService(new Intent(AudioSenderActivity.this, AudioReceiverService.class));
    Thread streamThread = new Thread(new Runnable() {

        public void run() {
        	try {
        	DatagramSocket socket = new DatagramSocket();
            Log.d("VS", "Socket Created");
            
           findAudioRecord();
                Log.d("VS","Buffer created of size " + minBufSize);
                DatagramPacket packet;
                final InetAddress destination = InetAddress.getByName(target.getText().toString());
                Log.d("VS", "Address retrieved");
                recorder.startRecording();
                buffer	=	new byte[minBufSize];
                while(status == true) {
                	//reading data from MIC into buffer
                    minBufSize = recorder.read(buffer, 0, buffer.length);
                    //putting buffer in the packet
                    packet = new DatagramPacket (buffer,buffer.length,destination,port);
                    socket.send(packet);
                	}
        	} catch(UnknownHostException e) {
                Log.e("VS", "UnknownHostException");
            } catch (IOException e) {
                Log.e("VS", "IOException");
                e.printStackTrace();
            } 


        }

    });
    streamThread.start();
 }
public void findAudioRecord() {
 int[] mSampleRates = new int[] { 44100, 11025, 22050, 8000 };
    for (int rate : mSampleRates) {
        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
            for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT}) {
                try {
                    Log.d("recorder", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                            + channelConfig);
                   minBufSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                   sampleRate = rate;      //How much will be ideal?
                   channelConfig = channelConfig;    
                   audioFormat = audioFormat; 
                    if (minBufSize != AudioRecord.ERROR_BAD_VALUE) {
                        // check if we can instantiate and have a success
                        recorder = new AudioRecord(AudioSource.DEFAULT, rate, channelConfig, audioFormat, minBufSize);
                        Log.d("VS", "Recorder initialized");
                        
                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                            break;
                    }
                } catch (Exception e) {
                    Log.e("recorder", rate + "Exception, keep trying.",e);
                }
            }
        }
    }
}

}
