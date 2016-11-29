package com.cse461.a16au.papertelephone;

import java.util.UUID;

/**
 * Created by saran on 11/24/2016. TODO
 */
public class Constants {
    public static final UUID APP_UUID = UUID.fromString("b914c3a4-e47f-4fa8-b23a-8e55a5981e5f");
    public static final String APP_NAME = "PaperTelephone";
    public static final int MESSAGE_CONNECTED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final String TOAST = "toast";

    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";

    public static final byte[] HEADER_IMAGE = "IMAGE".getBytes();
    public static final int MESSAGE_RECV_IMAGE = 0;
    public static final int MESSAGE_RECV_TEXT = 1;
}
