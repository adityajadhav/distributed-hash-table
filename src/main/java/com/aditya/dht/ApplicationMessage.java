package com.aditya.dht;

import java.io.Serializable;

/**
 * Created by neo on 11-07-2017.
 */
public class ApplicationMessage implements Serializable {

    public enum MSG {
        GET,
        PUT,
        REMOVE,
        TABLE, REQUEST_TABLE;
    }

    public MSG msg;
    public Serializable tag;
    public Serializable value;
    public String identifier;
}
