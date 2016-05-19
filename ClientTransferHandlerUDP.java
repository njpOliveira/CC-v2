import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Map;

public class ClientTransferHandlerUDP implements Runnable{
	
	private InetAddress clientIP;
	private int clientPort;
	private Transferencia transf;
	private DatagramSocket transferSocket;

	public ClientTransferHandlerUDP(InetSocketAddress endereco, Transferencia transf, DatagramSocket transferSocket) {
		this.clientIP = endereco.getAddress();
		this.clientPort = endereco.getPort();
		this.transf = transf;
		this.transferSocket = transferSocket;
	}

	@Override
	public void run() {
        byte[] buffer = new byte[PDU.MAX_SIZE];
    	DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
    	boolean terminado = false;
        while(!transferSocket.isClosed() && !terminado){
        	try {
				transferSocket.receive(receiveDatagram);
				PDU pdu = new PDU(receiveDatagram.getData());
				InetAddress clientIP = receiveDatagram.getAddress();
				int clientPort = receiveDatagram.getPort();
				if(clientIP.equals(this.clientIP) && clientPort == this.clientPort){
					switch(pdu.getType()){
					case PDU.SYN:
						enviarDados();
						this.transferSocket.setSoTimeout(30000);
						break;
					case PDU.SREJ:
						int segmento = pdu.getRejectSegment();
						reject(segmento);
						break;
					case PDU.FIN:
						terminado = true;
						break;
					}
				}
			} 
        	catch(SocketTimeoutException to){
        		this.transferSocket.close();
        	}
        	catch (IOException e) {	}
        }
		
	}

	private void reject(int segmento) {
		byte[] data = this.transf.getSegmentos().get(segmento);
		PDU p = new PDU((byte)1,(byte)0,PDU.DATA,PDU.toBytes(segmento),data.length,data);
		byte[] dataMessage = p.writeMessage();
		DatagramPacket dataPacket = new DatagramPacket(dataMessage, dataMessage.length, this.clientIP, this.clientPort);
		try {
			transferSocket.send(dataPacket);
		} catch (IOException e) {}		
	}

	private void enviarDados() {
		Map<Integer,byte[]> segmentos = transf.getSegmentos();
		for(int segIndex: segmentos.keySet()){
			byte[] data = segmentos.get(segIndex);
			PDU p = new PDU((byte)1,(byte)0,PDU.DATA,PDU.toBytes(segIndex),data.length,data);
			byte[] dataMessage = p.writeMessage();
			DatagramPacket dataPacket = new DatagramPacket(dataMessage, dataMessage.length, this.clientIP, this.clientPort);
			try {
				transferSocket.send(dataPacket);
			} catch (IOException e) {}		
		}		
	}

}
