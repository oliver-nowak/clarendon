package com.affectiva.clarendon.network;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
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

	
	
	ClientWorker(Socket _client) 
	{
		System.out.println("+++ creating ClientWorker thread...");
		
		client = _client;
		
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

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
