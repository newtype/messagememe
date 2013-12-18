package com.github.messageme;

import com.github.messageme.com.github.messageme.interfaces.NotificationIdManager;

import java.util.HashMap;
import java.util.Set;

/**
 * Keeps track of notification IDs by using static variables.
 * This will break in low-memory situations where our
 * process is killed.
 *
 * Created by keith on 12/16/13.
 */
public class StaticVarNotificationIdManager implements NotificationIdManager {
    private static final HashMap<String,Integer> idMap = new HashMap<String,Integer>();
    private static final int ID_NOT_FOUND = -1;
    private static int nextId = 0;

    @Override
    public int getId(String phoneNumber) {
        return getId(phoneNumber, true);
    }

    @Override
    public int getId(String phoneNumber, boolean createIfNotFound) {
        Integer savedId = idMap.get(phoneNumber);
        if (savedId != null) {
            return savedId.intValue();
        }

        if (createIfNotFound) {
            idMap.put(phoneNumber, new Integer(nextId));
            return nextId++;
        }
        else {
            return ID_NOT_FOUND;
        }
    }

    @Override
    public Set<String> getActiveNotificationPhoneNumbers() {
        return idMap.keySet();
    }

    @Override
    public void addNotificationPhoneNumber(String phoneNumber) {
        // does nothing in this implementation - getting the ID adds a phone number to the set
    }

    @Override
    public void removeNotificationPhoneNumber(String phoneNumber) {
        idMap.remove(phoneNumber);
    }
}
