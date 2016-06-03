import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MasterThread implements Runnable{

    private InputStream dIn;
    private OutputStream dOut;
    private Set<InetSocketAddress> registos;
    private Socket sDominio;

    public MasterThread(Set<InetSocketAddress> registos,Socket sDominio) throws IOException{
            this.registos = registos;
            this.sDominio = sDominio;
            this.dOut = sDominio.getOutputStream();
            this.dIn = sDominio.getInputStream();
    }
    
	@Override
	public void run() {
        try{
            while(!this.sDominio.isClosed()){
                PDU p = PDU.readMessage(dIn);
                switch(p.getType()){
                    case PDU.REGISTER:
                        InetAddress ip = p.getIP();
                        int porta = p.getPorta();
                        InetSocketAddress registo = new InetSocketAddress(ip, porta);
                        synchronized(this.registos){
                        	this.registos.add(registo);
                        }
                        PDU registerSuccess = new PDU((byte)1,(byte)0,PDU.ACK,null,0,null);
                        dOut.write(registerSuccess.writeMessage());
                        printDominios();
                        break;
                    case PDU.CONSULT_REQUEST:                    	        	
                    	consultRequest(p);                    	                  	
                    	break;
                    case PDU.ACK:
                        break;
                }
            }
        } 
        catch(Exception e){}		
	}

	private void consultRequest(PDU p) throws IOException {
		byte[] dados = p.getDados();
		byte[] musica = new byte[dados.length-8];
		byte[] ip = new byte[4];
		byte[] port = new byte[4];
		for(int i = 0; i<dados.length-8; i++){
			musica[i] = dados[i];
		}
		for(int i = 0; i<4; i++){
			int j = i+musica.length;
			ip[i] = dados[j];
		}
		for(int i = 0; i<4; i++){
			int j = i+musica.length+4;
			port[i] = dados[j];
		}
		InetSocketAddress enderecoServidor = new InetSocketAddress(InetAddress.getByAddress(ip), PDU.toInt(port));
		
		PDU request = new PDU((byte)1,(byte)0,PDU.CONSULT_REQUEST_FROM_MASTER,null,musica.length,musica); 
		byte[] request_message = request.writeMessage();
		HashSet<Registo> clientes = new HashSet<>(); 

		Iterator<InetSocketAddress> it = this.registos.iterator();
		while(it.hasNext() && clientes.size() < Master.MAX_HITS){
			InetSocketAddress endereco = it.next();
			if(!enderecoServidor.equals(endereco)){
				Socket s = null;
				try{
					s = new Socket(endereco.getAddress(), endereco.getPort());
					OutputStream sOut = s.getOutputStream();
					InputStream sIn = s.getInputStream();
					sOut.write(request_message);
					s.setSoTimeout(5000);
					PDU response = PDU.readMessage(sIn);
					HashSet<Registo> clientesResposta = response.getConsultResponseClients();
					clientes.addAll(clientesResposta);
				}
				catch(Exception e){
					e.printStackTrace();
					s.setSoTimeout(0);
				}
			}
		}
		
		if(clientes.isEmpty()){
			System.out.println("Musica nao encontrada");
		}
		else{
			System.out.println("Clientes com musica: "+clientes.size());
		}
		
		try{
			if(!clientes.isEmpty()){
				dOut.write(PDU.consultResponse(clientes).writeMessage());
			}
			else{
				byte[] data = new byte[1];
    			data[0] = PDU.NOT_FOUND;
    			PDU response = new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);
    			dOut.write(response.writeMessage());
			}
		}
		catch(IOException e){}
	}
	
    private void printDominios(){
    	System.out.println("Dominios registados: ");
        for(InetSocketAddress dominio: this.registos){
                System.out.println(dominio.toString());
        }
    }
	
}