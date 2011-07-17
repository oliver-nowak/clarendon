package com.affectiva.clarendon.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SensorWorker implements Runnable {

	private Socket sensor;
	
	private BufferedReader in;
	
	public volatile String data = "";
	
//	public String testData = "helloworld";
	
	public SensorWorker(Socket _sensor) 
	{
		System.out.println("+++ creating SensorWorker thread...");
		
		sensor = _sensor;
		
		try {
			in = new BufferedReader( new InputStreamReader(sensor.getInputStream()) );
		}
		catch(IOException e) {
			System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
			System.exit(-1);
		}
		
	}
	
//	public synchronized String getData()
//	{
//		return data;
//	}

	@Override
	public void run() 
	{
		int c;
		byte buffer[] = new byte[80]; // 56 too small
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
//		String data;
		
		try {
			System.out.println(">>> preparing to write capture data...");
			
			while( true ) {
				
//					synchronized(this) {
//						System.out.println("mau");
//						testData = "not here";
//					}
					
					c = in.read();
					
					switch(c) {
					case 10:
						break;
						
					case 81: // Q TERMINATE STRING
//						synchronized(this) {
							data = new String(byteBuffer.array());
//							testData = "not here";
//						}
//						System.out.println( data );
						byteBuffer.clear();
						
						try {
							Thread.sleep(125);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
						
						default:
							byteBuffer.put((byte) c);
							break;
					}
			}
		}
		catch (IOException e) {
			System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
		}
	}

}
