package com.aditya.dht;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by neo on 11-07-2017.
 */
public class DistributedHashTableTest {

    @Test
    public void testMapShouldContainWhatWasInsertedBefore() {
        DistributedHashTable<String, String> dhm = new DistributedHashTable<String, String>();
        dhm.put("Key", "Value");
        assertTrue(dhm.get("Key").equals("Value"));
    }

    @Test
    public void testOtherMapsShouldGetTheSameValue() {

        DistributedHashTable<String, String> peer1 = new DistributedHashTable<String, String>();
        DistributedHashTable<String, String> peer2 = new DistributedHashTable<String, String>();
        DistributedHashTable<String, String> peer3 = new DistributedHashTable<String, String>();

        peer2.connect(peer1.getHost(), peer1.getPort());
        peer3.connect(peer2.getHost(), peer2.getPort());

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        peer2.put("Key", "Value");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String ret1 = peer1.get("Key");
        String ret2 = peer2.get("Key");
        String ret3 = peer3.get("Key");
        assertEquals("Value", ret1);
        assertEquals("Value", ret2);
        assertEquals("Value", ret3);
    }

}
