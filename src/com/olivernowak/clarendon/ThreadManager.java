package com.olivernowak.clarendon;

import java.io.IOException;
import java.net.ServerSocket;

import com.olivernowak.clarendon.network.ClientWorker;
import com.olivernowak.clarendon.network.SensorWorker;

public class ThreadManager {

	private ServerSocket webSocket;
	private ServerSocket dataSocket;
	
	private SensorWorker sw;
	
	public Thread connectionThread;
	
	public ThreadManager() {
		try {
			System.out.println(">>> creating server socket...");
	
			webSocket = new ServerSocket(1030);
			dataSocket = new ServerSocket(1031);
		} 
		catch (IOException e) {
			System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
			System.exit(-1);
		} 
	}
	
	public synchronized void connectClient()
	{

		try {
			
			while (true) {
				System.out.println("+++ SENSOR THREAD IS ALIVE? " + connectionThread.isAlive());
				
				if (connectionThread != null && !connectionThread.isAlive()) {
					connectSensor();
				}
				
				System.out.println("+++ Waiting for client connection...");
				
				ClientWorker cw = new ClientWorker(webSocket.accept(), sw);
				
				System.out.println("+++ client connected. creating thread...");
	
				Thread _thread = new Thread(cw);
				_thread.start();
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public synchronized void connectSensor()
	{
		System.out.println(">>> creating sensor thread...");
		
		sw = new SensorWorker(dataSocket);

		connectionThread = new Thread(sw);
		connectionThread.start();
	}
}
