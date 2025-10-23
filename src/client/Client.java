package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try{
            Socket socket = new Socket("localhost", 9999);


            // on communique avec le server
            //le flux entrant donc le serveur à envoyer
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //le flux sortant
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            //lecture des messages écrit sur la console par le client
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            String msg;
            while(true){
                System.out.print("Taper le message que vous souhaiter envoyer");
                msg = reader.readLine();
                if(msg.trim().equalsIgnoreCase("quit")){

                    out.println(msg);
                    System.out.println("La réponse du serveur : " + in.readLine());
                    System.out.println("Deconnexion...");   
                    break;
                }
                System.out.println("Le message envoyer :" + msg);
                out.println(msg);

                System.out.println("La réponse du serveur : " + in.readLine());
            }


            /*
            System.out.println("Voici le message que j'ai envoyer :"+msg);

            //on lit ce qu'il y a dans le flux (donc ce que le serveur à envoyer au client)
            String message = in.readLine();

            System.out.println("Voici ce que le serveur m'a répondu : " + message);
            */
            //Une fois qu'on terminer la communaction avec le serveur et on eu ce qu'on voulait on ferme la socket de communication

            socket.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
