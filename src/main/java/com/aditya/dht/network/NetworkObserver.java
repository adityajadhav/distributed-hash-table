package com.aditya.dht.network;

import com.aditya.dht.ApplicationMessage;

/**
 * Created by neo on 11-07-2017.
 */
public interface NetworkObserver {

    public void onMessage(ObjectSocket socket, ApplicationMessage message);

}
