package com.example.doprava_bp;

import javax.crypto.AEADBadTagException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

public class Main {

    public static void main(String[] args) throws Exception {

        int KEY_LENGTH = 256;

        ObuParameters obuParameters = new ObuParameters();
        File file;
        if (KEY_LENGTH == 128){
            file = new File("obuparams128.txt");
        }
        else if (KEY_LENGTH == 256){
            file = new File("obuparams256.txt");
        }
        else{
            file = new File("obuparams.txt");}
        //if (!file.exists()) {
        //    System.out.println("The file obuparams.txt does not exist.");
        //    Client clientF = new Client();
        //    try (FileWriter writer = new FileWriter("obuparams.txt")) {
        //        writer.write(clientF.obuParameters.getDriverKey() + "\t" + clientF.obuParameters.getIdr() + "\t"
        //                + clientF.obuParameters.getKeyLengths());
        //    } catch (IOException e) {
        //        System.out.println("An error occurred while writing to the file: " + e.getMessage());
        //    }
        //}

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String[] data = reader.readLine().split("\t");
            obuParameters.setDriverKey(data[0]);
            obuParameters.setIdr(Integer.parseInt(data[1]));
            obuParameters.setKeyLengths(Integer.parseInt(data[2]));
        } catch (IOException e) {
            System.out.println("An error occurred while reading from the file: " + e.getMessage());
        }

        Cryptogram userCryptogram = new Cryptogram();
        Cryptogram pokus = new Cryptogram();
        Cryptogram receiverCryptogram = new Cryptogram();

        //nastavení receiverC

        Random rnd = new Random();
        int nr = rnd.nextInt();
        receiverCryptogram.setNonce(nr);
        receiverCryptogram.setIdr(obuParameters.getIdr());

        //výměna nonces

        ServerSocket serverSocket = new ServerSocket(10001);
        System.out.println("Server is up and running on ip " + serverSocket.getInetAddress().getLocalHost().getHostAddress());
        Socket socket = serverSocket.accept();

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

        userCryptogram = (Cryptogram) objectInputStream.readObject();
        Instant start = Instant.now();
        System.out.println("Prijmuto nonce");
        objectOutputStream.writeObject(receiverCryptogram);
        serverSocket.close();

        //přijmutí ciphertextu

        ServerSocket serverSocket2 = new ServerSocket(10003);
        System.out.println("Server is up and running on ip " + serverSocket.getInetAddress().getLocalHost().getHostAddress());
        Socket socket2 = serverSocket2.accept();

        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream objectInputStream2 = new ObjectInputStream(socket2.getInputStream());

        userCryptogram = (Cryptogram) objectInputStream2.readObject();
        System.out.println("Ciphertext prijmut");
        serverSocket2.close();
        //serverSocket.close();

        //Autentizace

        CryptoCore cryptoCore = new CryptoCore(obuParameters,userCryptogram,receiverCryptogram);
        try {
            receiverCryptogram.setAuthenticated(cryptoCore.dec(userCryptogram.cryptograms.get(0)));
        }
        catch (AEADBadTagException ex) {
            receiverCryptogram.setAuthenticated(false);
        }

        //poslání výsledku aut.

        ServerSocket serverSocket3 = new ServerSocket(10002);
        System.out.println("Server is up and running on ip " + serverSocket.getInetAddress().getLocalHost().getHostAddress());
        Socket socket3 = serverSocket3.accept();

        objectOutputStream = new ObjectOutputStream(socket3.getOutputStream());

        objectOutputStream.writeObject(receiverCryptogram);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start,end);
        System.out.println("Time: " + timeElapsed.toMillis() + " ms");
        serverSocket3.close();

    }
}

/*  POSLEDNÍ sekvence kdyžtak
serverSocket = new ServerSocket(10002);
        System.out.println("Server is up and running on ip " + serverSocket.getInetAddress().getLocalHost().getHostAddress());
        socket = serverSocket.accept();

        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

        objectOutputStream.writeObject(receiverCryptogram);
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start,end);
        System.out.println("Time: " + timeElapsed.toMillis() + " ms");
        serverSocket.close();
 */
