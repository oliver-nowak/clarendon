package com.affectiva.clarendon;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.affectiva.clarendon.network.ClientWorker;

public class Clarendon_Server {

	private static ServerSocket webSocket;
	private static ServerSocket dataSocket;
	
	private static Socket sensorClient;
	private static Socket browserClient;
	
	private static BufferedReader sensorData;
	
	private static boolean isHandshaking = true;
	
	private static String sample;
	
	private static String connectionRequest = "";
	
	
	private static String handshake = "HTTP/1.1 101 WebSocket Protocol Handshake\r\n" +
									  "Upgrade: WebSocket\r\n" +
									  "Connection: Upgrade\r\n" +
									  "Sec-WebSocket-Origin: http://127.0.0.1\r\n" +
									  "Sec-WebSocket-Location: ws://127.0.0.1:1030/\r\n\r\n";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		
		try {
			System.out.println(">>> creating server socket...");
	
			webSocket = new ServerSocket(1030);
			
			ClientWorker cw;
			
			cw = new ClientWorker(webSocket.accept());
			
			Thread _thread = new Thread(cw);
			_thread.start();
			
		} catch (IOException e) {
			System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
			System.exit(-1);
		}

		
	}
	
	private static long calculateKey( String _hash ) 
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
	
	private void initTestClient()
	{
		try {
			System.out.println(">>> creating server socket...");
	
			browserClient = webSocket.accept();
			
			System.out.println(">>> browserClient connected...");
			
			BufferedReader in = new BufferedReader( new InputStreamReader(browserClient.getInputStream()));

			DataOutputStream outputStream = new DataOutputStream( browserClient.getOutputStream() );

			System.out.println(">>> reading connection request headers..." + in.ready());
			
			StringBuffer sb = new StringBuffer();
			
			String fields[] = new String[9];
		
			
			while ( in.ready() ) {			
				sb.append( (char) in.read());
			}
			
			System.out.println(">>> Received connection request:\r\n" + sb.toString());
			
			String header = sb.toString();
			
			fields = header.split("\r\n");
			
			System.out.println(">>> fields.length : " + fields.length);
			
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
					
					String digBytes = new String(thedigest);
					
					outputStream.write(response);
					outputStream.write(thedigest);
					outputStream.flush();
					
					System.out.println(">>> " + in.ready());
					
					byte ack[] = new byte[9];
					browserClient.getInputStream().read(ack);
						
					String rdy = new String(ack);
						
					System.out.println("@@@ " + rdy);
					
					boolean isReady = false;
					
					if (rdy.indexOf("cafebabe")>-1) {
						System.out.println("+++ Client Connection Ready.");
						
						isReady = true;
					}
					
					int openByte = 0x00;
					int closeByte = 0xFF;
					
					String msg = "hello world";
					
					outputStream.write(openByte);
					outputStream.write(msg.getBytes("UTF-8"));
					outputStream.write(closeByte);
					

				} catch (NoSuchAlgorithmException e) {
					System.out.println("!!! ERROR : " + e.getStackTrace());
				}
			
			}
			

		} catch (IOException e) {
			System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
			System.exit(-1);
		}
	}
	
	private void initSensorSocket()
	{
		/////// SENSOR SOCKET
			try {
				dataSocket = new ServerSocket(1031);
			
				sensorClient = dataSocket.accept();
				
				System.out.println(">>> sensorClient connected...");
				
				sensorData = new BufferedReader( new InputStreamReader(sensorClient.getInputStream()));
				
				int c;
				byte buffer[] = new byte[80]; // 56 too small
				ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
				String data;
				
				System.out.println(">>> preparing to write capture data...");
				while( true ) {
						c = sensorData.read();
						
						switch(c) {
						case 10:
							break;
							
						case 81: // Q TERMINATE STRING
							data = new String(byteBuffer.array());
							System.out.println( data );
							byteBuffer.clear();
							break;
							
							default:
								byteBuffer.put((byte) c);
								break;
						}
	
				}

			} catch (IOException e) {
				System.out.println(">>> ERROR creating clientSocket on 1030 " + e.getStackTrace() + e.getMessage());
				System.exit(-1);
			}
			//////// END SENSOR SOCKET
	}

}
