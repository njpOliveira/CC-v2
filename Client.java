import java.net.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.io.*;

public class Client {

    private Socket clientSocket;
    private ServerSocket socketReceiver;
    private String id;
    private OutputStream dOut;
    @SuppressWarnings("unused")
	private InputStream dIn;
    private PDUBuffer mensagens;

    private Scanner s = new Scanner(System.in);
    
    public static final String ip = "localhost";
    public static final String pathMusicas = System.getProperty("user.dir")+"\\kit_TP2\\";


    protected static final int portaServidor = 6789;

    private PDU register(byte tipo){
            int porto = socketReceiver.getLocalPort();

            InetAddress ip = clientSocket.getInetAddress();
            byte[] bytesIP = ip.getAddress();

            byte[] bytesID = this.id.getBytes();

            byte[] bytesPORT = PDU.toBytes(porto);

            int sizeOfdados = 1 + bytesIP.length + bytesPORT.length + bytesID.length ;
            byte[] dados = new byte[sizeOfdados];

            dados[0] = tipo;

            int j;

            for(int i = 1;i<(bytesIP.length)+1;i++){
                    j=i-1;
                    dados[i] = bytesIP[j];
            }

            for(int i = 1+bytesIP.length;i<1+bytesIP.length+bytesPORT.length;i++){
                    j=i-1-bytesIP.length;
                    dados[i] = bytesPORT[j];
            }

            for(int i = 1+bytesIP.length+bytesPORT.length;i<1+bytesIP.length+bytesPORT.length+bytesID.length;i++){
                    j=i-1-bytesIP.length-bytesPORT.length;
                    dados[i] = bytesID[j];
            }

            int tamanho = dados.length;
            PDU message = new PDU((byte)1,(byte)0,PDU.REGISTER,null,tamanho,dados);
            return message;

    }

    public void start(){
            mensagens = new PDUBuffer();
            System.out.println("Insira o seu ID");
            id = s.nextLine();

            try{
                socketReceiver = new ServerSocket(0);
                clientSocket = new Socket(ip,portaServidor);
                Thread t = new Thread(new ClientListener(clientSocket,mensagens));
                t.start();
                PDU registo = this.register(PDU.IN);
                dOut = clientSocket.getOutputStream();
                dIn = clientSocket.getInputStream();

                dOut.write(registo.writeMessage());

                PDU mensagem = mensagens.nextMessage();

                if(mensagem.getType()==PDU.ACK) {
                        System.out.println("Registo efectuado com sucesso");
                        Thread receiverUDP = new Thread(new ClientReceiverUDP(socketReceiver.getLocalPort()));
                        receiverUDP.start();
                        Thread receiverTCP = new Thread(new ClientMainReceiverTCP(socketReceiver));
                        receiverTCP.start();
                        menu2();
                }
                if(mensagem.getType()==PDU.NACK){
                        System.out.println("Ja existe um utilizador com o seu id");
                        menu1();
                }
            }
            catch(IOException e){
                    System.out.println("Ligacao perdida. Sessao terminada");
            }
            finally{
                try{
                    clientSocket.close();
                }
                catch(Exception e){}
                s.close();
            }	
    }

	/* Menu antes de registo */
    public void menu1() throws IOException{
            System.out.println("");
            System.out.println("1.Registar no servidor");
            System.out.println("0.Sair");
            while(!s.hasNextInt()) s.nextLine();
            int choice = s.nextInt(); s.nextLine();
            switch(choice){
            case 1: 
            	start();
            	break;
            case 0:
                clientSocket.close();
                System.exit(0);
            }			
    }

    /* Menu apos registo */
    public void menu2() throws IOException{
            System.out.println("");
            System.out.println("2.Requisitar musica");
            System.out.println("1.Logout");
            System.out.println("0.Sair");
            while(!s.hasNextInt()) s.nextLine();
            int choice = s.nextInt(); s.nextLine();
            switch(choice){
            case 2:
            	requisitarMusica();
            	break;
            case 1: 
            	logout();
            	System.exit(0);
            case 0: 
                logout();
                System.exit(0);
            }
            menu2();
    }

    private void requisitarMusica() throws IOException {
        System.out.println("");
		System.out.print("Musica: ");
		
		// Enviar request
		String input = s.nextLine();
		byte[] musica = input.getBytes();
        PDU request = new PDU((byte)1,(byte)0,PDU.CONSULT_REQUEST,null,musica.length,musica);
        dOut.write(request.writeMessage());
        
        // Resposta do servidor
        PDU response = mensagens.nextMessage();
        if(response.getType() == PDU.CONSULT_RESPONSE){
    		// Musica encontrada        		
        	if(response.getDataType() == PDU.FOUND){
        		HashSet<Registo> clientes = response.getConsultResponseClients();
  
    			DatagramSocket socket = new DatagramSocket();
    			PDU probeRequestPDU = new PDU((byte)1,(byte)0,PDU.PROBE_REQUEST,null,0,null);
    			byte[] probeRequestMessage = probeRequestPDU.writeMessage(); 
    			byte[] responseBuffer = new byte[PDU.MAX_SIZE];
    			
        		long melhorTempo = Long.MAX_VALUE;
        		Registo melhorCliente = null;
        		
        		// Probing para cada cliente
        		System.out.println("\nProbing...");
        		Iterator<Registo> it = clientes.iterator();
        		while(it.hasNext()){
        			Registo registo = it.next();
        			DatagramPacket probeRequestPacket = new DatagramPacket(
        					probeRequestMessage, probeRequestMessage.length, registo.getIp(), registo.getPort());
        			DatagramPacket probeResponsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
        			socket.send(probeRequestPacket);
        			socket.receive(probeResponsePacket);		
        			PDU probeResponse = new PDU(probeResponsePacket.getData());
        			long timestamp = probeResponse.getProbeResponseTimestamp();
        			long owd = System.currentTimeMillis() - timestamp;
        			if(owd < melhorTempo){
        				melhorTempo = owd;
        				melhorCliente = registo;
        			}
        			
                    System.out.println(
                    		registo.getId() +
                    		 " - " + registo.getIp().toString() + ":" + registo.getPort() +
                    		 " -> " +"OWD = " + owd + " ms"
                    		);
        		}
        		
        		System.out.println(
        				"\nMelhor cliente: " + 
						melhorCliente.getId() +
						 " - " + melhorCliente.getIp().toString() + ":" + melhorCliente.getPort() +
						 " -> " + "OWD = " + melhorTempo + " ms");
        		
        		// TODO - transferencia ...
        		
        		socket.close();        		
        	}
    		// Musica nao encontrada      		
        	else if(response.getDataType() == PDU.NOT_FOUND){
        		System.out.println("Nao foi possivel obter a musica \""+input+"\".");
        	}
    		System.out.println("Prima ENTER para voltar ao menu");
    		s.nextLine();
        }		
	}

	public void logout() throws IOException{
            PDU logout = this.register(PDU.OUT);
            dOut.write(logout.writeMessage());
    }


    public static void main(String[] args){
            try {
                Client c = new Client();
                c.menu1();
            } catch (IOException e) {
                System.out.println("Ligacao perdida. Sessao terminada");
            }
    }
}
