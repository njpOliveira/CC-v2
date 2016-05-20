import java.net.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeMap;
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
                        Thread receiverUDP = new Thread(new ClientMainReceiverUDP(socketReceiver.getLocalPort()));
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
		
		// Enviar consult request ao servidor
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
  
    			PDU probeRequestPDU = new PDU((byte)1,(byte)0,PDU.PROBE_REQUEST,null,0,null);
    			byte[] probeRequestMessage = probeRequestPDU.writeMessage(); 
    			byte[] responseBuffer = new byte[PDU.MAX_SIZE];
    			
        		long melhorTempo = Long.MAX_VALUE;
        		Registo melhorCliente = null;
        		
        		// Probing para cada cliente
        		System.out.println("\nProbing...");
        		DatagramSocket socket = null;
        		Iterator<Registo> it = clientes.iterator();
        		while(it.hasNext()){
        			try{
	        			socket = new DatagramSocket();
	        			socket.setSoTimeout(3000);
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
	                    socket.close();
        			}
        			catch(IOException e){
    					if(socket != null){
    						try{
    							socket.close();
    						}
    						catch(Exception e2){}
    					}
        			}
        		}
        		
        		if(melhorCliente != null){
	        		System.out.println(
	        				"\nMelhor cliente: " + 
							melhorCliente.getId() +
							 " - " + melhorCliente.getIp().toString() + ":" + melhorCliente.getPort() +
							 " -> " + "OWD = " + melhorTempo + " ms");
	        		
	        		request(musica, melhorCliente);
        		}
        		else{
            		System.out.println("Nao foi obtida resposta de nenhum cliente");	
        		}
        	}
    		// Musica nao encontrada      		
        	else if(response.getDataType() == PDU.NOT_FOUND){
        		System.out.println("Nao foi possivel obter a musica \""+input+"\".");
        	}
    		System.out.println("Prima ENTER para voltar ao menu");
    		s.nextLine();
        }		
	}

	private void request(byte[] musica, Registo cliente) throws IOException{
		PDU requestPDU = new PDU((byte)1,(byte)0,PDU.REQUEST,PDU.toBytes(Client.TAMANHO_JANELA),musica.length,musica);
		byte[] requestMessage = requestPDU.writeMessage(); 
		
		// Enviar request
		DatagramSocket socket = new DatagramSocket();
		socket.setSoTimeout(3000);
		DatagramPacket probeRequestPacket = new DatagramPacket(
				requestMessage, requestMessage.length, cliente.getIp(), cliente.getPort());
		socket.send(probeRequestPacket);
		
		// Esperar por request_response
		byte[] buffer = new byte[PDU.MAX_SIZE];
    	DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
    	
    	try{
    		socket.receive(receiveDatagram);
    	}
    	catch(SocketTimeoutException to){
    		// Timout -> Reenviar request
    		try{
    			socket.send(probeRequestPacket);
    			socket.receive(receiveDatagram);
    		}
    		catch(SocketTimeoutException to2){
    			System.out.println("Erro: Timeout no estabelecimento da ligacao");
    			socket.close();
    			return;
    		}
    	}

		PDU requestResponse = new PDU(receiveDatagram.getData());
		if(requestResponse.getType() != PDU.REQUEST_RESPONSE){
			System.out.println("Erro: Confirmacao do pedido nao foi recebida");
			socket.close();
			return; 
		}
		int numSegmentos = requestResponse.getRequestResponseNumberOfSegments();
		int transferPort = requestResponse.getRequestResponsePort();
				
		transferencia(socket,cliente.getIp(),transferPort,new String(musica),numSegmentos);
	}

	private void transferencia(DatagramSocket socket, InetAddress clientIP, int clientPort, String musica, int numSegmentos) throws IOException{
		int timeouts = 0;
		socket.setSoTimeout(3000);
		
		System.out.println("A iniciar transferencia...");
		// Enviar SYN
		PDU synPDU = new PDU((byte)1,(byte)0,PDU.SYN,null,0,null);
		byte[] synMessage = synPDU.writeMessage();
		DatagramPacket synPacket = new DatagramPacket(
				synMessage, synMessage.length, clientIP, clientPort);
		socket.send(synPacket);
		boolean terminado = false;
		byte[] buffer = new byte[PDU.MAX_SIZE];
    	DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
    	TreeMap<Integer,byte[]> segmentos = new TreeMap<>();
    	
    	// Receber pacotes
		while(!terminado){
			try {
				socket.receive(receiveDatagram);
				PDU pdu = new PDU(receiveDatagram.getData());
				switch(pdu.getType()){
				case PDU.FIN:
					terminado = true;
					if(pdu.getDataType() == PDU.KO){
						socket.close();
						System.out.println("Erro: Ligação terminada pelo emissor");
						return;
					}
					break;
				case PDU.DATA:
					int segIndex = pdu.getDataSegmentIndex();
					segmentos.put(segIndex, pdu.getDados());
					break;
				}
			} 
			catch(SocketTimeoutException to){
				timeouts++;
				if(segmentos.isEmpty()){
					if(timeouts >= 3){
						socket.close();
						System.out.println("Erro: Impossivel estabelecer ligacao");
						return;
					}
					// Re-enviar SYN
					socket.send(synPacket);
				}
				else terminado = true;
			}
			catch (IOException e) {
				terminado = true;
				e.printStackTrace();
			}
		}
		
		// Verificar pacotes perdidos
		System.out.println("A verificar segmentos...");
		for(int i = 0; i<numSegmentos; i++){
			if(segmentos.containsKey(i)){
				System.out.println("Segmento "+i+" - RECEBIDO");
			}
			else{
				System.out.println("Segmento "+i+" - FALHA");
			}
			if(!segmentos.containsKey(i)){
				//Pedir retransmissao
				byte[] seg_i = PDU.toBytes(i);
				PDU sRej = new PDU((byte)1,(byte)0,PDU.SREJ,null,seg_i.length,seg_i);
				byte[] sRejMessage = sRej.writeMessage();
				DatagramPacket sRejPacket = new DatagramPacket(
						sRejMessage, sRejMessage.length, clientIP, clientPort);
				try{
					socket.send(sRejPacket);
					socket.receive(receiveDatagram);
				}
				catch(IOException e){
					//Erro -> Pedir novamente
					try {
						socket.send(sRejPacket);
						socket.receive(receiveDatagram);
					} catch (IOException e2) {
		    			System.out.println("Erro: segmento perdido ("+i+")");
		    			socket.close();
		    			return;
					}
				}
				PDU seg = new PDU(receiveDatagram.getData());
				segmentos.put(seg.getDataSegmentIndex(), seg.getDados());
				System.out.println("Segmento "+i+" - RECEBIDO <Retransmissao>");
			}
		}
		
		// Terminar ligacao
		byte[] data = new byte[1];
		data[0] = PDU.OK;
		PDU finPDU = new PDU((byte)1,(byte)0,PDU.FIN,null,data.length,data);
		byte[] finMessage = finPDU.writeMessage();
		DatagramPacket finPacket = new DatagramPacket(
				finMessage, finMessage.length, clientIP, clientPort);
		socket.send(finPacket);
		
		// Criar ficheiro
		try{
			String path = Client.pathMusicas+"+"+musica;
			FileOutputStream fos = new FileOutputStream(path);
			for(byte[] segmento: segmentos.values()){
				fos.write(segmento);
			}
			fos.close();
			System.out.println("Transferencia concluida ("+"+"+musica+")");

		}
		catch(IOException e){
			System.out.println("Erro ao guardar o ficheiro");
		}
		socket.close();		
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
