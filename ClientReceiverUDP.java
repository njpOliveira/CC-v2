import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class ClientReceiverUDP implements Runnable {
	
	private int port;
	private DatagramSocket serverSocket;
	
	private HashMap<InetSocketAddress,Transferencia> transferencias;
		
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
				InetSocketAddress clientAddress = new InetSocketAddress(clientIP, clientPort);
				switch(pdu.getType()){
				case PDU.PROBE_REQUEST:
					probeRequest(pdu, clientIP, clientPort);
					break;
				case PDU.REQUEST:
					request(pdu, clientAddress);
					break;
				case PDU.RR:
					int segmento = pdu.getRRsegment();
					receiverReady(clientAddress, segmento);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}

	private void request(PDU pdu, InetSocketAddress endereco) throws IOException {
		InetAddress clientIP = endereco.getAddress();
		int clientPort = endereco.getPort();
		int tamanhoJanela = PDU.toInt(pdu.getOpcoes());
		try {
			Transferencia t = new Transferencia(endereco,tamanhoJanela,pdu.getRequestSong());
			this.transferencias.put(endereco,t);
			this.receiverReady(endereco,0);
		} catch (IOException e) {
			byte[] data = new byte[1];
			data[0] = PDU.KO;
			PDU p = new PDU((byte)1,(byte)0,PDU.FIN,null,data.length,data);
			byte[] response = p.writeMessage();
			DatagramPacket responsePacket = new DatagramPacket(response, response.length, clientIP, clientPort);
			serverSocket.send(responsePacket);		
		}
		
	}

	private void receiverReady(InetSocketAddress endereco, int segmento) throws IOException {
		InetAddress clientIP = endereco.getAddress();
		int clientPort = endereco.getPort();
		
		Transferencia t = this.transferencias.get(endereco);
		t.receiverReady(segmento);
		Map<Integer,byte[]> mensagens = t.sendMax();
		
		for(int segIndex: mensagens.keySet()){
			byte[] data = mensagens.get(segIndex);
			PDU p = new PDU((byte)1,(byte)0,PDU.DATA,PDU.toBytes(segIndex),data.length,data);
			byte[] dadosEnvio = p.writeMessage();
			DatagramPacket responsePacket = new DatagramPacket(dadosEnvio, dadosEnvio.length, clientIP, clientPort);
			serverSocket.send(responsePacket);		
		}
		
		if(t.isFinished()){
			byte[] data = new byte[1];
			data[0] = PDU.OK;
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
