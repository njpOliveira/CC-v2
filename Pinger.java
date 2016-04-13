import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

public class Pinger implements Runnable{

	private Socket cliente;
	private InputStream dIn;
	private OutputStream dOut;
	private Map<String,Registo> registos;
	private String idCliente;
        public static final int INTERVALO_PINGS = 10000;
	
	public Pinger(Socket cliente){
		this.cliente = cliente;
	}
	
	@Override
	public void run() {
            PDU ping = new PDU((byte)1,(byte)0,PDU.PING,null,0,null);
            byte[] ping_message = ping.writeMessage();
            try{
                this.dOut = cliente.getOutputStream();
                while(!cliente.isClosed()){
                    Thread.sleep(this.INTERVALO_PINGS);
                    if(!cliente.isClosed()){
                        this.dOut.write(ping_message);
                    }
                }	
            }
            catch (Exception e) {
                    //e.printStackTrace();
            }	
	}
}
