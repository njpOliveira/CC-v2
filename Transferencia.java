import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;

public class Transferencia {
	
	private InetSocketAddress cliente;
	private String musica;
	private int numSegmentos;
	private TreeMap<Integer,byte[]> segmentos;
	
	public Transferencia(InetSocketAddress cliente, String musica) throws IOException{
		this.cliente = cliente;
		this.musica = musica;
		this.segmentos = new TreeMap<>();
		
		// Carregar dados do ficheiro 
		Path filePath = Paths.get(Client.pathMusicas+musica);
		byte[] dadosFicheiro = Files.readAllBytes(filePath);
		this.numSegmentos = dadosFicheiro.length / PDU.MAX_DADOS + ((dadosFicheiro.length % PDU.MAX_DADOS == 0) ? 0 : 1);
		for(int i = 0; i<numSegmentos; i++){
			int tamanhoSegmento = PDU.MAX_DADOS;
			if(i == numSegmentos-1){
				tamanhoSegmento = dadosFicheiro.length % PDU.MAX_DADOS;
				if(tamanhoSegmento == 0) tamanhoSegmento = PDU.MAX_DADOS;
			}
			byte[] segmento = new byte[tamanhoSegmento];
			for(int j = 0; j<tamanhoSegmento; j++){
				segmento[j] = dadosFicheiro[i*PDU.MAX_DADOS + j];
			}
			this.segmentos.put(i, segmento);
		}

	}
	

	public InetSocketAddress getCliente() {
		return cliente;
	}

	public void setCliente(InetSocketAddress cliente) {
		this.cliente = cliente;
	}

	public String getMusica() {
		return musica;
	}

	public void setMusica(String musica) {
		this.musica = musica;
	}

	public int getNumSegmentos() {
		return numSegmentos;
	}

	public void setNumSegmentos(int numSegmentos) {
		this.numSegmentos = numSegmentos;
	}

	public TreeMap<Integer, byte[]> getSegmentos() {
		return segmentos;
	}

	public void setSegmentos(TreeMap<Integer, byte[]> segmentos) {
		this.segmentos = segmentos;
	}
	
}
