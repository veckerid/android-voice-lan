package com.ptt;
import android.app.*;
import android.os.Bundle;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.http.conn.util.InetAddressUtils;
import android.content.*;
import android.media.*;
import android.media.MediaRecorder.AudioSource;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.*;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
public class PTTActivity extends Activity {
public static String SERVERIP = "";
private TextView streamingLabel;
private Button startButton,stopButton;
public byte[] buffer;
private int port=50005;
AudioRecord recorder=null;
private WifiManager mWifi;
int sampleRate = 8000;      //How much will be ideal?
int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;    
int audioFormat = AudioFormat.ENCODING_PCM_16BIT;       
int minBufSize =0;
AudioTrack speaker;//Audio Configuration. 
int Ratesample = 8000;      //How much will be ideal?
int Configchannel = AudioFormat.CHANNEL_IN_MONO;    
int Formataudio = AudioFormat.ENCODING_PCM_8BIT;       
private boolean status = true;
boolean connected=true;
int BufSizemin	=	600;
DatagramSocket socketR;
DatagramSocket sendSocket;
AudioRecord recordplayer=null;
DatagramSocket socketM4;
DatagramSocket socketM3;
Thread fst3 ;
Thread fst2 ;
Socket clientTcpSocket;
byte[] sendData;
private Handler handler = new Handler();
private ServerSocket serverSocket;
Socket initialsocket;
	DatagramSocket socketB;
	public static final int SERVERPORT = 8080;
	private Spinner SpinnerIp;
	private ArrayAdapter<CharSequence> AdapterIp; 
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    SERVERIP = getLocalIpAddress();
    streamingLabel = (TextView) findViewById(R.id.streaming_label);
    startButton = (Button) findViewById (R.id.start_button);
    stopButton = (Button) findViewById (R.id.stop_button);

    streamingLabel.setText("Press Start! to begin");
    SpinnerIp = (Spinner) findViewById(R.id.spinner1);
    AdapterIp = new ArrayAdapter<CharSequence>(this,android.R.layout.simple_spinner_item);
    AdapterIp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    SpinnerIp.setAdapter(AdapterIp); 
    SpinnerIp.setOnItemSelectedListener(new IpOnItemSelectedListener());
    
    startButton.setOnClickListener (startListener);
    stopButton.setOnClickListener (stopListener);
    stopButton.setVisibility(-1);
    WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE); 
    WifiManager.MulticastLock multicastLock = wm.createMulticastLock("mydebuginfo"); 
    multicastLock.acquire();
    Thread fst = new Thread(new ClientThread());
    fst.start();
    fst2 = new Thread(new ServerThread());
    fst2.start();
}
public class openSocket implements Runnable {

    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getByName((String) SpinnerIp.getSelectedItem());
            Log.d("ClientActivity", "C: Connecting...");
            initialsocket = new Socket(serverAddr, 8080);
            connected = true;
            while (connected) {
                try {
                    Log.d("ClientActivity", "C: Sending command.");
                    final PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(initialsocket
                                .getOutputStream())), true);
                        // where you issue the commands
                   String msg=SERVERIP;

                   out.println(msg);
                   BufferedReader inFromClient =
                       new BufferedReader(new InputStreamReader(initialsocket.getInputStream()));
                  String line="";
                   while ((line = inFromClient.readLine()) != null) {
                	   final String msgRec	=	line;
                       
                       handler.post(new Runnable() {
                           public void run() {
                        	   if(msgRec.startsWith("1")){
                            	   streamingLabel.setText(msgRec);
                            	   stopButton.setVisibility(-1);
                              	 startButton.setVisibility(-1);
                            	   try {
                   					startReceiving((String) SpinnerIp.getSelectedItem());
                   				} catch (UnknownHostException e) {
                   					// TODO Auto-generated catch block
                   					e.printStackTrace();
                   				}
                           		sendVoice((String) SpinnerIp.getSelectedItem());
                               }
                               else{
                            	 streamingLabel.setText(msgRec);
                            	 recorder.release();
                                 speaker.release();
                                 sendSocket.close();
                                 socketR.close();
                          		 stopButton.setVisibility(-1);
                              	 startButton.setVisibility(1);	
                              	 connected = false;
                              	 status=false;
                              	try {
									initialsocket.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
                        	}
                           }
                       });
                         
                   }
                  
                        
                        Log.d("ClientActivity", "C: Sent.");
                } catch (Exception e) {
                    Log.e("ClientActivity", "S: Error", e);
                }
            }
            Log.d("ClientActivity", "C: Closed.");
        } catch (Exception e) {
            Log.e("ClientActivity", "C: Error", e);
            connected = false;
        }
    }
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

                    clientTcpSocket = serverSocket.accept();
                   
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientTcpSocket.getInputStream()));
                        String line = in.readLine();
                        final PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientTcpSocket
                                .getOutputStream())), true);
                       final String mm=line;
                            Log.d("ServerActivity", line);
                            handler.post(new Runnable() {
                                public void run() {
                               	 AlertDialog alertDialog = new AlertDialog.Builder(PTTActivity.this).create();
                                 alertDialog.setTitle("Incoming call");
                                 alertDialog.setMessage(mm+" is calling Answer?");
                                 alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                                	 
                                     
                                     public void onClick(DialogInterface dialog, int which) {
                                    	 out.println(1);
                                    	 stopButton.setVisibility(1);
                                    	 startButton.setVisibility(-1);
                                    		try {
												startReceiving(mm);
											} catch (UnknownHostException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
                                    		sendVoice(mm);
                                
                                   } }); 
                                 alertDialog.show();
                                
                                }
                            });
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

        try {       
    	status = false;
    	connected=false;
    	 stopButton.setVisibility(-1);
      	 startButton.setVisibility(1);
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientTcpSocket
                .getOutputStream())), true);
        out.println(2);
		//clientTcpSocket.close();
                recorder.release();
                speaker.release();
                sendSocket.close();
                socketR.close();
                status=false;
                connected=false;
                } catch (IOException e) {
					e.printStackTrace();
					Log.e("VS", "error closing");
				}
                Log.d("VS","Recorder released");
                Log.d("VS","all closed");
    }

};

private final OnClickListener startListener = new OnClickListener() {

    public void onClick(View arg0) {
                status = true;
                startStreaming();    
                
    }

};

public void startStreaming() {
	fst3 = new Thread(new openSocket());
    fst3.start();
    
 }
public void sendVoice(final String dst){
	Thread streamThread = new Thread(new Runnable() {

        public void run() {
        	try {
        	sendSocket = new DatagramSocket();
            Log.d("VS", "Socket Created");
            
           findAudioRecord();
                Log.d("VS","Buffer created of size " + minBufSize);
                DatagramPacket packet;
                final InetAddress destination = InetAddress.getByName(dst);
                Log.d("VS", "Address retrieved");
                recorder.startRecording();
                buffer	=	new byte[512];
                while(status == true) {
                	//reading data from MIC into buffer
                    minBufSize = recorder.read(buffer, 0, buffer.length);
                    //putting buffer in the packet
                    packet = new DatagramPacket (buffer,buffer.length,destination,port);
                    sendSocket.send(packet);
                    }
        	} catch(UnknownHostException e) {
                Log.e("VS", "UnknownHostException");
            } catch (IOException e) {
                Log.e("VS", "IOException1");
                e.printStackTrace();
            } 


        }

    });
    streamThread.start();
}
public void startReceiving(final String dst) throws UnknownHostException {
	final InetAddress destination = InetAddress.getByName(dst);
    Thread receiveThread = new Thread (new Runnable() {

        public void run() {

            try {
                socketR = new DatagramSocket(50005);
                Log.d("VR", "Socket Created");

                startSpeaker();
                Log.d("Recorder", "Audio recorder initialised at " + speaker.getSampleRate());
				 byte[] buff = new byte[512];

	                   speaker.play();

	                while(status == true) {
	                    try {
	                    	DatagramPacket packet = new DatagramPacket(buff,buff.length);
	                        socketR.receive(packet);
	                        Log.d("VR", "Packet Received");

	                        //reading content from packet
	                        buff=packet.getData();
	                        Log.d("VR", "Packet data read into buffer");

	                        //sending data to the Audiotrack obj i.e. speaker
	                        speaker.write(buff, 0, buff.length);
	                        Log.d("VR", "Writing buffer content to speaker");

	                    } catch(IOException e) {
	                        Log.e("VR","IOException2");
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
 int[] mSampleRates = new int[] { 44100, 8000 };
    for (int rate : mSampleRates) {
        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
            for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT}) {
                try {
                    Log.d("recorder", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                            + channelConfig);
                   minBufSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat)*2;
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
	            for (short channelConfig : new short[] { AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT}) {
	                try {
	                    Log.d("recorder", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
	                            + channelConfig);
	                   BufSizemin = AudioTrack.getMinBufferSize(rate, channelConfig, audioFormat)*2;
	                   Ratesample = rate;      //How much will be ideal?
	                   Configchannel = channelConfig;    
	                   Formataudio = audioFormat; 
	                    if (BufSizemin != AudioTrack.ERROR_BAD_VALUE) {
	                        // check if we can instantiate and have a success
	                     speaker = new AudioTrack(AudioManager.STREAM_VOICE_CALL,Ratesample,Configchannel,Formataudio,BufSizemin,AudioTrack.MODE_STREAM);
	                    AudioManager audioManager;  
	                    audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE); 
	                     int currAudioMode = audioManager.getMode(); 
	                     audioManager.setMode(AudioManager.STREAM_VOICE_CALL); 
	                     audioManager.setRouting(AudioManager.MODE_NORMAL,1,
	                             AudioManager.STREAM_VOICE_CALL);

	                     audioManager.setSpeakerphoneOn(false); 	
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
        String ipv4;
        List<NetworkInterface>  nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
        if(nilist.size() > 0){
            for (NetworkInterface ni: nilist){
                List<InetAddress>  ialist = Collections.list(ni.getInetAddresses());
                if(ialist.size()>0){
                    for (InetAddress address: ialist){
                        if (!address.isLoopbackAddress() && InetAddressUtils.isIPv4Address(ipv4=address.getHostAddress())){ 
                            return ipv4;
                        }
                    }
                }

            }
        }

    } catch (SocketException ex) {

    }
    return "";

}
private InetAddress getBroadcastAddress() throws IOException {
	mWifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    DhcpInfo dhcp = mWifi.getDhcpInfo();
    if (dhcp == null) {
      Log.d("TAG", "Could not get dhcp info");
      return null;
    }

    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++)
      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    return InetAddress.getByAddress(quads);
  }



public class ClientThread implements Runnable {

	public void run() {
		try {
			
			String msg=getLocalIpAddress();

			String port = "1025";
			final Vector v = new Vector();
			int pnum = Integer.parseInt(port);
			socketM4=new DatagramSocket(pnum);
			
			while(true){
			//For Received message
				socketM3 = new DatagramSocket();
				socketM3.setBroadcast(true);
				DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),getBroadcastAddress(), pnum);
				socketM3.send(packet);
				byte[] bufin = new byte[msg.getBytes().length];
				DatagramPacket packet3 = new DatagramPacket(bufin, bufin.length);
				socketM4.receive(packet3);
				String recmessage =new String(packet3.getData());
			final String mm=recmessage;
			handler.post(new Runnable() {
                
                public void run() {
                	if(!v.contains(mm)){
                		AdapterIp.add(mm);
                	v.add(mm);
                	}
                	
                }
            });
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			

			
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			Log.e("VR", "errrrrrrrroooor broadcaaaaaaaaaaaasssssssst 11112111");
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("VR", "errrrrrrrroooor broadcaaaaaaaaaaaasssssssst");
			e.printStackTrace();
		}
    


}


}
}
