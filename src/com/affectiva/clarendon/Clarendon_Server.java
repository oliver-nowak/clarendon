package com.affectiva.clarendon;



/**
 * @author olivernowak
 * 
 * WebSocket implementation based on _HIXIE DRAFT #76_ 
 * http://tools.ietf.org/html/draft-hixie-thewebsocketprotocol-76#page-35
 *
 */
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
