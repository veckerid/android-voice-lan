package com.audiosender;

import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder.AudioSource;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

public class AudioReceiverService  extends Service{
			Socket s;
		 	PrintStream os;
		 	static DatagramSocket socket;
		 	AudioTrack speaker;//Audio Configuration. 
		 	int Ratesample = 0;      //How much will be ideal?
		 	int Configchannel = 0;    
		 	int Formataudio = 0;       

		    int BufSizemin	=	0;

		    AudioRecord recordplayer=null;
		    private boolean status = true;
	    @Override
	    public IBinder onBind(Intent arg0) {
	        // TODO Auto-generated method stub
	        return myBinder;
	    }

	    private final IBinder myBinder =  new LocalBinder();

	    public class LocalBinder extends Binder {
	        public AudioReceiverService getService() {
	            return AudioReceiverService.this;
	        }
	    }


	    @Override
	    public void onCreate() {
	        super.onCreate();
	    }

	    public void IsBoundable(){
	        Toast.makeText(this,"I bind like butter", Toast.LENGTH_LONG).show();
	    }

	    public void onStart(Intent intent, int startId){
	        super.onStart(intent, startId);
	        Toast.makeText(this,"Service created ...", Toast.LENGTH_LONG).show();
	        startReceiving();
	    }

	   

	    @Override
	    public void onDestroy() {
	        super.onDestroy();
	        try {
	            s.close();
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        s = null;
	    }
	    public void startReceiving() {

	        Thread receiveThread = new Thread (new Runnable() {

	            public void run() {

	                try {

	                    DatagramSocket socket = new DatagramSocket(50005);
	                    Log.d("VR", "Socket Created");

	                    findAudioRecord();
	                    Log.d("Recorder", "Audio recorder initialised at " + recordplayer.getSampleRate());
	    				 byte[] buffer = new byte[BufSizemin];

	    	                speaker = new AudioTrack(AudioManager.STREAM_MUSIC,Ratesample,Configchannel,Formataudio,BufSizemin,AudioTrack.MODE_STREAM);

	    	                speaker.play();

	    	                while(status == true) {
	    	                    try {


	    	                        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
	    	                        socket.receive(packet);
	    	                        Log.d("VR", "Packet Received");

	    	                        //reading content from packet
	    	                        buffer=packet.getData();
	    	                        Log.d("VR", "Packet data read into buffer");

	    	                        //sending data to the Audiotrack obj i.e. speaker
	    	                        speaker.write(buffer, 0, BufSizemin);
	    	                        Log.d("VR", "Writing buffer content to speaker");

	    	                    } catch(IOException e) {
	    	                        Log.e("VR","IOException");
	    	                    }
	                    //minimum buffer size. need to be careful. might cause problems. try setting manually if any problems faced
	                    //int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
	    	                }



	                } catch (SocketException e) {
	                    Log.e("VR", "SocketException");
	                    e.printStackTrace();
	                }


	            }

	        });
	        receiveThread.start();
	    }
	    public void findAudioRecord() {
	   	 int[] mSampleRates = new int[] { 44100, 11025, 22050, 8000 };
	   	    for (int rate : mSampleRates) {
	   	        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
	   	            for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT}) {
	   	                try {
	   	                    Log.d("recorder", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
	   	                            + channelConfig);
	   	                   BufSizemin = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
	   	                   Ratesample = rate;      //How much will be ideal?
	   	                   Configchannel = channelConfig;    
	   	                   Formataudio = audioFormat; 
	   	                    if (BufSizemin != AudioRecord.ERROR_BAD_VALUE) {
	   	                        // check if we can instantiate and have a success
	   	                        recordplayer = new AudioRecord(AudioSource.DEFAULT, rate, channelConfig, audioFormat, BufSizemin);
	   	                        Log.d("VS", "Recorder initialized");
	   	                        
	   	                        if (recordplayer.getState() == AudioRecord.STATE_INITIALIZED)
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
