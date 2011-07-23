This folder contains python code for the <futura_server>.

Summary
~~~
The futura_server is a light-weight, multi-threaded server application that
receives a socket stream containing EDA data from a device client and relays that data to all
web-based clients connected via an HTML5 websocket. 


Technics
~~~

Repository
	git@affectiva.beanstalkapp.com:/bert_streamer.git
	branch: web_client
	/repo_root
		/trunk
			/futura

Thread Handling
	Browser client threads are joined when clients leave the page.

	The device thread is recycled when a device has disconnected.


Concurrency
	There is a hard-coded limit of 5 (five) browser-based client connections
	and 1 (one) device client.


WebSocket Support
	This server only supports the WebSocket protocol 76.
	
	The following browsers have been tested:
	+ Chrome
	+ Safari 5.05
	+ Mobile Safari (iOS/iPad2)


Compatibility
	Python: 2.5.5
	HTML5 Websocket Protocol 76 (supports Chrome and Safari)


Active Ports
	[+] Device Client
		:1031
	
	[+] Websocket Client
		:1030

InActive Ports (reserved)
	[-] Command
		:1040

	[-] Aux Device
		:1032


Logging (default)
	/var/log/futura/futura-[timestamp].log


Help
	$> python futura_server.py -h



