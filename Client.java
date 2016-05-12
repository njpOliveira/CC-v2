import java.net.*;
import java.util.Scanner;
import java.io.*;

public class Client {

    private Socket clientSocket;
    private String id;
    private OutputStream dOut;
    @SuppressWarnings("unused")
	private InputStream dIn;
    private PDUBuffer mensagens;

    private Scanner s = new Scanner(System.in);
    
    public static final String ip = "localhost";

    protected static final int portaServidor = 6789;

    private PDU register(byte tipo){
            int porto = clientSocket.getLocalPort();

            InetAddress ip = clientSocket.getLocalAddress();
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
        	if(response.getDataType() == PDU.FOUND){
        		// Musica encontrada        		
        		// TODO ...
        	}
        	else if(response.getDataType() == PDU.NOT_FOUND){
        		// Musica nao encontrada      		
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
