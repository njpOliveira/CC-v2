
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;

public class ServerThread implements Runnable {
    private InputStream dIn;
    private OutputStream dOut;
    private Map<String,Registo> registos;
    private Socket cliente;
    private String idCliente;

    public ServerThread(Map<String,Registo> reg,Socket client) throws IOException{
            this.registos = reg;
            this.cliente = client;
            this.dOut = cliente.getOutputStream();
            this.dIn = cliente.getInputStream();

    }


    public void run(){
        try{
            cliente.setSoTimeout((int)2.5*Pinger.INTERVALO_PINGS);
            while(!this.cliente.isClosed()){
                byte[] cabecalho = new byte[11];
                for(int i = 0;i<11;i++){
                        cabecalho[i]= (byte) dIn.read();
                }

                byte[] tamanho = new byte[4];
                for (int i = 7;i<11;i++){
                        tamanho[i-7] = cabecalho[i];
                }

                int tamanhoInt = ByteBuffer.wrap(tamanho).getInt();
                byte[] dados = new byte[tamanhoInt];

                for (int i = 0;i<tamanhoInt;i++){
                        dados[i] = (byte) dIn.read();
                }

                PDU p = new PDU(cabecalho,dados);
                switch(p.getType()){
                    case PDU.REGISTER:
                        if(p.getDataType()==PDU.IN){
                            checkRegisto(p);
                        }
                        else if(p.getDataType()==PDU.OUT){
                            checkLogout(p);
                        }
                        break;
                    case PDU.CONSULT_REQUEST:                    	        	
                    	consultRequest(p);                    	                  	
                    	break;
                    case PDU.ACK:
                        break;
                }
            }
        } 
        catch(IOException e){
            synchronized(this.registos){
                this.registos.remove(idCliente);
            }
            printUtilizadores();
        	try{
            	this.cliente.close();
            }
            catch(IOException ioe){}
        }
    }

    private void consultRequest(PDU p) throws IOException {
    	
    	/*
    	 * TODO Perguntar a cada cliente se tem a musica e responder com CONSULT_RESPONSE.
    	 * 		Para ja, responde sempre que nao encontrou a musica.
    	 */
    	
		byte[] data = {PDU.NOT_FOUND};
		PDU response = new PDU((byte)1,(byte)2,PDU.CONSULT_RESPONSE,null,data.length,data);
		dOut.write(response.writeMessage());		
	}


	void checkRegisto(PDU p) throws IOException{
            InetAddress ip = p.getIP();
            int porta = p.getPorta();
            idCliente = p.getID();
            Registo r = new Registo(idCliente,ip,porta,cliente);
            if(registos.containsKey(idCliente)) {
                    PDU idAlreadyUsed = new PDU((byte)1,(byte)0,PDU.NACK,null,0,null);
                    dOut.write(idAlreadyUsed.writeMessage());
            }
            else {
                synchronized(this.registos){
                    registos.put(idCliente,r);
                }
                PDU registerSuccess = new PDU((byte)1,(byte)0,PDU.ACK,null,0,null);
                dOut.write(registerSuccess.writeMessage());
            }
            printUtilizadores();
    }

    void checkLogout(PDU p) throws IOException {
            String id = p.getID();
            dOut = cliente.getOutputStream();
            if(registos.containsKey(id)) {
                    synchronized(registos){
                        registos.remove(id);
                    }
                    printUtilizadores();
            }
    }
    
    private void printUtilizadores(){
    	System.out.println("Utilizadores registados: ");
        for(String key: this.registos.keySet()){
                String s_ip = this.registos.get(key).getIp().toString();
                int s_porta = this.registos.get(key).getPort();
                System.out.println(key + " - " + s_ip + ":" + s_porta );
        }
    }
}
