__author__ = 'Yigang'

import socket
serverName = ''
serverPort = 11111

#Declare server socket
serverSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serverSocket.bind((serverName, serverPort))
serverSocket.listen(1)
print "Server Started!"

#Accept the data
conn, addr = serverSocket.accept()
print 'Connected by', addr

while True:
    data = conn.recv(1024)
    if not data: break
    print "Data Received: ", data
    conn.send(data)

conn.close()
