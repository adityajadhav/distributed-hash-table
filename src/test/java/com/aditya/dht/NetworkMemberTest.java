package com.aditya.dht;

import com.aditya.dht.network.NetworkMember;
import com.aditya.dht.network.RequestMessage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by neo on 11-07-2017.
 */
public class NetworkMemberTest {

    @Test
    public void testIdentifierHasIpAndPort() {
        NetworkMember nm = new NetworkMember();
        assertEquals(nm.identifier(), nm.getHostFromIdentifier(nm.identifier()) + ":" + nm.getPortFromIdentifier(nm.identifier()));
    }

    public void testNetworkMemberWaitForPossibleConnectionsAndRespond() {
        NetworkMember serv = new NetworkMember();
        NetworkMember client = new NetworkMember();
        String[] hostId = serv.identifier().split(":");
        assertTrue(serv.getPeersList().size() == 0);
        client.connectPeer(hostId[0], Integer.parseInt(hostId[1]));
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, serv.getPeersList().size());
        assertEquals(1, client.getPeersList().size());
    }


    public void testNewMemberShouldGetPeersListFromNetwork() {
        NetworkMember serv = new NetworkMember();
        NetworkMember client = new NetworkMember();
        String[] hostId = serv.identifier().split(":");
        client.connectPeer(hostId[0], Integer.parseInt(hostId[1]));
        NetworkMember client2 = new NetworkMember();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client2.connectPeer(hostId[0], Integer.parseInt(hostId[1]));
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(2, client2.getPeersList().size());
    }


    public void testOldMemberShouldLearnANewMemberJoined() {
        NetworkMember serv = new NetworkMember();
        NetworkMember client = new NetworkMember();
        String[] hostId = serv.identifier().split(":");
        client.connectPeer(hostId[0], Integer.parseInt(hostId[1]));
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        NetworkMember client2 = new NetworkMember();
        client2.connectPeer(hostId[0], Integer.parseInt(hostId[1]));
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(2, client.getPeersList().size());
    }


    public void testSendObject() {
        NetworkMember s1 = new NetworkMember();
        NetworkMember s2 = new NetworkMember();
        String[] hostId = s1.identifier().split(":");
        s2.connectPeer(hostId[0], Integer.parseInt(hostId[1]));
        RequestMessage ms = new RequestMessage();
        ms.msg = RequestMessage.MSG.LEAVE_NETWORK;
        boolean succ = s2.sendObject(ms, s2.getPeersList().entrySet().iterator().next().getValue());
        assertTrue(succ);
    }

}
