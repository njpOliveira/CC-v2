
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
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
                PDU p = PDU.readMessage(dIn);
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
                    case PDU.CONSULT_REQUEST_FROM_MASTER:
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

    private void consultRequest(PDU p){
    	
    	HashSet<Registo> clientesComMusica = new HashSet<>();
    	
    	// Perguntar a cada cliente se tem a musica
    	Iterator<Registo> it = this.registos.values().iterator();
    	while(it.hasNext() && clientesComMusica.size() < Server.MAX_HITS){
    		Registo registo = (Registo)it.next();
    		if(!registo.getId().equals(this.idCliente)){
    			Socket socket = null;
    			try {
					socket = new Socket(registo.getIp(),registo.getPort());
					socket.setSoTimeout(5000);
					OutputStream output = socket.getOutputStream();
					InputStream input = socket.getInputStream();
					output.write(p.writeMessage());
					PDU respostaCliente = PDU.readMessage(input);
					if(respostaCliente.getDataType() == PDU.FOUND){
						clientesComMusica.add(registo);
					}
					
				} catch (IOException e) {}
    			finally{
    				try{
    					socket.close();
    				}
    				catch(Exception e2){}
    			}
    		}
    	}
    	
    	
    	PDU response = null;
    	// Musica nao foi encontrada em nenhum cliente
    	if(clientesComMusica.isEmpty()){
    		if(p.getType() == PDU.CONSULT_REQUEST_FROM_MASTER){
        		// Pedido foi feito pelo master
        		// Enviar NOT_FOUND
        		byte[] data = new byte[1];
    			data[0] = PDU.NOT_FOUND;
    			response = new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);
    		}
    		else{
    		// Recaminhar pedido para o master
    			
    		}
    	}
    	// Existem clientes que possuem a musica
    	else{
    		// Enviar pdu do tipo CONSULT_RESPONSE ao cliente
    		int numClientes = clientesComMusica.size();
    		byte[] nClientes = PDU.toBytes(numClientes);
    		byte[] data = new byte[5];
    		data[0] = PDU.FOUND;
    		for(int i = 0; i < 4; i++){
    			data[i+1] = nClientes[i];
    		}
    		
    		it = clientesComMusica.iterator();
    		while(it.hasNext()){
    			Registo registo = it.next();
    			byte[] id = registo.getId().getBytes();
    			byte[] ip = registo.getIp().getAddress();
    			byte[] porta = PDU.toBytes(registo.getPort());
    			int comp = id.length + ip.length + porta.length;
    			byte[] comprimento = PDU.toBytes(comp);
    			
    			byte[] cliente = new byte[comprimento.length + comp];
    			for(int i = 0; i < comprimento.length; i++){
    				cliente[i] = comprimento[i];
    			}
    			int j = comprimento.length;
    			for(int i = 0; i < ip.length; i++){
    				cliente[i+j] = ip[i];
    			}
    			j += ip.length;
    			for(int i = 0; i < porta.length; i++){
    				cliente[i+j] = porta[i];
    			}
    			j += porta.length;
    			for(int i = 0; i< id.length; i++){
    				cliente[i+j] = id[i];
    			}
    			
    			byte[] aux = new byte[data.length + cliente.length];
    			for(int i = 0; i < data.length; i++){
    				aux[i] = data[i];
    			}
    			j = data.length;
    			for(int i = 0; i < cliente.length; i++){
    				aux[i+j] = cliente[i];
    			}
    			data = aux;
    		}
			response = new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);
		}
		try {
			dOut.write(response.writeMessage());
		} catch (IOException e) {}		
	}


	void checkRegisto(PDU p) throws IOException{
            InetAddress ip = p.getIP();
            int porta = p.getPorta();
            String id = p.getID();
            Registo r = new Registo(id,ip,porta,cliente);
            if(registos.containsKey(id)) {
                    PDU idAlreadyUsed = new PDU((byte)1,(byte)0,PDU.NACK,null,0,null);
                    dOut.write(idAlreadyUsed.writeMessage());
            }
            else {
                synchronized(this.registos){
                	this.idCliente = id;
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
