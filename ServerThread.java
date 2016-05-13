
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
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
                    case PDU.CONSULT_RESPONSE:
                    	this.registos.get(idCliente).getBuffer().push(p);
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
    	for(Registo registo: this.registos.values()){
    		if(!registo.getId().equals(this.idCliente)){
    			try{
    				PDUBuffer buffer = registo.getBuffer();
    				synchronized (buffer) {
    					registo.getdOut().write(p.writeMessage());
						PDU respostaCliente = buffer.nextMessage();
						if(respostaCliente.getDataType() == PDU.FOUND){
							clientesComMusica.add(registo);
						}
					}
    			}
    			catch(IOException e){}
    		}
    	}
    	
    	byte[] data;
    	// Musica nao foi encontrada em nenhum cliente
    	if(clientesComMusica.isEmpty()){
    		data = new byte[1];
			data[0] = PDU.NOT_FOUND;
    	}
    	// Existem clientes que possuem a musica
    	else{
    		int numClientes = clientesComMusica.size();
    		byte[] nClientes = PDU.toBytes(numClientes);
    		data = new byte[5];
    		data[0] = PDU.FOUND;
    		for(int i = 0; i < 4; i++){
    			data[i+1] = nClientes[i];
    		}
    		
    		Iterator<Registo> it = clientesComMusica.iterator();
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
		}
		try {
			PDU response = new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);    	
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
