import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;


public class Server {

    private Map<String,Registo> registos;
    private static ServerSocket servidor;
    public static final int port = 6789;
    
    public static final String masterIP = "localhost";
    public static final int masterPort = 5846;
	
    public void start() throws IOException{
        this.registos = new HashMap<>();
        servidor = new ServerSocket(port);
        
        try{
        	registarMaster();
        }
        catch(IOException e){
        	System.out.println("Impossível estabelecer ligação com o master");
        	return;
        }
        
        Thread pinger = new Thread(new Pinger(registos));
        pinger.start();

        while(true){
            Socket cliente = null;
            try {
                cliente = servidor.accept();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Thread t = new Thread(new ServerThread(registos,cliente));
            t.start();
        }
    }
    


	private void registarMaster() throws IOException{
		
        InetAddress ip = InetAddress.getLocalHost();
        byte[] bytesIP = ip.getAddress();

        byte[] bytesID = new byte[0];

        byte[] bytesPORT = PDU.toBytes(port);

        int sizeOfdados = 1 + bytesIP.length + bytesPORT.length + bytesID.length ;
        byte[] dados = new byte[sizeOfdados];

        dados[0] = PDU.IN;

        int j;

        for(int i = 1;i<(bytesIP.length)+1;i++){
                j=i-1;
                dados[i] = bytesIP[j];
        }

        for(int i = 1+bytesIP.length;i<1+bytesIP.length+bytesPORT.length;i++){
                j=i-1-bytesIP.length;
                dados[i] = bytesPORT[j];
        }

        for(int i = 1+bytesIP.length+bytesPORT.length;i<1+bytesIP.length+bytesPORT.length+bytesID.length;i++){
                j=i-1-bytesIP.length-bytesPORT.length;
                dados[i] = bytesID[j];
        }

        int tamanho = dados.length;
        PDU message = new PDU((byte)1,(byte)0,PDU.REGISTER,null,tamanho,dados);

    	Socket masterConnection = new Socket(Server.masterIP,Server.masterPort);
        InputStream dIn = masterConnection.getInputStream();
        OutputStream dOut = masterConnection.getOutputStream();
        dOut.write(message.writeMessage());
        masterConnection.setSoTimeout(20000);
        PDU resposta = PDU.readMessage(dIn);
        if(resposta.getType() == PDU.ACK){
        	System.out.println("Registo no master sucedido");
        	masterConnection.close();
        }
        else{
        	masterConnection.close();
        	throw new IOException();
        }
	}



	public static void main(String[] args){
        try{
            new Server().start();
        }
        catch(IOException e){
                e.printStackTrace();
        }
    }

	
}
