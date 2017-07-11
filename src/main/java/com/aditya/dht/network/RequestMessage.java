package com.aditya.dht.network;

import java.io.Serializable;

/**
 * Created by neo on 11-07-2017.
 */
public class RequestMessage implements Serializable {

    public enum MSG {
        EXCHANGE_CONNECTION_PORTS,
        PROVIDE_CONNECTION_PORTS,
        LEAVE_NETWORK;
    }

    public MSG msg;

    public Serializable data;
    public String identifier;
}
