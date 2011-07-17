package com.affectiva.clarendon;

public class Clarendon_Server {


	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		
		ThreadManager threadManager = new ThreadManager();
		
		threadManager.connectSensor();
		threadManager.connectClient();
		
	}
}
