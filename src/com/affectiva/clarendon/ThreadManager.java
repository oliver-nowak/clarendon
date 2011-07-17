package com.affectiva.clarendon;

import java.io.IOException;
import java.net.ServerSocket;

import com.affectiva.clarendon.network.ClientWorker;
import com.affectiva.clarendon.network.SensorWorker;

public class ThreadManager {

	private static boolean isReady = false;
	
	private ServerSocket webSocket;
	private ServerSocket dataSocket;
	
	private ClientWorker cw;
	SensorWorker sw;
	
	private volatile String data = "";
	
	
	public ThreadManager() {
		try {
			System.out.println(">>> creating server socket...");
	
			webSocket = new ServerSocket(1030);
			dataSocket = new ServerSocket(1031);

			isReady = true;
		} 
		catch (IOException e) {
			System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
			System.exit(-1);
		} 
	}
	
	public synchronized void transfer()
	{
		cw.streamData = sw.data;
	}
	
	
	public synchronized void connectClient()
	{
		System.out.println("+++ Waiting for client socket...");
		
//		ClientWorker cw;
		
		try {
			cw = new ClientWorker(webSocket.accept(), sw);
			
//			cw.streamData = data;
			
			Thread _thread = new Thread(cw);
			_thread.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public synchronized void connectSensor()
	{
		System.out.println("+++ Waiting for data socket...");
		
//		SensorWorker sw;
		try {
			sw = new SensorWorker(dataSocket.accept());
			
//			data = sw.testData;
			
			Thread _sensorThread = new Thread(sw);
			_sensorThread.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
