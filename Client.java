import java.net.*;
import java.util.Scanner;
import java.util.Stack;
import java.io.*;

public class Client {

    private Socket clientSocket;
    private String id;
    private OutputStream dOut;
    private InputStream dIn;
    private Stack<PDU> mensagens;

    public static final String ip = "localhost";

    protected static final int portaServidor = 6789;

    private PDU register(byte tipo){
            int porto = clientSocket.getLocalPort();

            InetAddress ip = clientSocket.getLocalAddress();
            byte[] bytesIP = ip.getAddress();

            byte[] bytesID = this.id.getBytes();

            byte[] bytesPORT = toBytes(porto);

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
            PDU message = new PDU((byte)1,(byte)0,(byte)1,null,tamanho,dados);
            return message;

    }

    public void start(){
            Scanner s = new Scanner(System.in);

            mensagens = new Stack<PDU>();

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

                    while(mensagens.isEmpty());

                    PDU mensagem = mensagens.pop();

                    if(mensagem.getType()==PDU.ACK) {
                            System.out.println("Registo efectuado com sucesso");
                            menu2();
                    }
                    if(mensagem.getType()==PDU.NACK){
                            System.out.println("Já existe um utilizador com o seu id");
                            menu1();
                    }
                    menu2();
            }
            catch(IOException e){
                    e.printStackTrace();
            }
            finally{
                try{
                    clientSocket.close();
                }
                catch(Exception e){}
            }	
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

    public void menu1() throws IOException{
            @SuppressWarnings("resource")
            Scanner s = new Scanner(System.in);
            System.out.println("1.Registar no servidor");
            System.out.println("0.Sair");
            int choice = s.nextInt();
            switch(choice){
                            case 1: start();
                            case 0: {
                                    clientSocket.close();
                                    System.exit(0);
                            }
            }			
    }

    public void menu2() throws IOException{
            @SuppressWarnings("resource")
            Scanner s = new Scanner(System.in);
            System.out.println("1.Logout");
            System.out.println("0.Sair");
            int choice = s.nextInt();
            switch(choice){
                            case 1: logout();
                            case 0: {
                                    logout();
                                    System.exit(0);
                            }
            }
    }

    public void logout() throws IOException{
            PDU logout = this.register(PDU.OUT);
            dOut.write(logout.writeMessage());
            //clientSocket.close();
    }


    public static void main(String[] args){
            try {
                    Client c = new Client();
                    c.menu1();
            } catch (Exception e) {
                    e.printStackTrace();
            }
    }
}
