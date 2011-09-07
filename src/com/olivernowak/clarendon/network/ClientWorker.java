package com.olivernowak.clarendon.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientWorker implements Runnable {
	
	private Socket client;
	
	private BufferedReader in;
	private DataOutputStream outputStream;
	
	public volatile String streamData = "";
	
	private String fields[]	= new String[8];
	private String prevPacket = "";
	private String currPacket = "";
	private String data_bytes = "";
	
	private static final int KEY_3_LENGTH = 8;
	
	private String handshake = "HTTP/1.1 101 WebSocket Protocol Handshake\r\n" +
			  "Upgrade: WebSocket\r\n" +
			  "Connection: Upgrade\r\n" +
//			  "Sec-WebSocket-Origin: http://127.0.0.1\r\n" +
//			  "Sec-WebSocket-Location: ws://127.0.0.1:1030/\r\n\r\n";
			  "Sec-WebSocket-Origin: http://50.16.164.116\r\n" +
			  "Sec-WebSocket-Location: ws://50.16.164.116:1030/\r\n\r\n";

	private SensorWorker sw;
	
	
	public ClientWorker(Socket _client, SensorWorker _sw) 
	{
		System.out.println("+++ creating ClientWorker thread...");
		
		client = _client;
		
		sw = _sw;
		
		try {
			in = new BufferedReader( new InputStreamReader(client.getInputStream()) );
			outputStream = new DataOutputStream( client.getOutputStream() );
			
		} catch (IOException e) {
			System.out.println("!!! ERROR [1] " + e.getMessage() + " : " + e.getStackTrace() );
		}
	}
	
	@Override
	public void run() 
	{
		boolean isReady = validateHandshake();
		
		System.out.println("+++ preparing to write to socket...isValidHandshake? " + isReady);
		
		int openByte = 0x00;
		int closeByte = 0xFF;
		
		try {
			
			while (isReady) {
				
				if (sw != null) {
					
					if (in.ready()) {
						System.out.println("+++ Client Worker Input Stream is ready...");
						StringBuffer sb = new StringBuffer();
						
						while( in.ready() ) {
							sb.append( (char) in.read() );
						}
						
						if (sb.toString().indexOf("1313") > -1) {
							System.out.println("+++ Browser client requested CLOSE EVENT...");
							client.close();
							isReady = false;
							break;
						}
					}
					
					if (sw.data.indexOf("1313") > -1) {
						System.out.println("+++ Client Worker received CLOSE EVENT...");
						outputStream.write(closeByte);
						outputStream.write(openByte);
						outputStream.flush();
						client.close();
						isReady = false;
						
					} else {
						fields = sw.data.split(",");
						currPacket = fields[0];
					
					
						if (currPacket != prevPacket) {
							prevPacket = currPacket;

							outputStream.write(openByte);
							outputStream.write( fields[6].getBytes() );
							outputStream.write(closeByte);
							
							outputStream.flush();
							
						}
					}
					
				}
				
				Thread.sleep(60);
			}

		} 
		catch(IOException e) {
			System.out.println("!!! ERROR [2] " + e.getMessage() + " : " + e.getStackTrace() );
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("!!! ClientWorker thread dying...");
	}
	
	private boolean validateHandshake()
	{
		try {
			System.out.println("+++ reading connection request headers..." + in.ready());
			
			try {
				while( !in.ready() ) {
					System.out.println("!!! input not ready yet...");
					Thread.sleep(200);
				}
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			String fields[];
			
			int bytesAvail = client.getInputStream().available();
			
			byte[] handshakeBytes = new byte[bytesAvail];
			
			client.getInputStream().read(handshakeBytes);
			
			byte[] handshakeHeader = new byte[bytesAvail - KEY_3_LENGTH];
			byte[] k_3 = new byte[KEY_3_LENGTH];
			
			for (int i = 0; i < handshakeHeader.length; i++) {
				handshakeHeader[i] = handshakeBytes[i];
			}
			
			int ind = 0;
			for (int j = handshakeBytes.length - KEY_3_LENGTH; j < handshakeBytes.length; j++) {
				k_3[ind] = handshakeBytes[j];
				ind++;
			}
			
			String header = new String(handshakeHeader);
			
			System.out.println("+++ Received connection request: \r\n" + header);

			fields = header.split("\r\n");
			
			System.out.println("# fields " + fields.length);
			
			if (fields.length >= 7) {
				String key_chunk1 = (String) fields[5];
				
				String key_strip1 = key_chunk1.split(": ")[1];
				long key_1 = calculateKey(key_strip1);	

				String key_chunk2 = (String) fields[6];
				String key_strip2 = key_chunk2.split(": ")[1];
				long key_2 = calculateKey(key_strip2);
				
				Long k1 = key_1;
				Long k2 = key_2;
				
				int k_1 = k1.intValue();
				int k_2 = k2.intValue();
				
				ByteBuffer byteBuffer = ByteBuffer.allocate(16);
				byteBuffer.putInt(k_1);
				byteBuffer.putInt(k_2);
				byteBuffer.put(k_3);

				byte[] responseKey;
				
				MessageDigest md;
				try {
					
					md = MessageDigest.getInstance("MD5");
					responseKey = md.digest( byteBuffer.array() );
					
					byte responseHeader[] = handshake.getBytes("UTF-8");
					
					byte response[] = new byte[ responseHeader.length + responseKey.length];
					
					int bytePointer = 0;
					for (int i = 0; i < responseHeader.length; i++) {
						response[bytePointer] = responseHeader[i];
						bytePointer++;
					}
					
					for (int j = 0; j < responseKey.length; j++) {
						response[bytePointer] = responseKey[j];
						bytePointer++;
					}					
					
					outputStream.write(response);
					
					outputStream.flush();
					
					System.out.println("+++ flushing handshake response...");
					
					byte ack[] = new byte[9];
					int numRead = client.getInputStream().read(ack);
					
					System.out.println("NUM READ " + numRead);
						
					String rdy = new String(ack);
					
					System.out.println("+++ client rdy ? " + rdy);
					
					if (rdy.indexOf("cafebabe")>-1) {
						System.out.println("+++ Client Connection Ready.");
						
						return true;
					} else {
						return false;
					}

				} catch (NoSuchAlgorithmException e) {
					System.out.println("!!! ERROR : " + e.getStackTrace());
				}
			}
		} catch (IOException e) {
			System.out.println("!!! ERROR " + e.getMessage() + " : " + e.getStackTrace() );
		}
		
		return false;
	}
	
	
	private long calculateKey( String _hash ) 
	{
		Pattern pattern = Pattern.compile("\\d");
		
		Matcher matcher = pattern.matcher(_hash);
		
		String key = "";
		
		while( matcher.find() ) {
			key += matcher.group();
		}
		
		pattern = Pattern.compile(" ");
		matcher = pattern.matcher(_hash);
		
		long spcValCount = 0;
		
		while( matcher.find() ) {
			spcValCount++;
		}
		
		long hash = Long.parseLong(key) / spcValCount;
		
		return hash;
	}
}
