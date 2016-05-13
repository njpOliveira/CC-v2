import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientListener implements Runnable {

	@SuppressWarnings("unused")
	private Socket socket;
    private OutputStream dOut;
    private InputStream dIn;
    private PDUBuffer mensagens;

    public static final String pathMusicas = System.getProperty("user.dir")+"\\kit_TP2\\";

    public ClientListener(Socket clientSocket, PDUBuffer mensagens) throws IOException{
            this.socket = clientSocket;
            this.mensagens = mensagens;
            dOut = clientSocket.getOutputStream();
            dIn = clientSocket.getInputStream();
    }

    @Override
    public void run(){
            while(true){
                    try{
                        byte[] cabecalho = new byte[11];
                        for (int i = 0;i<11;i++){
                                cabecalho[i] = (byte) dIn.read();
                        }

                        byte[] bytesSize = new byte[4];
                        for(int i = 7;i<11;i++){
                                bytesSize[i-7]=cabecalho[i];
                        }

                        int tamanho = PDU.toInt(bytesSize);
                        byte[] dados = new byte[tamanho];

                        for(int i = 0;i<tamanho;i++){
                                dados[i]=(byte) dIn.read();
                        }
                        PDU p = new PDU(cabecalho,dados);
                        switch(p.getType()){
                        case PDU.CONSULT_REQUEST:
                        	this.consultRequest(p);
                        	break;
                        case PDU.PING:
                            this.acknowledge();
                            break;
                        default:
                            this.mensagens.push(p);
                        }
                    }
                    catch(Exception e){
                            //e.printStackTrace();
                            return;
                    }
            }
    }

    private void consultRequest(PDU request) throws IOException {
    	String musica = pathMusicas+request.getRequestSong();
    	
    	byte temMusica = PDU.NOT_FOUND;
    	File f = new File(musica);
    	if(f.isFile()) { 
    	    temMusica = PDU.FOUND;
    	}

		byte[] data = {temMusica};
		PDU response = new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);
		dOut.write(response.writeMessage());
	}

	public void acknowledge() throws IOException{
            PDU ack = new PDU((byte)1,(byte)0,PDU.ACK,null,0,null);
            dOut.write(ack.writeMessage());
    }

}
