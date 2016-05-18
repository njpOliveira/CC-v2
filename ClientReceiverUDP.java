import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ClientReceiverUDP implements Runnable {
	
	private int port;
	private DatagramSocket serverSocket;
	
	private InetAddress clientIP;
	private int clientPort;
		
	public ClientReceiverUDP(int port){
		this.port = port;
	}

	@Override
	public void run() {
        try {
			this.serverSocket = new DatagramSocket(this.port);
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}
        
        byte[] buffer = new byte[PDU.MAX_SIZE];
        while(true){
        	DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
        	try {
				serverSocket.receive(receiveDatagram);
				PDU pdu = new PDU(receiveDatagram.getData());
				clientIP = receiveDatagram.getAddress();
				clientPort = receiveDatagram.getPort();
				switch(pdu.getType()){
				case PDU.PROBE_REQUEST:
					probeRequest(pdu);
					break;
				case PDU.REQUEST:
					request(pdu);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}

	private void request(PDU pdu) {
		// TODO
		
	}

	private void probeRequest(PDU pdu) throws IOException {
		long timestamp = System.currentTimeMillis();
		byte[] data = PDU.toBytes(timestamp);
		PDU p = new PDU((byte)1,(byte)0,PDU.PROBE_RESPONSE,null,data.length,data);
		byte[] response = p.writeMessage();
		DatagramPacket responsePacket = new DatagramPacket(response, response.length, clientIP, clientPort);
		serverSocket.send(responsePacket);		
	}

}
