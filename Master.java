import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.io.*;


public class Master {

    private Set<InetSocketAddress> registos;
    private static ServerSocket ss;
    public static final int port = 5846;
	
    public void start() throws IOException{
        this.registos = new HashSet<>();
        ss = new ServerSocket(port);

        while(true){
            Socket servidorDominio = null;
            try {
            	servidorDominio = ss.accept();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Thread t = new Thread(new MasterThread(registos,servidorDominio));
            t.start();
        }
    }
	
    public static void main(String[] args){
        try{
            new Master().start();
        }
        catch(IOException e){
                e.printStackTrace();
        }
    }

	
}
