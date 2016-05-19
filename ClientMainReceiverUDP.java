import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;

public class ClientMainReceiverUDP implements Runnable {
	
	private int port;
	private DatagramSocket serverSocket;
	
	private HashMap<InetSocketAddress,DatagramSocket> transferencias;
		
	public ClientMainReceiverUDP(int port){
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
				InetSocketAddress clientAddress = new InetSocketAddress(clientIP, clientPort);
				switch(pdu.getType()){
				case PDU.PROBE_REQUEST:
					probeRequest(pdu, clientIP, clientPort);
					break;
				case PDU.REQUEST:
					request(pdu, clientAddress);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}

	private void request(PDU pdu, InetSocketAddress endereco) throws IOException{
		if(this.transferencias.containsKey(endereco)){
			this.transferencias.get(endereco).close();
			this.transferencias.remove(endereco);
		}
		
		InetAddress clientIP = endereco.getAddress();
		int clientPort = endereco.getPort();
		
		try {
			DatagramSocket transferSocket = new DatagramSocket();	
			Transferencia t = new Transferencia(endereco,pdu.getRequestSong());

			byte[] numSegmentos = PDU.toBytes(t.getNumSegmentos());
			byte[] socketPort = PDU.toBytes(transferSocket.getLocalPort());
			byte[] responseData = new byte[numSegmentos.length + socketPort.length];
			for(int i = 0; i<socketPort.length; i++){
				responseData[i] = socketPort[i];
			}
			int j = socketPort.length;
			for(int i=0; i<numSegmentos.length; i++){
				responseData[i+j] = numSegmentos[i];
			}
			PDU responsePDU = new PDU((byte)1,(byte)0,PDU.REQUEST_RESPONSE,null,responseData.length,responseData);
			byte[] responseMessage = responsePDU.writeMessage();
			DatagramPacket responsePacket = new DatagramPacket(responseMessage, responseMessage.length, clientIP, clientPort);
			serverSocket.send(responsePacket);
			
			Thread transferThread = new Thread(new ClientTransferHandlerUDP(endereco,t,transferSocket));
			transferThread.start();
			this.transferencias.put(endereco, transferSocket);
			
			serverSocket.send(responsePacket);			
		} catch (IOException e) {
			e.printStackTrace();
			
			byte[] data = new byte[1];
			data[0] = PDU.KO;
			PDU p = new PDU((byte)1,(byte)0,PDU.FIN,null,data.length,data);
			byte[] response = p.writeMessage();
			DatagramPacket responsePacket = new DatagramPacket(response, response.length, clientIP, clientPort);
			serverSocket.send(responsePacket);		
		}
		
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
