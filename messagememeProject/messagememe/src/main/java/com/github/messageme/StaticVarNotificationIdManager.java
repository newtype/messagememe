package com.github.messageme;

import com.github.messageme.com.github.messageme.interfaces.NotificationIdManager;

import java.util.HashMap;

/**
 * Keeps track of notification IDs.
 * Created by keith on 12/16/13.
 */
public class StaticVarNotificationIdManager implements NotificationIdManager {
    private static final HashMap<String,Integer> idMap = new HashMap<String,Integer>();
    private static int nextId = 0;

    @Override
    public int getId(String phoneNumber) {
        Integer savedId = idMap.get(phoneNumber);
        if (savedId != null) {
            return savedId.intValue();
        }

        idMap.put(phoneNumber, new Integer(nextId));
        return nextId++;
    }
}
