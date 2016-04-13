import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class PDU {

	public static final byte REGISTER = 1;
	public static final byte CONSULT_REQUEST = 2;
	public static final byte CONSULT_RESPONSE = 3;
	public static final byte PROBE_REQUEST = 4;
	public static final byte PROBE_RESPONSE = 5;
	public static final byte REQUEST = 6;
	public static final byte DATA = 7;
	public static final byte IN = 8;
	public static final byte OUT = 9;
	public static final byte PING = 10;
	public static final byte ACK = 11;
	public static final byte NACK = 12;

	private static final int MAX_DADOS = 48*1024-11;
	private byte versao;
	private byte seguranca;
	private byte tipo;
	private byte[] opcoes;
	private byte[] tamanho;
	private byte[] dados;
	
	public byte getType(){
		return this.tipo;
	}
	
	public byte getTipo(){
		return this.dados[0];
	}
	
	public byte[] getDados(){
		return this.dados;
	}
	
	public byte[] getTamanho(){
		return this.tamanho;
	}
	
	public InetAddress getIP() throws UnknownHostException{
		byte[] d = new byte[4];
		for(int i = 1;i<5;i++){
			d[i-1]=this.dados[i];
		}
		return InetAddress.getByAddress(d);
	}
	
	public int getPorta(){
		byte[] porta = new byte[4];
		for (int i = 5;i<9;i++){
			porta[i-5]=this.dados[i];
		}
		return ByteBuffer.wrap(porta).getInt();
	}
	
	public String getID(){
		int sizeOfDados = this.dados.length;
		byte[] id = new byte[sizeOfDados-9];
		for (int i = 9;i<sizeOfDados;i++){
			id[i-9] = this.dados[i];
		}
		String s = new String(id);
		return s;
	}
	
	public PDU(byte versao,byte seguranca,byte tipo,byte[] opcoes,int sizeDados,byte[] dados){
		this.versao=versao;
		this.seguranca=seguranca;
		this.tipo=tipo;
		this.opcoes=new byte[4];
		
		if(opcoes!=null){
			for(int i = 0;i<4 && i<opcoes.length; i++){
				this.opcoes[i]=opcoes[i];
			}
		}
		
		if(dados!=null){
			
			if(sizeDados > MAX_DADOS) sizeDados = MAX_DADOS;
			this.dados=new byte[sizeDados];
				for(int i = 0;i<sizeDados; i++){
					this.dados[i]=dados[i];
				}
			this.tamanho=toBytes(sizeDados);
		}
		else {
			this.dados = new byte[0];
			this.tamanho = toBytes(0);
		}
		
		
		
	}
	
	public PDU(byte[] cabecalho,byte[] dados){
		this.versao = cabecalho[0];
		this.seguranca = cabecalho[1];
		this.tipo = cabecalho[2];
		this.opcoes = new byte[4];
		for (int i = 3;i<7;i++){
			this.opcoes[i-3] = cabecalho[i];
		}
		this.tamanho = new byte[4];
		for (int i = 7;i<11;i++){
			this.tamanho[i-7] = cabecalho[i];
		}
		this.dados = new byte[dados.length];
		for(int i = 0;i<dados.length;i++){
			this.dados[i]=dados[i];
		}
	}
	
	public byte[] writeMessage() {
		byte[] message = new byte[1 + 1 + 1 + 4 + 4 + dados.length];
		message[0]=versao;
		message[1]=seguranca;
		message[2]=tipo;
		for(int i = 0;i<4; i++){
			message[i+3]=opcoes[i];
		}
		
		byte[] tamanhoDados=tamanho;
		for(int i = 7;i<11;i++){
			message[i]=tamanhoDados[i-7];
		}
		
		if(dados.length>0){
		for(int i = 0;i<dados.length; i++){
			message[i+3+4+4]=dados[i];
		}
		}
		
		return message;
	}
	
	private byte[] toBytes(int i)
	{
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i >> 0);

	  return result;
	}

}
