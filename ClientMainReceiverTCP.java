import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientMainReceiverTCP implements Runnable{
	
    private ServerSocket ssocket;


	public ClientMainReceiverTCP(ServerSocket ssocket){
		this.ssocket = ssocket;
	}
	
	@Override
	public void run() {
        while(true){
            Socket socket = null;
            try {
				socket = ssocket.accept();
	            Thread t = new Thread(new ClientReceiverTCP(socket));
	            t.start();
			} catch (IOException e) {}
        }
	}
}
