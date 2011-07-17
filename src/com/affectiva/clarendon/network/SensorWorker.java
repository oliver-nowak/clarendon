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
	

	@Override
	public void run() 
	{
		int c;
		byte buffer[] = new byte[80]; // 56 too small
		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		
		boolean isReady = true;
		
		try {
			System.out.println(">>> preparing to write capture data...");
			
			while( isReady ) {
					
					c = in.read();
					
					switch(c) {
					case 10:
						break;
						
					case 81: // Q TERMINATE STRING
						data = new String(byteBuffer.array());
						
						if (data.indexOf("1313")>-1) {
							System.out.println("!!! Data Socket requested CLOSE event...");
							data = "1313";
							
							Thread.sleep(30);
							
							sensor.close();
							isReady = false;
						}
						
						byteBuffer.clear();

						Thread.sleep(60);

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
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("!!! SensorWorker thread dying...");
	}

}
