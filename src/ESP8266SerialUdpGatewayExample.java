import java.io.IOException;
import java.util.Random;
import java.net.*;
public class ESP8266SerialUdpGatewayExample {
/*
 *  this a java code that works with the arduino TestSerialLinkLibrary using a ESP8266 as a serial IP gateway
 *  IP address must be provide as argument
 *  It listens on a UDP port from the arduino
 *  It prints received data in hexadecimal format
 *  If frame type 1 received it sends a frame that is to set randomly 0 or 1 a GPIO
 *  If frame type 2 received it prints a received count and sends a frame containing this count and a random value
 */
	public static String ipGateway="";  // 
	public static int udpPort=1830;
	static char[] TAB_BYTE_HEX = { '0', '1', '2', '3', '4', '5', '6','7',
        '8', '9', 'A', 'B', 'C', 'D', 'E','F' };
    static InetAddress IPAddress = null;
	public static void main(String args[]) throws Exception
	{  
		ipGateway=args[0];
		System.out.println("SP8266SerialUdpGatewayExample");
		try {

		      IPAddress = InetAddress.getByName(ipGateway);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	      DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(udpPort);
		} catch (SocketException e1) {
		}
		while(true)
		{	
			int InpLen=0; // init input datalength 
			int FrameType=0; // init input datalength 
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			 InpLen=(byte)receiveData[4];       // get the data length inside the frame
			 FrameType=(byte)receiveData[6];  // get the data frame type inside the frame
			 	for (int i=0;i<InpLen+6;i++)  
			 		{
			 			hexaPrint(receiveData[i]); // print all the bytes of the frame
			 		}
			 	System.out.println();
			 
			 	byte[] sendData = new byte[25];
				byte[] DataToSend = new byte[11];
				
			 switch(FrameType)
			 {
			 		case 1:
			 		    Random randomGenerator = new Random();
			 		    int randomInt = randomGenerator.nextInt(2);
			 			DataToSend[0]=0x01; // flag for the arduino treatment
			 			DataToSend[1]=0x0d; // gpio number 13 (or 12) (gpio must be defined output in arduino) 
			 			DataToSend[2]=(byte)(randomInt); // set randomly to 0 or 1
			 			sendData = DataToSend;
			 			break;
			 		case 2:
			 		    randomGenerator = new Random();
			 		    randomInt = randomGenerator.nextInt(65536);  // set to a random value 
			 			int oct0=(byte)(receiveData[14]&0x7F)-(byte)(receiveData[14]&0x80); // to unsigned byte
				    	int oct1=(byte)(receiveData[15]&0x7F)-(byte)(receiveData[15]&0x80); // to unsigned byte
				    	int count=256*oct0+oct1;
						System.out.println("receive count:"+count);
			 			sendData = new byte[25];
			 			DataToSend = new byte[11];
			 			DataToSend[0]=0x02; // flag for the arduino treatment
			 			DataToSend[1]=(byte)(count/256); //
						DataToSend[2]=(byte)(count);
						DataToSend[3]=0x00;  // 0x00 or ASCII byte needed to avoid forbidden sequence
						DataToSend[4]=(byte)(randomInt/256);
						DataToSend[5]=(byte)(randomInt);
						DataToSend[7]=0x00;
						DataToSend[8]=0x00;
						DataToSend[9]=0x00;
						DataToSend[10]=0x00;
						sendData = DataToSend;
					break;
			 }

			      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8888);
		      clientSocket.send(sendPacket);
		}
}
	public static void hexaPrint(byte y)
	{
			System.out.print("-" + String.format("%x", y));
		}
}