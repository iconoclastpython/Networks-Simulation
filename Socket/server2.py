__author__ = 'Yigang'

import socket
serverName = ''
serverPort = 2000

#Declare server socket
serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serverSocket.bind((serverName, serverPort))
serverSocket.listen(1)
print "Server Started!"


#Accept the data
conn, addr = serverSocket.accept()
print 'Connected by', addr

global probes

###::Connection Setup Phase::###
def CSP_Server():
    global probes
    setup = True
    while setup:
        CSPdata = []
        CSPdata.append(conn.recv(1024))
        while CSPdata[-1][-1] != '\n':
            CSPdata.append(conn.recv(1024))
        CSPdata = ''.join(CSPdata)

        CSPdata = CSPdata.replace('\n', '')
        #Split and check the setup message
        split_CSPdata = CSPdata.split()
        CSPphase = split_CSPdata[0]
        type = split_CSPdata[1]
        probes = int(split_CSPdata[2])
        size = int(split_CSPdata[3])
        delay = int(split_CSPdata[4])
        
        if (CSPphase == 's') and (type == 'rtt' or type == 'tput') and (probes > 0) and (size > 0) and (delay >= 0):
            response = "200 OK:Ready\n"
            setup = False
            conn.send(response)
            print response
        else:
            response = "404 ERROR: Invalid Connection Setup Message\n"
            conn.send(response)
            print response
    print 'Socket connection setup!'
        
    
###::Measurement Phase::###
def MP_Server():
    global probes
    connect = True
    prePSN = 0
    while connect:
        MPdata = []
        MPdata.append(conn.recv(1024))
        while MPdata[-1][-1] != '\n':
            MPdata.append(conn.recv(1024))
        MPdata = ''.join(MPdata)
        MPdata = MPdata.replace('\n', '') 
            
        split_MPdata = MPdata.split()
        MPphase = split_MPdata[0]
        PSN = int(split_MPdata[1])  #PSN: Probe Sequence Number
        payload = split_MPdata[2]
        MPdata = MPdata.split()
        MPdata.append('\n')
        MPdata = ' '.join(MPdata)

        if (MPphase == 'm') and (PSN - prePSN == 1):
            response = MPdata
            prePSN = PSN
            if(PSN == probes):
                connect = False
            
        else:
            response = "404 ERROR: Invalid Measurement Message\n"
            connect = False
            
        conn.send(response)
    print 'Measurement finished!'

###::Connection Termination Phase::###
def CTP_Server():
    connect = True
    while connect:
        CTPdata = []
        CTPdata.append(conn.recv(1024))
        while CTPdata[-1][-1] != '\n':
            CTPdata.append(conn.recv(1024))
        CTPdata = ''.join(CTPdata)
        CTPdata = CTPdata.replace('\n', '') 
        
        if  CTPdata == 't':
            response = "200 OK: Closing Connection\n"
        else:
            response = "404 ERROR: Invalid Connection Termination Message\n"
            
        connect = False
        conn.send(response)
    print 'Connection terminated!'

CSP_Server()
MP_Server()
CTP_Server()
conn.close()
