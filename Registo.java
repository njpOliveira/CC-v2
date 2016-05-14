import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Registo {

	
	private String id;
	private InetAddress ip;
	private int port;
	
	private Socket socket;
    private OutputStream dOut;
    
	public Registo(String id, InetAddress ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.socket = null;
		this.dOut = null;
	}
	
	public Registo(String id, InetAddress ip, int port, Socket socket) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.setSocket(socket);
		try{
			this.setdOut(socket.getOutputStream());
		}
		catch(IOException e){}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public InetAddress getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public OutputStream getdOut() {
		return dOut;
	}

	public void setdOut(OutputStream dOut) {
		this.dOut = dOut;
	}
	
}
