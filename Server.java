import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;


public class Server {

    private Map<String,Registo> registos;
    private static ServerSocket servidor;
    public static final int port = 6789;
	
    public void start() throws IOException{
        this.registos = new HashMap<>();
        servidor = new ServerSocket(port);

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
	
    public static void main(String[] args){
        try{
            new Server().start();
        }
        catch(IOException e){
                e.printStackTrace();
        }
    }

	
}
