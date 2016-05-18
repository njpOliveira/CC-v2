import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;

public class ClientReceiverUDP implements Runnable {
	
	private int port;
	private DatagramSocket serverSocket;
	
	private HashMap<String,Transferencia> transferencias;
		
	public ClientReceiverUDP(int port){
		this.port = port;
		this.transferencias = new HashMap<>();
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
				InetAddress clientIP = receiveDatagram.getAddress();
				int clientPort = receiveDatagram.getPort();
				switch(pdu.getType()){
				case PDU.PROBE_REQUEST:
					probeRequest(pdu, clientIP, clientPort);
					break;
				case PDU.REQUEST:
					request(pdu, clientIP, clientPort);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}

	private void request(PDU pdu, InetAddress clientIP, int clientPort) throws IOException {
		InetSocketAddress endereco = new InetSocketAddress(clientIP, clientPort);
		int tamanhoJanela = PDU.toInt(pdu.getOpcoes());
		try {
			Transferencia t = new Transferencia(endereco,tamanhoJanela,pdu.getRequestSong());
			transferencia(t);
		} catch (IOException e) {
			byte[] data = new byte[1];
			data[0] = PDU.KO;
			PDU p = new PDU((byte)1,(byte)0,PDU.FIN,null,data.length,data);
			byte[] response = p.writeMessage();
			DatagramPacket responsePacket = new DatagramPacket(response, response.length, clientIP, clientPort);
			serverSocket.send(responsePacket);		
		}
		
	}

	private void transferencia(Transferencia t) {
		// TODO Auto-generated method stub
		
	}

	private void probeRequest(PDU pdu, InetAddress clientIP, int clientPort) throws IOException {
		long timestamp = System.currentTimeMillis();
		byte[] data = PDU.toBytes(timestamp);
		PDU p = new PDU((byte)1,(byte)0,PDU.PROBE_RESPONSE,null,data.length,data);
		byte[] response = p.writeMessage();
		DatagramPacket responsePacket = new DatagramPacket(response, response.length, clientIP, clientPort);
		serverSocket.send(responsePacket);		
	}

}
