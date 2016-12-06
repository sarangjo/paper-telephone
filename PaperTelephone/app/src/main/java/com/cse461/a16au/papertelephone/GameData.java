package com.cse461.a16au.papertelephone;

import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: class documentation
 */

public class GameData {
    public static List<String> connectedDevices = new ArrayList<>();
    public static int nextDevice = 0;
    public static int startDevice = -1;
    public static Set<String> unplacedDevices = new HashSet<>();
    public static int lastPair = 0;
    public static CountDownTimer turnTimer = null;
}
