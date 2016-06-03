import java.io.OutputStream;
import java.util.Map;

public class Pinger implements Runnable{
	
    private Map<String,Registo> registos;
    
    public static final int INTERVALO_PINGS = 15000;
	
	public Pinger(Map<String,Registo> registos){
		this.registos = registos;
	}
	
	@Override
	public void run() {
            PDU ping = new PDU((byte)1,(byte)0,PDU.PING,null,0,null);
            byte[] ping_message = ping.writeMessage();
            OutputStream out;
        	while(true){
        		for(Registo registo: registos.values()){
        			try{
        				out = registo.getdOut();
        				out.write(ping_message);
        			}
                    catch (Exception e) {}	
        		}
        		try {
					Thread.sleep(Pinger.INTERVALO_PINGS);
				} catch (InterruptedException e) {}
        	}
	}
}
