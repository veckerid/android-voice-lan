package com.audiosender;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

import com.audiosender.AudioReceiverService;

import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.*;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
public class AudioSenderActivity extends Activity {
	public static String SERVERIP = "192.168.1.115";
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
AudioTrack speaker;//Audio Configuration. 
	int Ratesample = 8000;      //How much will be ideal?
	int Configchannel = AudioFormat.CHANNEL_IN_MONO;    
	int Formataudio = AudioFormat.ENCODING_PCM_8BIT;       
private boolean status = true;
int BufSizemin	=	600;
DatagramSocket socketR;
AudioRecord recordplayer=null;
byte[] sendData;
private Handler handler = new Handler();

private ServerSocket serverSocket;
	DatagramSocket socketB;
	public static final int SERVERPORT = 8080;
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    SERVERIP = getLocalIpAddress();
    target = (EditText) findViewById (R.id.target_IP);
    streamingLabel = (TextView) findViewById(R.id.streaming_label);
    startButton = (Button) findViewById (R.id.start_button);
    stopButton = (Button) findViewById (R.id.stop_button);

    streamingLabel.setText("Press Start! to begin");

    startButton.setOnClickListener (startListener);
    stopButton.setOnClickListener (stopListener);
    Thread fst = new Thread(new ClientThread());
    fst.start();
}

public class ServerThread implements Runnable {

    public void run() {
        try {
            if (SERVERIP != null) {
                handler.post(new Runnable() {
                   
                    public void run() {
                        streamingLabel.setText("Listening on IP1: " + SERVERIP);
                    }
                });
                serverSocket = new ServerSocket(SERVERPORT);
                while (true) {
                    // listen for incoming clients
                    Socket client = serverSocket.accept();
                    
                    handler.post(new Runnable() {
                        
                        public void run() {
                            streamingLabel.setText("Connected.");
                        }
                    });

                   
                        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        String line = null;
                        while ((line = in.readLine()) != null) {
                        	
                            final String mm=line;
                            Log.d("ServerActivity", line);
                            handler.post(new Runnable() {
                                public void run() {
                                	streamingLabel.setText(mm);
                                }
                            });
                                    // do whatever you want to the front end
                                    // this is where you can be creative
                                	
                              
                        }
                        break;
                     
                }
            } else {
                handler.post(new Runnable() {
                   
                    public void run() {
                    	streamingLabel.setText("Couldn't detect internet connection.");
                    }
                });
            }
        } catch (Exception e) {
            handler.post(new Runnable() {
               
                public void run() {
                	streamingLabel.setText("Error");
                }
            });
            e.printStackTrace();
        }
    }
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
	startReceiving();
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
public void startReceiving() {
   
    Thread receiveThread = new Thread (new Runnable() {

        public void run() {

            try {
                DatagramSocket socketR = new DatagramSocket(50005);
                Log.d("VR", "Socket Created");

                startSpeaker();
                Log.d("Recorder", "Audio recorder initialised at " + speaker.getSampleRate());
				 byte[] buffer = new byte[BufSizemin];

	                   speaker.play();

	                while(status == true) {
	                    try {


	                        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
	                        socketR.receive(packet);
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
public void startSpeaker() {
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
	                    if (BufSizemin != AudioTrack.ERROR_BAD_VALUE) {
	                        // check if we can instantiate and have a success
	                     speaker = new AudioTrack(AudioManager.STREAM_MUSIC,Ratesample,Configchannel,Formataudio,BufSizemin,AudioTrack.MODE_STREAM);
	                     	Log.d("VS", "Recorder initialized");
	                        
	                        if (speaker.getState() == AudioTrack.STATE_INITIALIZED)
	                            break;
	                    }
	                } catch (Exception e) {
	                    Log.e("recorder", rate + "Exception, keep trying.",e);
	                }
	            }
	        }
	    }
  	    
  	}
// gets the ip address of your phone's network
private String getLocalIpAddress() {
    try {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress()) { return inetAddress.getHostAddress().toString(); }
            }
        }
    } catch (SocketException ex) {
        Log.e("ServerActivity", ex.toString());
    }
    return null;
}
public class ClientThread implements Runnable {

	public void run() {
		try {
			
			String msg="10.0.0.14";
			InetAddress address = InetAddress.getByName("224.0.0.1");
			MulticastSocket socketM = new MulticastSocket(51004);
			socketM.joinGroup(address);
			String port = "51004";
			int pnum = Integer.parseInt(port);
			String mess = msg;
			byte message[] = mess.getBytes();
			DatagramPacket packet = new DatagramPacket(message, message.length, address, pnum);
			socketM.send(packet);
			while(true){
			//For Received message
			 byte[] bufin = new byte[256];
			DatagramPacket packet1 = new DatagramPacket(bufin, bufin.length);
			socketM.receive(packet1);
			String recmessage =new String(packet1.getData());
			final String mm=recmessage;
			handler.post(new Runnable() {
                
                public void run() {
                    streamingLabel.setText(mm);
                }
            });
			}
			
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			Log.e("VR", "errrrrrrrroooor broadcaaaaaaaaaaaasssssssst 11111111");
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("VR", "errrrrrrrroooor broadcaaaaaaaaaaaasssssssst");
			e.printStackTrace();
		}
    


}


}
}
