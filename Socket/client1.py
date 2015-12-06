import socket
serverName = 'csa1.bu.edu'
serverPort = 58001

#Declare the client socket
clientSocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
clientSocket.connect((serverName, serverPort))

connect = True
#Send input message to server
while connect:
    sentence = raw_input('Input your message: ')
    clientSocket.send(sentence)

    #Receive message from server
    data = clientSocket.recv(1024)
    print 'Received From Servre: ', repr(data)
    
    #Send terminate message
    if sentence == 'exit':
        connect = False
    
clientSocket.close()
