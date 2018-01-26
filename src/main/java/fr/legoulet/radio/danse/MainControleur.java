package fr.legoulet.radio.danse;

import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zsmartsystems.zigbee.dongle.ember.EzspFrameHandler;
import com.zsmartsystems.zigbee.dongle.ember.ZigBeeDongleEzsp;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.EzspFrame;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibRxHandler;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibSendPacketRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibSendPacketResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibSetChannelRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibSetChannelResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibStartRequest;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibStartResponse;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.structure.EmberStatus;
import com.zsmartsystems.zigbee.serial.ZigBeeSerialPort;
import com.zsmartsystems.zigbee.transport.ZigBeePort;
import com.zsmartsystems.zigbee.transport.ZigBeePort.FlowControl;

import artnet4j.ArtNet;
import artnet4j.ArtNetException;
import artnet4j.ArtNetServer;
import artnet4j.NodeReportCode;
import artnet4j.events.ArtNetServerListener;
import artnet4j.packets.ArtDmxPacket;
import artnet4j.packets.ArtNetPacket;
import artnet4j.packets.PacketType;

public class MainControleur {
    /**
     * The {@link Logger}.
     */
    private final static Logger logger = LoggerFactory.getLogger(MainControleur.class);   

    private static ZigBeeDongleEzsp dongle;
    private static ArtNet artnet;
    
    private static boolean captureEnable = false;
    private static EzspFrameHandler ezspListener = new EzspFrameHandler() {

		@Override
		public void handlePacket(EzspFrame response) {
			if( captureEnable ) {
				if(response instanceof EzspMfgLibRxHandler) {
					int[] msg = ((EzspMfgLibRxHandler)response).getMessageContents();
				}
			}
		}

		@Override
		public void handleLinkStateChange(boolean state) {
			// TODO Auto-generated method stub
			
		}
    	
    };

	public static void main(String[] args) {
		
        logger.info("Controleur Start !");
    	
    	/* Run cleanUp on all interaces when exiting */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        	// TODO effectuer le nettoyage avant de quitter
        	logger.info("Close dongle");
        	dongle.shutdown();
        	
        	logger.info("stopping artnet");
        	artnet.stop();
        }));


        // TODO le prog principal
        final String serialPortName = "/dev/ttyUSB0";
        final int serialBaud = 57600;
        final FlowControl flowControl = FlowControl.FLOWCONTROL_OUT_XONOFF;

        logger.info("Create Serial Port");
        final ZigBeePort serialPort = new ZigBeeSerialPort(serialPortName, serialBaud, flowControl);

        logger.info("Create ezsp dongle");
        dongle = new ZigBeeDongleEzsp(serialPort);

        logger.info("ASH Init");
        if( dongle.initializeEzspProtocol() ) {
        	// add listener for receive all ezsp response and handler
        	dongle.addListener( ezspListener );
        	
        	// start MfgLib
        	EzspMfgLibStartRequest mfgStartRqst = new EzspMfgLibStartRequest();
        	mfgStartRqst.setRxCallback(true);
        	EzspMfgLibStartResponse mfgStartRsp = (EzspMfgLibStartResponse) dongle.singleCall(mfgStartRqst, EzspMfgLibStartResponse.class);
        	if(EmberStatus.EMBER_SUCCESS == mfgStartRsp.getStatus()) {
            	// set channel
        		EzspMfgLibSetChannelRequest mfgSetChannelRqst = new EzspMfgLibSetChannelRequest();
        		mfgSetChannelRqst.setChannel(15);
        		EzspMfgLibSetChannelResponse mfgSetChannelRsp = (EzspMfgLibSetChannelResponse) dongle.singleCall(mfgSetChannelRqst, EzspMfgLibSetChannelResponse.class);
        		
        		if(EmberStatus.EMBER_SUCCESS == mfgSetChannelRsp.getStatus() ) {
        			/** @todo ??? */
        			startArtNetDevice();
        			
        			while(true)
        				;
        			
        		}
        	}
        }        
        
        /*
        try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
    	logger.info("Close dongle");
    	dongle.shutdown();
    	
    	logger.info("stopping artnet");
    	//artnet.stop();
        
        
        logger.error("Controleur STOP !");
        */
	}
	
	private static void startArtNetDevice() {
        logger.info("starting artnet");
        artnet = new ArtNet();
        try {
			artnet.start();
			artnet.addServerListener(new ArtNetServerListener() {
				
				@Override
				public void artNetServerStopped(ArtNetServer server) {
					// TODO Auto-generated method stub
					logger.info("artnet server stopped");
				}
				
				@Override
				public void artNetServerStarted(ArtNetServer server) {
					// TODO Auto-generated method stub
					logger.info("artnet server started");
				}
				
				@Override
				public void artNetPacketUnicasted(ArtNetPacket packet) {
					// TODO Auto-generated method stub
					logger.info("artnet server packet unicast : {}", packet);
				}
				
				@Override
				public void artNetPacketReceived(ArtNetPacket packet) {
					// TODO Auto-generated method stub
					// logger.info("artnet server packet received : {}", packet);
					if( PacketType.ART_POLL == packet.getType() ) {
						artnet4j.packets.ArtPollReplyPacket out = new artnet4j.packets.ArtPollReplyPacket();
						out.setReportCode(NodeReportCode.RcPowerOk);
						
						byte[] pollReply = {0x41, 0x72, 0x74, 0x2d, 0x4e, 0x65, 0x74, 0x00, // ID: Art-Net
									0x00, 0x21, // OpCode : ART_POLL_REPLY
									(byte) 192, (byte) 168, 1, 84, // IpAddress
									0x36, 0x19, // PortNumber  
									0, 1, // VersInfo
									0, // NetSwitch 
									1, // SubSwitch  
									(byte) 0xff, (byte) 0xff, // Oem
									0, // UbeaVersion
									0, // Status1
									0, 0, // EstaMan
									'a', 'r', 't', 'N', 'e', 't', 'R', 'a', 'd', 'i', 'o', 0, 0, 0, 0, 0, 0, 0, // ShortName
									'l', 'e', ' ', 'G', 'o', 'u', 'l', 'e', 't', 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // LongName
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // NodeReport
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
									0x00, 0x01, // NumPorts
									(byte) 0x80, 0x00, 0x00, 0x00, // PortTypes [4]
									(byte) 0x80, 0x00, 0x00, 0x00, // GoodInput [4]
									(byte) 0x00, 0x00, 0x00, 0x00, // GoodOutput [4]
									0x00, 0x00, 0x00, 0x00, // SwIn[4] 
									0x00, 0x00, 0x00, 0x00, // SwOut[4]
									0x00, // SwVideo
									0x00, // SwMacro 
									0x00, // SwRemote
									0x00, // Spare1
									0x00, // Spare2
									0x00, // Spare3
									0x00, // Style
									0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Mac
									(byte) 192, (byte) 168, 1, 84, // BindIp
									1, // BindIndex
									0, // Status2
									0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // Filler
									0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
									0, 0, 0, 0, 0, 0
						};
						
						out.setData(pollReply);

						artnet.setBroadCastAddress("192.168.1.255");
						artnet.broadcastPacket(out);
					}
					else if( PacketType.ART_OUTPUT == packet.getType() ) {
						ArtDmxPacket data = (ArtDmxPacket) packet;
						
						if( (0 == data.getUniverseID()) && (1 == data.getSubnetID()) ) {
							String l_str = new String();
							for( int loop=0; loop<20; loop++ )
							{
								l_str = l_str + String.format("%02X ", data.getDmxData()[loop]);
							}
							logger.info("ArtDmxPacket : {}", l_str );
							
							
							// send radio packet
			        		EzspMfgLibSendPacketRequest mfgSendPacketlRqst = new EzspMfgLibSendPacketRequest();
			        		
							int[] messageContents = new int[7+4+64];
							// MAC Header
							// Frame control
							messageContents[0] = 0x01;
							messageContents[1] = 0x08;
							// seq number
							messageContents[2] = data.getSequenceID();
							// dest panId
							messageContents[3] = 0xCD;
							messageContents[4] = 0xAB;
							// dest addr
							messageContents[5] = 0xFF;
							messageContents[6] = 0xFF;
							
							// MAC Payload
							// Custom Header
							// TAG
							messageContents[7] = 'D';
							messageContents[8] = 'M';
							messageContents[9] = 'X';
							// Option|Offset : 
							// - Option :
							// -- 0 : no compression
							// - Offset : 4 lower bits, in raw of 64. ie :
							// -- offset 0 means data 0->63
							// -- offset 1 means data 64->127
							// ...
							messageContents[10] = 0;
							
							// payload
							for( int loop=0; loop<64; loop++ )
							{
								messageContents[11+loop] = data.getDmxData()[loop];
							}
							
			        		mfgSendPacketlRqst.setMessageContents(messageContents);
			        		EzspMfgLibSendPacketResponse mfgSendPacketRsp = (EzspMfgLibSendPacketResponse) dongle.singleCall(mfgSendPacketlRqst, EzspMfgLibSendPacketResponse.class);
						}
					}
				}
				
				@Override
				public void artNetPacketBroadcasted(ArtNetPacket packet) {
					// TODO Auto-generated method stub
					// logger.info("artnet server packet broadcast : {}", packet);
				}
			});
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ArtNetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}
	

}
