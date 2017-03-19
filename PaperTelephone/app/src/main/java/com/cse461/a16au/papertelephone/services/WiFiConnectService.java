package com.cse461.a16au.papertelephone.services;

/**
 * TODO: class documentation
 */

class WiFiConnectService extends ConnectService {
    private static ConnectService ourInstance = new WiFiConnectService();

    private WiFiConnectService() {
    }

    static ConnectService getInstance() {
        return ourInstance;
    }

    @Override
    public boolean write(byte[] data, String device) {
        return false;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void connect(String address) {

    }
}
