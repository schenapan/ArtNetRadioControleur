package fr.legoulet.radio.danse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zsmartsystems.zigbee.dongle.ember.EzspFrameHandler;
import com.zsmartsystems.zigbee.dongle.ember.ZigBeeDongleEzsp;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.EzspFrame;
import com.zsmartsystems.zigbee.dongle.ember.ezsp.command.EzspMfgLibRxHandler;
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
import artnet4j.packets.ArtNetPacket;
import artnet4j.packets.PacketType;

public class MainControleur {
    /**
     * The {@link Logger}.
     */
    private final static Logger logger = LoggerFactory.getLogger(MainControleur.class);   

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
        	captureEnable = false;
        }));


        // TODO le prog principal
        final String serialPortName = "/dev/ttyUSB0";
        final int serialBaud = 57600;
        final FlowControl flowControl = FlowControl.FLOWCONTROL_OUT_XONOFF;

        logger.info("Create Serial Port");
        final ZigBeePort serialPort = new ZigBeeSerialPort(serialPortName, serialBaud, flowControl);

        logger.info("Create ezsp dongle");
        final ZigBeeDongleEzsp dongle = new ZigBeeDongleEzsp(serialPort);

        logger.info("ASH Init");
        if( dongle.initializeEzspProtocol() ) {
        	// add listener for receive alla ezsp response and handler
        	dongle.addListener( ezspListener );
        	
        	// start MfgLib
        	EzspMfgLibStartRequest mfgStartRqst = new EzspMfgLibStartRequest();
        	mfgStartRqst.setRxCallback(true);
        	EzspMfgLibStartResponse mfgStartRsp = (EzspMfgLibStartResponse) dongle.singleCall(mfgStartRqst, EzspMfgLibStartResponse.class);
        	if(EmberStatus.EMBER_SUCCESS == mfgStartRsp.getStatus()) {
            	// set channel
        		EzspMfgLibSetChannelRequest mfgSetChannelRqst = new EzspMfgLibSetChannelRequest();
        		mfgSetChannelRqst.setChannel(11);
        		EzspMfgLibSetChannelResponse mfgSetChannelRsp = (EzspMfgLibSetChannelResponse) dongle.singleCall(mfgSetChannelRqst, EzspMfgLibSetChannelResponse.class);
        		
        		if(EmberStatus.EMBER_SUCCESS == mfgSetChannelRsp.getStatus() ) {
        			/** @todo ??? */
        		}
        	}
        }        
        
        logger.info("starting artnet");
        /*
        ArtNet artnet = new ArtNet();
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
					logger.info("artnet server packet received : {}", packet);
					if( PacketType.ART_POLL == packet.getType() ) {
						artnet4j.packets.ArtPollReplyPacket out = new artnet4j.packets.ArtPollReplyPacket();
						out.setReportCode(NodeReportCode.RcPowerOk);
						out.setData(new byte[] { 0,0,0,0, 0x36,0x19, 0,0, 0,0, (byte)0xFF, (byte)0xFF, 0, 
								(byte)0xff,(byte)0xff, 0, 0, 0, 1,0, (byte)0x80,0,0,0, 0,0,0,0, 0,0,0,0, 
								0,0,0,0, 0,0,0,0, 0, 0});
						
						
						
						artnet.broadcastPacket(out);
					}
				}
				
				@Override
				public void artNetPacketBroadcasted(ArtNetPacket packet) {
					// TODO Auto-generated method stub
					logger.info("artnet server packet broadcast : {}", packet);
				}
			});
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ArtNetException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/

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
	}

}
