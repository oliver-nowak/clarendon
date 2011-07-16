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
	
	private String handshake = "HTTP/1.1 101 WebSocket Protocol Handshake\r\n" +
			  "Upgrade: WebSocket\r\n" +
			  "Connection: Upgrade\r\n" +
			  "Sec-WebSocket-Origin: http://127.0.0.1\r\n" +
			  "Sec-WebSocket-Location: ws://127.0.0.1:1030/\r\n\r\n";

	
	
	public ClientWorker(Socket _client) 
	{
		System.out.println("+++ creating ClientWorker thread...");
		
		client = _client;
		
		try {
			in = new BufferedReader( new InputStreamReader(client.getInputStream()) );
			outputStream = new DataOutputStream( client.getOutputStream() );
			
		} catch (IOException e) {
			System.out.println("!!! ERROR " + e.getMessage() + " : " + e.getStackTrace() );
		}
		
		
	}

	@Override
	public void run() 
	{
		boolean isReady = validateHandshake();
		
		System.out.println("+++ preparing to write to socket...");
		
		int openByte = 0x00;
		int closeByte = 0xFF;
		
		String msg = "hello world";
		try {
			outputStream.write(openByte);
			outputStream.write(msg.getBytes("UTF-8"));
			outputStream.write(closeByte);
		} catch(IOException e) {
			System.out.println("!!! ERROR " + e.getMessage() + " : " + e.getStackTrace() );
		}

	}
	
	private boolean validateHandshake()
	{
		try {
			System.out.println("+++ reading connection request headers..." + in.ready());
			
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
				
				System.out.println(">>> bytes len : " + key_chunk3.getBytes().length);
				
				Long k1 = key_1;
				Long k2 = key_2;
				
				int k_1 = k1.intValue();
				int k_2 = k2.intValue();
				
				ByteBuffer bb = ByteBuffer.allocate(16);
				bb.putInt(k_1);
				bb.putInt(k_2);
				bb.put(key_3);
				
				System.out.println("bb size : " + bb.array().length);
				

				byte[] thedigest;
				
				MessageDigest md;
				try {
					
					md = MessageDigest.getInstance("MD5");
					thedigest = md.digest( bb.array() );
					
					System.out.println("sizeof thedigest : " + thedigest.length);
					
					byte response[] = handshake.getBytes("UTF-8");
					
					outputStream.write(response);
					outputStream.write(thedigest);
					outputStream.flush();
					
					System.out.println(">>> " + in.ready());
					
					byte ack[] = new byte[9];
					client.getInputStream().read(ack);
						
					String rdy = new String(ack);
						
					System.out.println("@@@ " + rdy);
					
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
			System.out.println("??? " + matcher.group());
			System.out.println("+++ " + key);
		}
		
		System.out.println(">>> matchCount : " + key);
		
		pattern = Pattern.compile(" ");
		matcher = pattern.matcher(_hash);
		
		long spcValCount = 0;
		
		while( matcher.find() ) {
			spcValCount++;
		}
		
		System.out.println(">>> spaceCount : " + spcValCount);
		
		long hash = Long.parseLong(key) / spcValCount;
		
		System.out.println(">>> hash Value : " + hash);
		
		return hash;
	}

}
