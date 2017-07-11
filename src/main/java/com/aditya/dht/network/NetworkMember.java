package com.aditya.dht.network;

import com.aditya.dht.ApplicationMessage;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by neo on 11-07-2017.
 */
public class NetworkMember {

    ExecutorService executorServerSocket = Executors.newSingleThreadExecutor();
    ExecutorService executorSockets = Executors.newSingleThreadExecutor();

    private ServerSocket serverSocket;
    private ConcurrentHashMap<String, ObjectSocket> peers = new ConcurrentHashMap<String, ObjectSocket>();
    private HashSet<String> connectionPorts = new HashSet<String>();

    private ArrayList<String> pendingConnections = new ArrayList<String>();

    private NetworkObserver observer;

    /**
     * NetworkMember starts a server socket at a free port.
     * Also creates another thread to listen for incoming messages.
     */
    public NetworkMember() {
        try {
            serverSocket = new ServerSocket(0);
            //we add ourselves as a connection port so when we initiate a connection to another peer, it can now our public port
            //this way all peers pass our public port information to each other
            connectionPorts.add(identifier());
            System.out.println(identifier() + " started");
            executorServerSocket.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    while (true) {
                        ObjectSocket socket = new ObjectSocket(serverSocket.accept());
                        System.out.println(identifier() + " got connection from " + socket.toString() + "             " + socket.identifier());
                        addToPeers(socket);
                        requestExchangeConnectionPorts(socket);
                    }
                }
            });

            executorSockets.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    while (true) {
                        try {
                            for (ObjectSocket peer : peers.values()) {
                                if (peer.available() > 0) {
                                    handlePeerMessage(peer);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param host
     * @param port
     */
    public void connectPeer(String host, int port) {
        try {
            connectionPorts.add(getIdentifierFromHostPort(host, port));
            Socket sck = new Socket(host, port);
            ObjectSocket socket = new ObjectSocket(sck);
            //this is a public port for connection requests. We keep a list of them to pass the list to new members.
            addToConnectionPorts(socket);
            //this is a peer now
            addToPeers(socket);
            System.out.println(identifier() + " is connected to " + socket.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return local port that this instance run on
     */
    public int getServerPort() {
        if (serverSocket == null)
            return 0;
        else
            return serverSocket.getLocalPort();
    }

    public ConcurrentHashMap<String, ObjectSocket> getPeersList() {
        return (ConcurrentHashMap<String, ObjectSocket>) peers;
    }

    /**
     * If the message got from peer is a RequestMessage, it is related to network operations therefore it is handled
     * Otherwise message is redirected to observer application
     *
     * @param socket Socket of peer
     */
    private void handlePeerMessage(ObjectSocket socket) {
        try {
            Object objectMessage = socket.readObject();
            if (objectMessage instanceof RequestMessage) {
                RequestMessage requestMessage = (RequestMessage) objectMessage;
                System.out.println("Message ( " + identifier() + " ) " + requestMessage.msg.name() + " received from " + getIdentifierFromHostPort(socket.getRemoteHostAddress(), socket.getRemoteHostPort()));
                switch (requestMessage.msg) {
                    case EXCHANGE_CONNECTION_PORTS:
                        messageExchangeConnectionPorts(socket, requestMessage);
                        break;
                    case PROVIDE_CONNECTION_PORTS:
                        messageProvideConnectionPorts(socket, requestMessage);
                        break;
                }
            } else {
                ApplicationMessage applicationMessage = (ApplicationMessage) objectMessage;
                System.out.println("Message ( " + identifier() + " ) " + applicationMessage.msg.name() + " received from " + getIdentifierFromHostPort(socket.getRemoteHostAddress(), socket.getRemoteHostPort()));
                notifyObserver(socket, applicationMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes an object to outputstream of desired target and flushes
     *
     * @param object should implement Serializable
     * @param socket ObjectSocket to receive the object
     * @return
     */
    public boolean sendObject(Serializable object, ObjectSocket socket) {
        try {
            socket.sendObjectAndFlush(object);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void addToConnectionPorts(ObjectSocket socket) {
        connectionPorts.add(getIdentifierFromHostPort(socket.getSocket().getInetAddress().toString().split("/")[1], socket.getSocket().getPort()));
    }

    /**
     * Sends the list of server ports of peers it knows. Also requests the same from the peer.
     *
     * @param socket Peer to send this message
     */
    private void requestExchangeConnectionPorts(ObjectSocket socket) {
        System.out.println("Connection ports of " + identifier() + " ------> " + connectionPorts.toString());
        RequestMessage req = new RequestMessage();
        req.msg = RequestMessage.MSG.EXCHANGE_CONNECTION_PORTS;
        req.data = connectionPorts;
        req.identifier = identifier();
        sendObject(req, socket);

    }

    /**
     * Sends ths list of server ports of peers it knows
     *
     * @param socket
     */
    private void requestProvideConnectionPorts(ObjectSocket socket) {
        System.out.println("Connection ports of " + identifier() + " ------> " + connectionPorts.toString());
        RequestMessage req = new RequestMessage();
        req.msg = RequestMessage.MSG.PROVIDE_CONNECTION_PORTS;
        req.data = connectionPorts;
        req.identifier = identifier();
        sendObject(req, socket);
    }

    /**
     * Broadcasts a message to all peers
     *
     * @param message
     */
    public void broadcast(RequestMessage message) {
        for (ObjectSocket peer : peers.values()) {
            sendObject(message, peer);
        }
    }

    /**
     * Broadcasts a message to all peers
     *
     * @param message
     */
    public void broadcast(ApplicationMessage message) {
        String ids = "";
        for (ObjectSocket peer : peers.values()) {
            sendObject(message, peer);
            ids += peer.identifier() + ";";
        }
        System.out.println(identifier() + " broadcasted to " + ids);
    }

    private void addToPeers(ObjectSocket socket) {
        peers.put(socket.identifier(), socket);
    }

    /**
     * It is called when an EXCHANGE_CONNECTION_PORTS request caught. Registers the given connection ports
     * connects all of them one by one and sends back the list of all known connection ports to the sender.
     *
     * @param socket
     * @param message
     */
    private void messageExchangeConnectionPorts(ObjectSocket socket, RequestMessage message) {
        try {
            connectionPorts.add(((String) message.identifier));
            for (String s : ((HashSet<String>) message.data)) {
                if (!connectionPorts.contains(s) && !pendingConnections.contains(s)) {
                    pendingConnections.add(s);
                }
            }
            for (String s : pendingConnections) {
                System.out.println("Connecting( " + identifier() + " ) to " + s);
                connectPeer(getHostFromIdentifier(s), getPortFromIdentifier(s));
            }
            pendingConnections.clear();

            requestProvideConnectionPorts(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It is called when a list of connection ports arrives. Connects all of them one by one
     *
     * @param socket
     * @param message
     */
    private void messageProvideConnectionPorts(ObjectSocket socket, RequestMessage message) {
        connectionPorts.add(((String) message.identifier));
        for (String s : ((HashSet<String>) message.data)) {
            if (!connectionPorts.contains(s) && !pendingConnections.contains(s)) {
                pendingConnections.add(s);
            }
        }
        for (String s : pendingConnections) {
            connectPeer(getHostFromIdentifier(s), getPortFromIdentifier(s));
        }
        pendingConnections.clear();
    }

    public String getHostFromIdentifier(String id) {
        return id.split(":")[0];
    }

    public int getPortFromIdentifier(String id) {
        return Integer.parseInt(id.split(":")[1]);
    }

    public String getIdentifierFromHostPort(String host, int port) {
        if (host.contains("/")) {
            host = host.split("/")[1];
        }
        return host + ":" + port;
    }

    public String identifier() {
        try {
            return getIdentifierFromHostPort(InetAddress.getLocalHost().toString().split("/")[1], getServerPort());
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * NetworkMember supports only one observer at a time. Registering a new observer removes previous one.
     * NetworkMember redirects every message other than its own internal messages to the observer.
     *
     * @param observer Observer object to be notified.
     */
    public void registerObserver(NetworkObserver observer) {
        this.observer = observer;
    }

    public void notifyObserver(ObjectSocket socket, ApplicationMessage message) {
        if (observer != null) {
            observer.onMessage(socket, message);
        }
    }
}
