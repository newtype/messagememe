package com.github.messageme.com.github.messageme.interfaces;

/**
 * Specs for managing notification IDs.
 * An implementation would deal with where
 * to actually store the information (database,
 * file, shared prefs, static vars, etc)
 *
 * Created by keith on 12/16/13.
 */
public interface NotificationIdManager {
    /**
     * Get the notification ID associated with the phoneNumber.
     * If there isn't one, create it.
     * @param phoneNumber phone number of the contact
     * @return notification ID
     */
    int getId(String phoneNumber);
}
