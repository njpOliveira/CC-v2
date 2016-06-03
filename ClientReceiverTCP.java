import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientReceiverTCP implements Runnable {
	
	private Socket socket;
    private InputStream dIn;
    private OutputStream dOut;

	public ClientReceiverTCP(Socket socket) throws IOException {
		this.socket = socket;
        this.dOut = socket.getOutputStream();
        this.dIn = socket.getInputStream();
	}

	@Override
	public void run() {
		while(!this.socket.isClosed()){
			try{
				PDU p = PDU.readMessage(dIn);
				switch(p.getType()){
				case PDU.CONSULT_REQUEST:
				case PDU.CONSULT_REQUEST_FROM_MASTER:
					consultRequest(p);
					break;
				}
			}
			catch(Exception e){
			}
		}		
	}
	
    private void consultRequest(PDU request) throws IOException {
    	String musica = Client.pathMusicas+request.getRequestSong();
    	
    	byte temMusica = PDU.NOT_FOUND;
    	File f = new File(musica);
    	if(f.isFile()) { 
    	    temMusica = PDU.FOUND;
    	}

		byte[] data = {temMusica};
		PDU response = new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);
		dOut.write(response.writeMessage());
	}
}
