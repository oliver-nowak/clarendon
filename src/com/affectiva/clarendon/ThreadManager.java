package com.affectiva.clarendon;

import java.io.IOException;
import java.net.ServerSocket;

import com.affectiva.clarendon.network.ClientWorker;
import com.affectiva.clarendon.network.SensorWorker;

public class ThreadManager {

	private ServerSocket webSocket;
	private ServerSocket dataSocket;
	
	private ClientWorker cw;
	private SensorWorker sw;
	
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
	
	public synchronized void transfer()
	{
		cw.streamData = sw.data;
	}
	
	
	public synchronized void connectClient()
	{
		System.out.println("+++ Waiting for client socket...");

		try {
			cw = new ClientWorker(webSocket.accept(), sw);

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
		
		try {
			sw = new SensorWorker(dataSocket.accept());

			Thread _sensorThread = new Thread(sw);
			_sensorThread.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
