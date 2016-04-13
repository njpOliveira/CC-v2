
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Map;

public class ServerThread implements Runnable {
    private InputStream dIn;
    private OutputStream dOut;
    private Map<String,Registo> registos;
    private Socket cliente;
    private String idCliente;
    private int TIMEOUT = 1000*4;

    public ServerThread(Map<String,Registo> reg,Socket client) throws IOException{
            this.registos = reg;
            this.cliente = client;
            this.dOut = cliente.getOutputStream();
            this.dIn = cliente.getInputStream();

    }


    public void run(){
        boolean remover = false;        
        try{
            cliente.setSoTimeout(Pinger.INTERVALO_PINGS + this.TIMEOUT);
            while(!this.cliente.isClosed()){
                byte[] cabecalho = new byte[11];
                for(int i = 0;i<11;i++){
                        cabecalho[i]= (byte) dIn.read();
                }

                byte[] tamanho = new byte[4];
                for (int i = 7;i<11;i++){
                        tamanho[i-7] = cabecalho[i];
                }

                int tamanhoInt = ByteBuffer.wrap(tamanho).getInt();
                byte[] dados = new byte[tamanhoInt];

                for (int i = 0;i<tamanhoInt;i++){
                        dados[i] = (byte) dIn.read();
                }

                PDU p = new PDU(cabecalho,dados);
                switch(p.getType()){
                    case PDU.REGISTER:
                        if(p.getTipo()==PDU.IN){
                            checkRegisto(p);
                        }
                        else if(p.getTipo()==PDU.OUT){
                            checkLogout(p);
                        }
                        break;
                    case PDU.ACK:
                        System.out.println(idCliente+" - ACK");
                        break;
                }
            }
        } 
        catch(SocketTimeoutException e){
            remover = true;
        }
        catch(IOException e){
            remover = true;
        }
        if(remover){
            synchronized(this.registos){
                this.registos.remove(idCliente);
            }
            System.out.println("removi cliente "+idCliente);
            System.out.println("Utilizadores registados: ");
            for(String key: this.registos.keySet()){
                    String s_ip = this.registos.get(key).getIp().toString();
                    int s_porta = this.registos.get(key).getPort();
                    System.out.println(key + " - " + s_ip + ":" + s_porta );
            }
        }
    }

    void checkRegisto(PDU p) throws IOException{
            InetAddress ip = p.getIP();
            int porta = p.getPorta();
            idCliente = p.getID();
            Registo r = new Registo(idCliente,ip,porta);
            if(registos.containsKey(idCliente)) {
                    PDU idAlreadyUsed = new PDU((byte)1,(byte)0,PDU.NACK,null,0,null);
                    dOut.write(idAlreadyUsed.writeMessage());
            }
            else {
                synchronized(this.registos){
                    registos.put(idCliente,r);
                }
                PDU registerSuccess = new PDU((byte)1,(byte)0,PDU.ACK,null,0,null);
                dOut.write(registerSuccess.writeMessage());
                Thread t = new Thread(new Pinger(cliente));
                t.start();
            }
            System.out.println("Utilizadores registados: ");
            for(String key: this.registos.keySet()){
                    String s_ip = this.registos.get(key).getIp().toString();
                    int s_porta = this.registos.get(key).getPort();
                    System.out.println(key + " - " + s_ip + ":" + s_porta );
            }
    }

    void checkLogout(PDU p) throws IOException {
            InetAddress ip = p.getIP();
            int porta = p.getPorta();
            String id = p.getID();
            dOut = cliente.getOutputStream();
            if(registos.containsKey(id)) {
                    synchronized(registos){
                        registos.remove(id);
                    }
                    System.out.println("Utilizadores registados: ");
                    for(String key: this.registos.keySet()){
                            String s_ip = this.registos.get(key).getIp().toString();
                            int s_porta = this.registos.get(key).getPort();
                            System.out.println(key + " - " + s_ip + ":" + s_porta );
                    }
                    //System.out.println(registos.size());
            }
            else {
                    ;
            }
    }
}
