import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Stack;

public class ClientListener implements Runnable {

    private Socket clientSocket;
    private OutputStream dOut;
    private InputStream dIn;
    private PDUBuffer mensagens;


    public ClientListener(Socket clientSocket, PDUBuffer mensagens) throws IOException{
            this.clientSocket = clientSocket;
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

                        int tamanho = ByteBuffer.wrap(bytesSize).getInt();
                        byte[] dados = new byte[tamanho];

                        for(int i = 0;i<tamanho;i++){
                                dados[i]=(byte) dIn.read();
                        }
                        PDU p = new PDU(cabecalho,dados);
                        switch(p.getType()){
                        case PDU.PING:
                                //System.out.println("Recebi um ping");
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

    public void acknowledge() throws IOException{
            PDU ack = new PDU((byte)1,(byte)2,PDU.ACK,null,0,null);
            dOut.write(ack.writeMessage());
    }

}
