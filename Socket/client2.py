__author__ = 'Yigang'

import socket
import time
import sys

#Declare hostname and port
serverName = 'pcvm2-2.instageni.idre.ucla.edu'
#serverName = 'pcvm2-1.genirack.nyu.edu'
#serverName = 'pcvm3-1.instageni.illinois.edu'
#serverName = 'pcvm2-19.utah.geniracks.net'
#serverName = 'localhost'
serverPort = 2000

clientSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
clientSocket.connect((serverName, serverPort))

global countProbes
global messageSize  

###::Connection Setup Phase::###
def CSP_Client():
    global countProbes
    global messageSize
    
    setup = True
    #Send input message to server
    while setup:
        # s rtt 10 100 0\n, the message size is 1, 100, 200, 400, 800, 1000 bytes -- for RTT
        # s tput 10 2K 0\n, the message size is 1K, 2K, 4K, 8K, 16K, 32K bytes -- for Throughput
        print 'Input set up message: '
        sentence = sys.stdin.read()
        clientSocket.send(sentence)
        
        split_sentence = sentence.split()
        countProbes = split_sentence[2]
        messageSize = split_sentence[3]

        CSPdata = []
        CSPdata.append(clientSocket.recv(1024))
        while CSPdata[-1][-1] != '\n':
            CSPdata.append(clientSocket.recv(1024))
        CSPdata = ''.join(CSPdata)

        CSPdata = CSPdata.replace('\n', '')
        print 'Received From Server: ', repr(CSPdata)

        if CSPdata == "200 OK:Ready":
            setup = False

    
###::Measurement Phase::###
def MP_Client():
    global countProbes
    global messageSize
    
    #Generate buffer with correct message size
    buffer = []
    for b in range(0, int(messageSize)):
        buffer.append('f')
    buffer = ''.join(buffer)
    
    #Send and receive socket messages
    connect = True
    times = []
    while connect:
        for i in range(0, int(countProbes)):
            measure = 'm'+' '+str(i+1)+' '+buffer+'\n'
            clientSocket.send(measure)
            start = time.time()

            #Receive message and save in MPdata
            MPdata = []
            MPdata.append(clientSocket.recv(1024))
            finish = time.time()
            while MPdata[-1][-1] != '\n':
                MPdata.append(clientSocket.recv(1024))
            MPdata = ''.join(MPdata)
            MPdata = MPdata.replace('\n', '') 
            entry = finish - start
            times.append(entry)
            
            #Uncomment lines below to get information
            #print 'Receive From Server: ', repr(MPdata)
            #print 'Message ', i+1, ' takes: ', repr(entry)
        
            split_MPdata = MPdata.split()
            count = split_MPdata[1]
            
            if (MPdata == "404 ERROR: Invalid Measurement Message") or (count == countProbes):
                connect = False
    
    #Count mean time
    sum = 0
    for x in range(0, len(times)):
        sum += times[x]
    mean = sum / len(times)
    print 'Mean is: ', mean
    
    
###::Connection Termination Phase::###
def CTP_Client():
    connect = True
    while connect:
        print 'Input termination message: '
        #Input termination message
        terminate = sys.stdin.read()
        clientSocket.send(terminate)
        
        #Receive message from server
        CTPdata = []
        CTPdata.append(clientSocket.recv(1024))
        while CTPdata[-1][-1] != '\n':
            CTPdata.append(clientSocket.recv(1024))
        CTPdata = ''.join(CTPdata)
        CTPdata = CTPdata.replace('\n', '') 
        
        print 'Receive From Server: ', repr(CTPdata)
        
        if CTPdata == '200 OK: Closing Connection':
            connect = False
        
        

    
CSP_Client()
MP_Client()
CTP_Client()
#Close socket
clientSocket.close()
