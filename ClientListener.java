import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientListener implements Runnable {

	private Socket socket;
    private OutputStream dOut;
    private InputStream dIn;
    private PDUBuffer mensagens;

    public ClientListener(Socket clientSocket, PDUBuffer mensagens) throws IOException{
            this.socket = clientSocket;
            this.mensagens = mensagens;
            dOut = clientSocket.getOutputStream();
            dIn = clientSocket.getInputStream();
    }

    @Override
    public void run(){
            while(!socket.isClosed()){
                    try{
                        PDU p = PDU.readMessage(dIn);
                        switch(p.getType()){
                        case PDU.PING:
                            this.acknowledge();
                            break;
                        default:
                            this.mensagens.push(p);
                        }
                    }
                    catch(Exception e){}
            }
    }

	public void acknowledge() throws IOException{
            PDU ack = new PDU((byte)1,(byte)0,PDU.ACK,null,0,null);
            dOut.write(ack.writeMessage());
    }

}
