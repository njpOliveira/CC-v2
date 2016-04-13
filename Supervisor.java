import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Supervisor implements Runnable{

	private Socket cliente;
	private InputStream dIn;
	private OutputStream dOut;
	private Map<String,Registo> registos;
	private String idCliente;
	
	public Supervisor(Socket cliente, String idCliente, Map<String,Registo> registos){
		this.cliente = cliente;
		this.registos = registos;
		this.idCliente = idCliente;
		try{
			this.dOut = cliente.getOutputStream();
			this.dIn = cliente.getInputStream();
		}
		catch(Exception e){
		}
	}
	
	@Override
	public void run() {
		while(true){
			try {
				Thread.sleep(1000*10);
				PDU ping = new PDU((byte)1,(byte)0,PDU.PING,null,0,null);
				this.dOut.write(ping.writeMessage());
				
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}
}
