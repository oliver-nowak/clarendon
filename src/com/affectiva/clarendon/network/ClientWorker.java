package com.affectiva.clarendon.network;

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
	private String eda = "";
	
	private String handshake = "HTTP/1.1 101 WebSocket Protocol Handshake\r\n" +
			  "Upgrade: WebSocket\r\n" +
			  "Connection: Upgrade\r\n" +
//			  "Sec-WebSocket-Origin: http://127.0.0.1\r\n" +
//			  "Sec-WebSocket-Location: ws://127.0.0.1:1030/\r\n\r\n";
			  "Sec-WebSocket-Origin: http://labs.qa.affectiva.com\r\n" +
			  "Sec-WebSocket-Location: ws://labs.qa.affectiva.com:1030/\r\n\r\n";

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
			
			while (true) {
				
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
						client.close();
						isReady = false;
						
					} else {
						fields = sw.data.split(",");
						currPacket = fields[0];
					
					
						if (currPacket != prevPacket) {
							eda = fields[6];
							prevPacket = currPacket;

							outputStream.write(openByte);
							outputStream.write(eda.getBytes());
							outputStream.write(closeByte);
							
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
			
			StringBuffer sb = new StringBuffer();
			String fields[] = new String[9];
			
			while( in.ready() ) {
				sb.append( (char) in.read() );
			}
			
			System.out.println("+++ Received connection request: \r\n" + sb.toString());
			
			String header = sb.toString();
			
			fields = header.split("\r\n");
			
			if (fields.length >= 8) {
				String key_chunk1 = (String) fields[5];
				String key_strip1 = key_chunk1.split(": ")[1];
				long key_1 = calculateKey(key_strip1);	

				String key_chunk2 = (String) fields[6];
				String key_strip2 = key_chunk2.split(": ")[1];
				long key_2 = calculateKey(key_strip2);
				
				String key_chunk3 = (String) fields[8];			
				byte key_3[] = new byte[8];
				key_3 = key_chunk3.getBytes();
				
				for (int q = 0; q < key_3.length; q++) {
					System.out.println( Integer.toHexString( key_3[q]) );
				}
				
				Long k1 = key_1;
				Long k2 = key_2;
				
				int k_1 = k1.intValue();
				int k_2 = k2.intValue();
				
				ByteBuffer bb = ByteBuffer.allocate(16);
				bb.putInt(k_1);
				bb.putInt(k_2);
				bb.put(key_3);

				byte[] thedigest;
				
				MessageDigest md;
				try {
					
					md = MessageDigest.getInstance("MD5");
					thedigest = md.digest( bb.array() );
					
					byte response[] = handshake.getBytes("UTF-8");
					
					byte test[] = new byte[ response.length + thedigest.length];
					
					int count = 0;
					for (int i = 0; i < response.length; i++) {
						test[count] = response[i];
						count++;
					}
					
					for (int j = 0; j < thedigest.length; j++) {
						test[count] = thedigest[j];
						count++;
						
						System.out.printf("0x%02X", thedigest[j]);
					}
					
					
					outputStream.write(test);
					
//					outputStream.write(response);
//					outputStream.writeUTF(handshake);
//					outputStream.write(0x0D);
//					outputStream.write(0x0A);
					
//					System.out.println(new String(response));
					
//					outputStream.write(thedigest);
					
//					System.out.println(new String(thedigest));
//					System.out.println("leng : " + thedigest.length);
					
					outputStream.flush();
					
					System.out.println("+++ flushing handshake response...");
					
//					try {
//						Thread.sleep(50);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					
					byte ack[] = new byte[9];
					client.getInputStream().read(ack);
						
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
