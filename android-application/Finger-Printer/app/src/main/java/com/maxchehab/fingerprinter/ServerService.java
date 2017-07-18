package com.maxchehab.fingerprinter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by maxchehab on 7/17/17.
 */

public class ServerService extends Service {

    public int counter = 0;

    private Timer timer;
    private TimerTask timerTask;

    static SharedPreferences sharedPreferences;



    public ServerService(Context applicationContext){
        super();
        new ServerInitializer().start();

        sharedPreferences = applicationContext.getSharedPreferences("data", MODE_PRIVATE);

        Log.i("ServerService","initilized");
    }

    public ServerService(){}

    private static class ServerInitializer extends Thread{
        public void run(){
            int clientNumber = 0;
            try {
                ServerSocket serverSocket = new ServerSocket(61597);
                try{
                    while(true){
                        new ServerListener(serverSocket.accept(),clientNumber++).start();
                    }
                }finally {
                    serverSocket.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }

        }
    }

    private static class ServerListener extends Thread{
        private Socket socket;
        private int clientNumber;

        public ServerListener(Socket socket, int clientNumber){
            this.socket = socket;
            this.clientNumber = clientNumber;

            Log.i("ServerListener","New connection with client #" + clientNumber + " @ " + socket);
        }

        public void run(){
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                writer.println("Hello, you have connected!");

                while(true){
                    String input = reader.readLine();
                    if(input == null){
                        break;
                    }

                    Log.i("ServerListener","client #" + clientNumber + " responded with the message {" + input + "}");

                    try {
                        JsonParser jp = new JsonParser();
                        JsonElement root = jp.parse(input);
                        JsonObject rootobj = root.getAsJsonObject();

                        String applicationID = null;
                        String uniqueKey = null;

                        switch (rootobj.get("command").getAsString()) {
                            case "knock-knock":
                                writer.println("{\"sucess\":true,\"message\":\"who is there?\"}");
                                break;
                            case "pair":
                                applicationID = rootobj.get("applicationID").getAsString();
                                uniqueKey = rootobj.get("uniqueKey").getAsString();

                                if(sharedPreferences.contains(applicationID)){
                                    writer.println("{\"sucess\":false,\"message\":\"already paired\"}");
                                    break;
                                }

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(applicationID, uniqueKey);
                                editor.commit();

                                writer.println("{\"sucess\":true,\"message\":\"paired\"}");
                                break;
                            case "authenticate":
                                applicationID = rootobj.get("applicationID").getAsString();
                                if(!sharedPreferences.contains(applicationID)){
                                    writer.println("{\"sucess\":false,\"message\":\"i do not know that application key\"}");
                                    break;
                                }

                                authenticate();
                                writer.println("{\"sucess\":true,\"message\":\"authenticating\"}");
                                break;
                            default:
                                writer.println("{\"sucess\":false,\"message\":\"i do not understand that command\"}");
                        }
                    }catch(IllegalStateException | NullPointerException | JsonSyntaxException e){
                        writer.println("{\"sucess\":false,\"message\":\"i do not understand that command\"}");
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                try{
                    socket.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void authenticate(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i("ServerService", "onDestroy()");

        Intent broadcastIntent = new Intent(".RestartServer");
        sendBroadcast(broadcastIntent);
        stopTimerTask();
    }

    public void startTimer(){
        timer = new Timer();
        initilizeTimerTask();
        timer.schedule(timerTask,1000,1000);
    }

    public void initilizeTimerTask(){
        timerTask = new TimerTask(){
            public void run(){
                Log.i("TimerTask", "timer: " + (counter++));
            }
        };
    }

    public void stopTimerTask(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

}
