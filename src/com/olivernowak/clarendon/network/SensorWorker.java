package com.olivernowak.clarendon.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SensorWorker implements Runnable {

	private Socket connection;
	private ServerSocket serverSocket;
	
	private BufferedReader in;
	
	public volatile String data = "";
	
	public SensorWorker(ServerSocket _serverSocket) 
	{
		System.out.println("+++ creating SensorWorker thread...");
		
		serverSocket = _serverSocket;
	}
	

	@Override
	public void run() 
	{
		while (true) {

			int c;
			byte buffer[] = new byte[80]; // 56 too small
			ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
			byteBuffer.clear();
			
			boolean isReady = true;
			
			data = "";
			
			try {
				System.out.println(">>> waiting for sensor connection...");
				
				connection = serverSocket.accept();
				System.out.println(">>> sensor connected...");
				
				in = new BufferedReader( new InputStreamReader(connection.getInputStream()) );			
				System.out.println(">>> grabbing sensor input stream...");
				
				while( isReady ) {
					
						c = connection.getInputStream().read();
						
						switch(c) {
						case 10:
							break;
							
						case 81: // TERMINATE STRING
							data = new String(byteBuffer.array());
							
							if (data.indexOf("1313")>-1) {
								System.out.println("!!! Data Socket requested CLOSE event...");
								data = "1313";
								
								Thread.sleep(30);
								
								connection.close();
								isReady = false;
							}
							
							byteBuffer.clear();
	
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
		}
	}

}
