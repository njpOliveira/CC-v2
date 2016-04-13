import java.net.InetAddress;

public class Registo {

	
	private String id;
	private InetAddress ip;
	private int port;
	
	public Registo(String id, InetAddress ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
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
	
	public void imprime(){
		System.out.println(this.port);
	}
	

	
}
