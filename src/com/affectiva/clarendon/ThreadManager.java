package com.affectiva.clarendon;

import java.io.IOException;
import java.net.ServerSocket;

import com.affectiva.clarendon.network.ClientWorker;
import com.affectiva.clarendon.network.SensorWorker;

public class ThreadManager {

	private ServerSocket webSocket;
	private ServerSocket dataSocket;
	
//	private ClientWorker cw;
	private SensorWorker sw;
	
	public Thread sensorThread;
	
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
	
	public synchronized boolean sensorStatus()
	{
		if (sensorThread == null) {
			return false;
		}
		else if (sensorThread != null) {
			return sensorThread.isAlive();
		}
		return false;
	}
	
	
	public synchronized void connectClient()
	{

		try {
			
			while (true) {
				System.out.println("+++ SENSOR THREAD IS ALIVE? " + sensorThread.isAlive());
				
				if (sensorThread != null && !sensorThread.isAlive()) {
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
		
		//			while ( !sensorThread.isAlive() ) {
		//			while (true) {
						sw = new SensorWorker(dataSocket);
			
						sensorThread = new Thread(sw);
						sensorThread.start();
		//				Thread _sensorThread = new Thread(sw);
		//				_sensorThread.start(); 
		//			}
		
	}

}
