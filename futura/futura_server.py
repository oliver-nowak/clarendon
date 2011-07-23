import optparse
import socket
import re
import hashlib
import struct
import binascii
import base64
import threading
import time

# Class Members
isLogging		= True
isConnected		= False
logFileUrl		= ""
logFile			= 0
web_socket 		= 0
commandSocket 	= 0
dataSocket 		= 0
auxDataSocket 	= 0
key_1 			= 0
key_2 			= 0
key_3 			= 0
data 			= ''
header 			= ''
sensor_data 	= ''
key1_chunk 		= ''
key2_chunk 		= ''
key3_chunk 		= ''
handshake ="\
HTTP/1.1 101 WebSocket Protocol Handshake\r\n"+\
"Upgrade: WebSocket\r\n"+\
"Connection: Upgrade\r\n"+\
"Sec-WebSocket-Origin: http://50.16.164.116\r\n"+\
"Sec-WebSocket-Location: ws://50.16.164.116:1030/\r\n\r\n"

# data thread structures
data_thread = 0
datasock	= 0
data_addr	= 0
condition 	= threading.Condition()

def getTime():
	return time.strftime("%m-%d-%Y_%H:%M:%S") + "> "

def log(msg=''):
	global isLogging
	global logFileUrl
	global logFile
	if isLogging == True:
		logFile = open(logFileUrl, 'a')	
		logFile.write( getTime() )	
		logFile.write( str(msg) + "\r\n")		
		logFile.close()
		print str(msg)
	else:
		print str(msg)
		

def calcKey( key ):
	log(' - calculating key')
	chunk = re.split('\d: ',key)[1]

	reg_ex_key = re.compile('\d')
	
	matches = reg_ex_key.findall( chunk )

	key_val = ''	

	for i in matches:
		key_val += i
	
	matches = []

	reg_ex_space = re.compile(' ')
	matches = reg_ex_space.findall( chunk )
	
	space_val = len(matches)
		
	calculatedKey = int(key_val) / int(space_val)

	return calculatedKey

def breakTimer():
	a = 1

def client_connection(condition, buffer, websocket, addr):	
	global data
	global handshake
	
	log(' - client connection thread created')

	header = ''
	header += websocket.recv(256)
	
	log(' - received handshake request')
	
	if header.find('\r\n\r\n') != -1:
		log(' - parsing handshake request')
		fields = header.split('\r\n')
		key1_chunk = fields[5]
		key2_chunk = fields[6]
		key3_chunk = fields[8]
		
		key_1 = calcKey(key1_chunk)
		key_2 = calcKey(key2_chunk)

		keyStr = ''		
		keyStr = struct.pack('>L', key_1) + struct.pack('>L', key_2) + key3_chunk
		
		hashFunc = hashlib.md5()
		hashFunc.update(keyStr)
		crypt_key = hashFunc.digest()

		isActive = False
	
		o_byte = '00'
		open_byte = o_byte.decode('hex')
	
		c_byte = 'FF'
		close_byte = c_byte.decode('hex')

		log(' - sending handshake response to client')
		websocket.send(handshake + crypt_key)	
	
		rdy_msg = websocket.recv(32)
		log(' - received client message with ' + str(rdy_msg))
	
		if rdy_msg.find('cafebabe') != -1:
			log(' - handshake ACCEPTED')	
			isActive = True
		else:
			log(' - handshake FAILED')
			
		#websocket.settimeout(0.015)
		
		send_fuse = 0
		recv_fuse = 0
		
		while isActive:	
			time.sleep(0.050)
			
			condition.acquire()
			msg = data[0:5]
			
			try:
				websocket.send(open_byte + msg + close_byte)	
			except socket.error:
				log('[*] websocket <send> error with ' + str(addr))
				send_fuse = send_fuse+1
				if send_fuse >= 13:
					log('[*] blowing fuse')
					isActive = False
			except socket.timeout:
				log('[*] websocket <send> timeout error with ' + str(addr))
				send_fuse = send_fuse+1
				if send_fuse >= 13:
					log('[*] blowing fuse')
					isActive = False
				
			condition.release()
			
			# check for client-side web socket close
			websocket.settimeout(0.015)
			close_msg = ''
			
			try:
				close_msg = websocket.recv(8)
			except socket.error:
				if recv_fuse <= 13:
					log('[*] websocket <recv> error with ' + str(addr))
					recv_fuse = recv_fuse+1
			except socket.timeout:
				if recv_fuse <= 13:
					log('[*] websocket <recv> timeout error with ' + str(addr))
					recv_fuse = recv_fuse+1
			
			websocket.settimeout(None)
			
			if close_msg.find('1313') != -1:
				log(' - client requested web socket close from ' + str(addr))
				isActive = False
			
	else:
		log(' - handshake was malformed')
	
	log( '[!] leaving client connection thread at ' + str(addr) )
		
def listenForClient():
	global data
	while True:
		log('[$] listening for new client connection')
		client_connect, addr = web_socket.accept()

		log( ' - client connection established : ' + str(addr) )
		
		client_thread = threading.Thread( None, client_connection, None, (condition, data, client_connect, addr) ) 
		client_thread.start()

def data_connection(condition, buffer=None, data_socket=None, addr=None):
	global data
	global dataSocket
	log(' - data connection thread created')
	
	while 1:
		log('[$] listening for new data socket connection')
		datasock, data_addr = dataSocket.accept()
		log(' - data socket connection established : ' + str(data_addr))
	
		isActive = True
	
		payload = ''
		while isActive:
			time.sleep(0.050)
			eda = ''
		
			try:
				payload = datasock.recv(156)

				if payload.find('1313') != -1:
					log(' - data socket requested connection close')
					isActive = False
				else:
					eda = re.split(',', payload)[6]
				
			except IndexError:
				log('[*] datasocket indexerror with ' + str(data_addr))
			else:
				a = 1
			finally:
				condition.acquire()
				data = eda
				condition.release()
			
		log('[!] data socket connection dropped')
	
	log('[!] leaving data socket connection thread')
	

def spawnDataThread():
	global data_thread
	global data
	log('[+] spawning data thread')
	data_thread = threading.Thread(None, data_connection, None, (condition, data, None, None))
	data_thread.start()
	

def initSocketListeners():
	global web_socket 
	global commandSocket
	global dataSocket
	global auxDataSocket
	log('[+] initializing socket listeners')
	
	#create client facing web socket listener
	log(' - creating web socket listener...')
	web_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	web_socket.bind(('', 1030))
	web_socket.listen(5)
	log('  [ OK ]')
	
	#create socket listeners for QLive or Mobile
	log(' - creating command socket...')
	commandSocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
	commandSocket.bind(('', 1040))
	commandSocket.listen(1)
	log('  [ OK ]')

	log(' - creating data socket...')
	dataSocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
	dataSocket.bind(('', 1031))
	dataSocket.listen(1)
	log('  [ OK ]')

	log(' - creating auxDataSocket listener...')
	auxDataSocket = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
	auxDataSocket.bind(('', 1032))
	auxDataSocket.listen(1)
	log('  [ OK ]')
	
	log('[+] initializing complete')
	

	
def start():
	log('[@] starting futura server')
	
	initSocketListeners()
	spawnDataThread()
	listenForClient()
	
	log('[!] leaving.')

def startLogging():
	global logFile
		
	try:
		logFile = open(logFileUrl, 'w')
		logFile.close()
	except IOError:
		print 'IOError'
	log('[@] opening log file')
		
def main():
	global isLogging
	global logFile
	global logFileUrl
	
	version			= "%prog 0.05292011.0231"
	usage 			= "usage: %prog [options] arg1 arg2"
	description		= "Futura Server for streaming EDA data to web-based clients"
	optParser		= optparse.OptionParser(version=version, usage=usage, description=description)
	optParser.add_option("-l","--log", help="write log to file, default=True", metavar="True")
	optParser.add_option("-d","--log_dest", help="set path for log, default=/var/log/futura", metavar="path/to/log")
	(options, args) = optParser.parse_args()	
	
	writeFlag = options.log
	if writeFlag == "True" or writeFlag == "TRUE" or writeFlag == "T" or writeFlag == "t":
		isLogging = True
	elif writeFlag == "False" or writeFlag == "FALSE" or writeFlag == "F" or writeFlag == "f":
		isLogging = False
	
	fileUrl = options.log_dest
	if fileUrl != None:
		logFileUrl = fileUrl + "/futura" + time.strftime("%m-%d-%Y_%H:%M:%S") + ".log"
		startLogging()
	else:
		logFileUrl = "/var/log/futura/futura-" + time.strftime("%m-%d-%Y_%H:%M:%S") + ".log"
	
	log('[@] loading as stand-alone server')
	start()
	
	
if __name__ == '__main__':
	main()
	