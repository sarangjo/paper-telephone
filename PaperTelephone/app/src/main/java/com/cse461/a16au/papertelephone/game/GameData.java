package com.cse461.a16au.papertelephone.game;

import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: class documentation
 */

public class GameData {
    // SETUP
    public static List<String> connectedDevices = new ArrayList<>();
    public static int nextDevice = 0;
    public static int startDevice = -1;
    public static int lastSuccessor = 0;
    public static Set<String> unplacedDevices = new HashSet<>();

    // IN-GAME
    public static CountDownTimer turnTimer = null;
    /**
     * A list of the devices that have not finished the current turn.
     */
    public static List<String> unfinishedDeviceList = new ArrayList<>();
    public static boolean isDone;
}
