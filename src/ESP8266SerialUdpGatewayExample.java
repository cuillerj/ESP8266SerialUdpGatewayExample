import java.io.IOException;
import java.util.Random;



import java.net.*;
public class ESP8266SerialUdpGatewayExample {
/*
 *  this a java code that works with the arduino TestSerialLinkLibrary using a ESP8266 as a serial IP gateway
 *  It listens on a UDP port from the arduino
 *  It prints received data in hexadecimal format
 *  If frame type 1 received it sends a frame that is to set randomly 0 or 1 a GPIO
 *  If frame type 2 received it prints a received count and sends a frame containing this count and a random value
 */
	public static String ipGateway="";  // 
	public static int udpPort1=1901;
	public static int udpPort2=1902;
	public static int arduinoPort=8888;
	static char[] TAB_BYTE_HEX = { '0', '1', '2', '3', '4', '5', '6','7',
        '8', '9', 'A', 'B', 'C', 'D', 'E','F' };
    static InetAddress IPAddress = null;
    static int currentSentFrameNumber=0;
	static int outHeaderLen=8;
	public static byte ackByte=0x3f;
	public static byte padByte=0x3b;
	public static boolean request=true;
	public static boolean response=false;
	public static boolean toAck=true;
	public static boolean noAck=false;

	/*
	  request
	*/
	public static byte statusRequest =0x10;
	public static byte registersRequest =0x16;
	public static  byte indicatorsRequest =0x07;

	public static void main(String args[]) throws Exception 
	{  

		System.out.println("ESP8266SerialUdpGatewayExample");
		System.out.println("listening to Arduino UDP port:"+udpPort1);
		/*
		 * to listen to the Esp8266 UDP port change port to udpport2
		 */
	      DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DatagramSocket serverSocket1 = null;

		try {
			serverSocket1 = new DatagramSocket(udpPort1);
		} catch (SocketException e1) {
		}
		while(true)
		{	
			int InpLen=0; // init input datalength 
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				serverSocket1.receive(receivePacket);
				InetAddress IPReceiveAddress = receivePacket.getAddress();
				IPAddress=IPReceiveAddress;
				System.out.println("Receive data from arduino IP address :"+IPReceiveAddress);
				InpLen=receiveData[4]-5;
			 	for (int i=0;i<InpLen;i++)  
		 		{
		 			hexaPrint(receiveData[i+9]); // print all the bytes of the frame
		 		}
			 	switch(receiveData[9]){
			 	case 0x07:
			 		byte sign=(byte) (receiveData[12] & 0x80);
			 		int value=(receiveData[12])<< 8 & 0x00007f00 | (receiveData[13])& 0x000000ff;
			 		if (sign!=0x00){
			 			value=-value;
			 		}
			 		System.out.println(" > indicators received:"+value);
			 		break;
			 	case 0x10:
			 		System.out.println(" > status received");
			 		break;
			 	case 0x16:
			 		System.out.println(" > registers received");
			 		break;
			 	}
		 	System.out.println();	 
			Random randomGenerator = new Random();
			int randomInt = randomGenerator.nextInt(65536);  // set to a random value 
			String message="send random:"+randomInt;
			System.out.println(message);
		 	byte[] DataToSend = new byte[11];
			DataToSend[0]=(byte)(randomInt/256);
			DataToSend[1]=(byte)(randomInt);
			byte command=0x0e;
			FrameOut newFrame = new FrameOut();
			byte[] dataToSend =  newFrame.BuildFrameOut(response,noAck, command,DataToSend, 2) ;
			DatagramPacket sendPacket = new DatagramPacket(dataToSend, 2+10, IPAddress, arduinoPort);
			clientSocket.send(sendPacket);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

}
	public static void hexaPrint(byte y)
	{
			System.out.print("-" + String.format("%x", y));
		}
	protected static byte Crc8(byte[] stringData,int len) {
	    int i = 0;
	    byte crc = 0x00;
	    while (len-- > 0) {
	        byte extract = (byte) stringData[i++];	        
	        for (byte tempI = 8; tempI != 0; tempI--) {
	            byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
	            sum = (byte) ((sum & 0xFF) & 0x01); // 
	            crc = (byte) ((crc & 0xFF) >>> 1);
	            if (sum != 0) {
	                crc = (byte)((crc & 0xFF) ^ 0x8C);
	            }
	            extract = (byte) ((extract & 0xFF) >>> 1);
	        }
	    }
	    return (byte) (crc & 0xFF);
	}
	public byte[] BuildFrameOut(boolean requestResponse,boolean toBeAck, byte command, byte inputdata[], int dataLen){
		
		byte[] outFrame =new byte[2048];
		byte[] outData =new byte[2048];
		int frameLen;
		currentSentFrameNumber++;
		System.arraycopy(inputdata, 0, outFrame, outHeaderLen, dataLen);
		outFrame[0]=(byte) ((currentSentFrameNumber)%256); // (currentSentFrameNumber,frameSourceId) are used to check the frame sequence by the station
		outFrame[1]=0x01;    // frame coming from dispatcher
		if (requestResponse && toBeAck)
		{
			outFrame[2]=(byte) 0xc0;
		}
		else if (requestResponse)
		{
			outFrame[2]=(byte) 0x80;				
		}
		else if (toBeAck)
		{
			outFrame[2]=(byte) 0x40;				
		}
		else
		{
			outFrame[2]=(byte) 0x00;				
		}
		outFrame[3]=padByte;
		outFrame[4]=(byte)(dataLen+10); // frame len
		outFrame[5]=padByte;
		outFrame[7]=(byte) (command);
		frameLen=dataLen+10;  // reserved for 0x00 +crc
		System.arraycopy(outFrame, outHeaderLen-1, outData, 0, dataLen+1);
		outFrame[frameLen-1]=Crc8(outData,dataLen+1);
		return outFrame;
}
	static class FrameOut{

		int frameLen;
		byte[] outFrame =new byte[2048];
		byte[] outData =new byte[2048];
		public FrameOut(){
		}
	
		public byte[] BuildFrameOut(boolean requestResponse,boolean toBeAck, byte command, byte inputdata[], int dataLen){
				currentSentFrameNumber++;
				System.arraycopy(inputdata, 0, outFrame, outHeaderLen, dataLen);
				outFrame[0]=(byte) ((currentSentFrameNumber)%256); // (currentSentFrameNumber,frameSourceId) are used to check the frame sequence by the station
				outFrame[1]=0x01;    // frame coming from dispatcher
				if (requestResponse && toBeAck)
				{
					outFrame[2]=(byte) 0xc0;
				}
				else if (requestResponse)
				{
					outFrame[2]=(byte) 0x80;				
				}
				else if (toBeAck)
				{
					outFrame[2]=(byte) 0x40;				
				}
				else
				{
					outFrame[2]=(byte) 0x00;				
				}
				outFrame[3]=padByte;
				outFrame[4]=(byte)(dataLen+10); // frame len
				outFrame[5]=padByte;
				outFrame[7]=(byte) (command);
				this.frameLen=dataLen+10;  // reserved for 0x00 +crc
				System.arraycopy(outFrame, outHeaderLen-1, this.outData, 0, dataLen+1);
				outFrame[frameLen-1]=Crc8(outData,dataLen+1);
				return this.outFrame;
		}
	}
	public  void ForwardToStation (InetAddress IPAddress, int IPport,byte command,byte data[],int dLen) 
	{
		String pgm="ForwardToStation";
		String message=" forward command to " + IPAddress+ " len:"+data.length;
		System.out.println(message);
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
		// TODO Auto-generated catch block

			message=" socket exception";
			System.out.println(message);
			e1.printStackTrace();
		}

		FrameOut newFrame = new FrameOut();
		byte[] dataToSend =  newFrame.BuildFrameOut(request,noAck, command,data, dLen) ;
		DatagramPacket sendPacket2 = new DatagramPacket(dataToSend, dLen+2, IPAddress, IPport);
		try {
			clientSocket.send(sendPacket2);
		} catch (IOException e) {
			e.printStackTrace();
			message=" ForwardToStation packet exception";
			System.out.println(message);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clientSocket.close();
	}
}