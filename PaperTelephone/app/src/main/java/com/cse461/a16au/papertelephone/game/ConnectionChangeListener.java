package com.cse461.a16au.papertelephone.game;

public interface ConnectionChangeListener {
    void disconnection(String address);

    void connection(String address);
}
