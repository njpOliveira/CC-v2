import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class Transferencia {
	
	private InetSocketAddress cliente;
	private int tamanhoJanela;
	private int janelaAtual;
	private String musica;
	private int numSegmentos;
	private int ultimoSegmento;
	private TreeMap<Integer,byte[]> segmentos;
	private boolean terminado;
	
	public Transferencia(InetSocketAddress cliente, int tamanhoJanela, String musica) throws IOException{
		this.cliente = cliente;
		this.tamanhoJanela = tamanhoJanela;
		this.musica = musica;
		
		// Carregar dados do ficheiro 
		Path filePath = Paths.get(musica);
		byte[] dadosFicheiro = Files.readAllBytes(filePath);
		int numSegmentos = dadosFicheiro.length / PDU.MAX_DADOS + ((dadosFicheiro.length % PDU.MAX_DADOS == 0) ? 0 : 1);
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
		
		this.janelaAtual = tamanhoJanela;
		this.ultimoSegmento = -1;
		this.terminado = false;
	}
	
	public void receiverReady(int segmento){
		if(segmento > this.ultimoSegmento){
			int confirmados = this.ultimoSegmento - (segmento-1);
			this.janelaAtual += confirmados;
			if(this.janelaAtual > this.tamanhoJanela)
				this.janelaAtual = tamanhoJanela;
			this.ultimoSegmento = segmento-1;
		}
	}
	
	public boolean isFinished(){
		return this.terminado;
	}
	
	public Map<Integer,byte[]> sendMax(){
		TreeMap<Integer,byte[]> mensagens = new TreeMap<>();
		
		int numMensagens = this.tamanhoJanela - (this.tamanhoJanela - this.janelaAtual);
		for(int i = 0; i<numMensagens && !this.terminado; i++){
			int segIndex = this.ultimoSegmento + 1 + i;
			if(segIndex < this.numSegmentos){
				mensagens.put(segIndex, this.segmentos.get(segIndex));
			}
			else if(segIndex + 1 >= numSegmentos){
				this.terminado = true;
			}
		}
		
		this.janelaAtual = 0;

		return mensagens;
	}

	public InetSocketAddress getCliente() {
		return cliente;
	}

	public void setCliente(InetSocketAddress cliente) {
		this.cliente = cliente;
	}

	public int getTamanhoJanela() {
		return tamanhoJanela;
	}

	public void setTamanhoJanela(int tamanhoJanela) {
		this.tamanhoJanela = tamanhoJanela;
	}

	public int getJanelaAtual() {
		return janelaAtual;
	}

	public void setJanelaAtual(int janelaAtual) {
		this.janelaAtual = janelaAtual;
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

	public int getUltimoSegmento() {
		return ultimoSegmento;
	}

	public void setUltimoSegmento(int ultimoSegmento) {
		this.ultimoSegmento = ultimoSegmento;
	}

	public TreeMap<Integer, byte[]> getSegmentos() {
		return segmentos;
	}

	public void setSegmentos(TreeMap<Integer, byte[]> segmentos) {
		this.segmentos = segmentos;
	}
	
	

}
