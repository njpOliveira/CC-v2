import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PDUBuffer {
	
	private ArrayList<PDU> mensagens;
	private ReentrantLock rl;
	private Condition semMensagens;
	
	public PDUBuffer(){
		this.mensagens = new ArrayList<>();
		this.rl = new ReentrantLock();
		this.semMensagens = this.rl.newCondition();
	}
	
	public PDU nextMessage(){
		this.rl.lock();
		
		//Esperar por uma mensagem
		while(this.mensagens.isEmpty()){
			try {
				this.semMensagens.await();
			} catch (InterruptedException e) {
				// Ignorar
			}
		}
		
		//Existe mensagem disponivel
		PDU ret = this.mensagens.get(0);
		this.mensagens.remove(0);
		
		this.rl.unlock();
		return ret;
	}
	
	public void push(PDU pdu){
		this.rl.lock();
		this.mensagens.add(pdu);
		this.semMensagens.signalAll();
		this.rl.unlock();
	}

}
