import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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

	private void consultRequest(PDU p) {
		// TODO Auto-generated method stub
		
	}
	
    private void printDominios(){
    	System.out.println("Dominios registados: ");
        for(InetSocketAddress dominio: this.registos){
                System.out.println(dominio.toString());
        }
    }
	
}