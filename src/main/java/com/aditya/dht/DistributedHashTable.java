package com.aditya.dht;

import com.aditya.dht.network.NetworkMember;
import com.aditya.dht.network.NetworkObserver;
import com.aditya.dht.network.ObjectSocket;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by neo on 11-07-2017.
 */
public class DistributedHashTable<K, V> extends HashMap<K, V> implements NetworkObserver {
    /**
     * It handles network operations
     */
    private NetworkMember member;

    private boolean initialized;

    public DistributedHashTable() {
        super();
        member = new NetworkMember();
        member.registerObserver(this);
    }

    public void connect(String host, int port) {
        member.connectPeer(host, port);
    }

    public String getHost() {
        return member.getHostFromIdentifier(member.identifier());
    }

    public int getPort() {
        return member.getPortFromIdentifier(member.identifier());
    }

    public void onMessage(ObjectSocket socket, ApplicationMessage message) {
        switch (message.msg) {
            case PUT:
                messagePut(socket, message);
                break;
            case GET:
                messageGet(socket, message);
                break;
            case REMOVE:
                messageRemove(socket, message);
                break;
            case TABLE:
                messageTable(socket, message);
                break;
            case REQUEST_TABLE:
                messageRequestTable(socket, message);
                break;
        }
    }

    @Override
    public V put(K key, V value) {
        V returnValue = super.put(key, value);
        ApplicationMessage applicationMessage = new ApplicationMessage();
        applicationMessage.msg = ApplicationMessage.MSG.PUT;

        applicationMessage.tag = (Serializable) key;
        applicationMessage.value = (Serializable) value;
        member.broadcast(applicationMessage);
        return returnValue;
    }

    @Override
    public V get(Object key) {
        //TODO
        return super.get(key);
    }

    @Override
    public V remove(Object key) {

        ApplicationMessage applicationMessage = new ApplicationMessage();
        applicationMessage.msg = ApplicationMessage.MSG.REMOVE;
        applicationMessage.tag = (Serializable) key;
        member.broadcast(applicationMessage);
        return super.remove(key);
    }

    private void messagePut(ObjectSocket socket, ApplicationMessage message) {
        System.out.println(member.identifier() + " got PUT message from " + socket.identifier());
        super.put((K) message.tag, (V) message.value);
    }

    private void messageGet(ObjectSocket socket, ApplicationMessage message) {
        //TODO
    }

    private void messageRemove(ObjectSocket socket, ApplicationMessage message) {
        System.out.println(member.identifier() + " got REMOVE message from " + socket.identifier());
        super.remove(message.tag);
    }

    private void messageTable(ObjectSocket socket, ApplicationMessage message) {
        putAll((Map) message.value);
    }

    private void messageRequestTable(ObjectSocket socket, ApplicationMessage message) {
        ApplicationMessage applicationMessage = new ApplicationMessage();
        applicationMessage.msg = ApplicationMessage.MSG.TABLE;
        applicationMessage.value = (Serializable) super.clone();
        member.sendObject(applicationMessage, socket);
    }

    private void requestCurrentTable() {
        ApplicationMessage applicationMessage = new ApplicationMessage();
        applicationMessage.msg = ApplicationMessage.MSG.REQUEST_TABLE;
        member.broadcast(applicationMessage);
    }
}
