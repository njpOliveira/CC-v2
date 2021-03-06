import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
	public static final byte FOUND = 13;
	public static final byte NOT_FOUND = 14;
	public static final byte SYN = 15;
	public static final byte SYN_ACK = 16;
	public static final byte FIN = 17;
	public static final byte OK = 18;
	public static final byte KO = 19;
	public static final byte SREJ = 20;
	public static final byte REQUEST_RESPONSE = 21;
	public static final byte CONSULT_REQUEST_FROM_MASTER = 22;



	public static final int MAX_SIZE = 48*1024;
	public static final int MAX_DADOS = 48*1024-11;

	private byte versao;
	private byte seguranca;
	private byte tipo;
	private byte[] opcoes;
	private byte[] tamanho;
	private byte[] dados;
	
	public byte getType(){
		return this.tipo;
	}
	
	public byte getDataType(){
		return this.dados[0];
	}
	
	public byte[] getDados(){
		return this.dados;
	}
	
	public byte[] getTamanho(){
		return this.tamanho;
	}
	
	public byte[] getOpcoes(){
		return this.opcoes;
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
	
	public PDU(byte[] mensagem){
		
		this.versao = mensagem[0];
		this.seguranca = mensagem[1];
		this.tipo = mensagem[2];
		this.opcoes = new byte[4];
		for (int i = 3;i<7;i++){
			this.opcoes[i-3] = mensagem[i];
		}
        
		this.tamanho = new byte[4];
		for (int i = 7;i<11;i++){
			this.tamanho[i-7] = mensagem[i];
		}

        int tamanhoDados = PDU.toInt(this.tamanho);
        this.dados = new byte[tamanhoDados];

        for(int i = 0;i<tamanhoDados;i++){
                dados[i]= mensagem[i+11];
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
	
	public static PDU readMessage(InputStream dIn) throws IOException{
        byte[] cabecalho = new byte[11];
        for (int i = 0;i<11;i++){
                cabecalho[i] = (byte) dIn.read();
        }

        byte[] bytesSize = new byte[4];
        for(int i = 7;i<11;i++){
                bytesSize[i-7]=cabecalho[i];
        }

        int tamanho = PDU.toInt(bytesSize);
        byte[] dados = new byte[tamanho];

        for(int i = 0;i<tamanho;i++){
                dados[i]=(byte) dIn.read();
        }
        PDU p = new PDU(cabecalho,dados);
        return p;
	}
	
	public static PDU consultResponse(Set<Registo> clientesComMusica){
		int numClientes = clientesComMusica.size();
		byte[] nClientes = PDU.toBytes(numClientes);
		byte[] data = new byte[5];
		data[0] = PDU.FOUND;
		for(int i = 0; i < 4; i++){
			data[i+1] = nClientes[i];
		}
		
		Iterator<Registo> it = clientesComMusica.iterator();
		while(it.hasNext()){
			Registo registo = it.next();
			byte[] id = registo.getId().getBytes();
			byte[] ip = registo.getIp().getAddress();
			byte[] porta = PDU.toBytes(registo.getPort());
			int comp = id.length + ip.length + porta.length;
			byte[] comprimento = PDU.toBytes(comp);
			
			byte[] cliente = new byte[comprimento.length + comp];
			for(int i = 0; i < comprimento.length; i++){
				cliente[i] = comprimento[i];
			}
			int j = comprimento.length;
			for(int i = 0; i < ip.length; i++){
				cliente[i+j] = ip[i];
			}
			j += ip.length;
			for(int i = 0; i < porta.length; i++){
				cliente[i+j] = porta[i];
			}
			j += porta.length;
			for(int i = 0; i< id.length; i++){
				cliente[i+j] = id[i];
			}
			
			byte[] aux = new byte[data.length + cliente.length];
			for(int i = 0; i < data.length; i++){
				aux[i] = data[i];
			}
			j = data.length;
			for(int i = 0; i < cliente.length; i++){
				aux[i+j] = cliente[i];
			}
			data = aux;
		}
		return new PDU((byte)1,(byte)0,PDU.CONSULT_RESPONSE,null,data.length,data);
	}
	
	/*
	 * Formato do array de dados de um PDU ConsultResponse:
	 * 1 byte -> Tipo (Found/Not Found)
	 * 4 bytes -> N�mero de clientes 
	 * 	Para cada cliente:
	 * 	4 bytes -> Comprimento dos dados do cliente
	 * 	4 bytes -> IP
	 * 	4 bytes -> Porta
	 * 	(Comprimento - (4+4)) bytes -> ID
	 */
	public HashSet<Registo> getConsultResponseClients(){
		if(this.tipo != CONSULT_RESPONSE) return new HashSet<>();
		if(this.getDataType() == NOT_FOUND) return new HashSet<>();
				
		HashSet<Registo> clientes = new HashSet<>();
		byte[] numClientes = new byte[4];
		int j = 1;
		for(int i = 0; i < 4; i++){
			numClientes[i] = this.dados[j+i];
		}
		j += 4;
		int nClientes = toInt(numClientes);
		for(int n = 0; n < nClientes; n++){
			byte[] comp = new byte[4];
			for(int i = 0; i < 4; i++){
				comp[i] = this.dados[j+i];
			}
			j += 4;
			int comprimento = toInt(comp);
			
			byte[] ip = new byte[4];
			for(int i = 0; i<4; i++){
				ip[i] = this.dados[j+i];
			}
			j += 4;
			
			byte[] porta = new byte[4];
			for(int i = 0; i<4; i++){
				porta[i] = this.dados[j+i];
			}
			j += 4;
			
			int comprimentoID = comprimento - (4 + 4);
			byte[] id = new byte[comprimentoID];
			for(int i = 0; i < comprimentoID ; i++){
				id[i] = this.dados[j+i];
			}
			j += comprimentoID;
			
			try {
				InetAddress ipAddress = InetAddress.getByAddress(ip);
				String stringID = new String(id);
				int intPorta = toInt(porta);
				Registo reg = new Registo(stringID,ipAddress,intPorta);
				clientes.add(reg);
			} catch (UnknownHostException e) {}
		}	
		return clientes;
	}
	
	public int getDataSegmentIndex(){
		if(this.tipo != DATA) return -1;
		
		else return toInt(this.opcoes);
	}
	
	/*
	 * Posicoes 4 a 7 do array de dados
	 */
	public int getRequestResponseNumberOfSegments(){
		if(this.tipo != REQUEST_RESPONSE) return -1;
		
		else{
			byte[] numberOfSegments = new byte[4];
			for(int i = 4; i<8; i++){
				numberOfSegments[i-4] = this.dados[i];
			}
			return toInt(numberOfSegments);
		}
	}
	
	/*
	 * Posicoes 0 a 3 do array de dados
	 */
	public int getRequestResponsePort(){
		if(this.tipo != REQUEST_RESPONSE) return -1;
		
		else{
			byte[] numberOfSegments = new byte[4];
			for(int i = 0; i<4; i++){
				numberOfSegments[i] = this.dados[i];
			}
			return toInt(numberOfSegments);
		}
	}

	public int getRejectSegment() {
		if(this.tipo != SREJ) return -1;
		
		else return toInt(this.dados);
	}
	
	public String getRequestSong(){
		return new String(this.dados);
	}
	
	public long getProbeResponseTimestamp(){
		if(this.tipo != PROBE_RESPONSE) return Long.MIN_VALUE;
		
		return toLong(this.dados);
	}
	
	public static byte[] toBytes(int i)
	{
	  byte[] result = new byte[4];

	  result[0] = (byte) (i >> 24);
	  result[1] = (byte) (i >> 16);
	  result[2] = (byte) (i >> 8);
	  result[3] = (byte) (i >> 0);

	  return result;
	}
	
	public static byte[] toBytes(long l) {
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	        result[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
	    return result;
	}
	
	public static int toInt(byte[] b){
		return ByteBuffer.wrap(b).getInt();
	}
	
	public static long toLong(byte[] b) {
	    long result = 0;
	    for (int i = 0; i < 8; i++) {
	        result <<= 8;
	        result |= (b[i] & 0xFF);
	    }
	    return result;
	}

}
