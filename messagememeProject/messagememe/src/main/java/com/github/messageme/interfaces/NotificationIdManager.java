package com.github.messageme.interfaces;

import java.util.Set;

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

    /**
     * Get the notification ID associated with the phoneNumber.
     * @param phoneNumber phone number of the contact
     * @param createIfNotFound if true, create a new notification ID
     * @return notification ID or -1 if not found and createIfNotFound set to false
     */
    int getId(String phoneNumber, boolean createIfNotFound);

    /**
     * Get the phone numbers for any active notifications
     * @return iterable of phone numbers
     */
    Set<String> getActiveNotificationPhoneNumbers();

    /**
     * Add a phone number to the set of active notifications
     */
    void addNotificationPhoneNumber(String phoneNumber);

    /**
     * Remove a phone number from the set of active notifications
     * @param phoneNumber phone number of the contact
     */
    void removeNotificationPhoneNumber(String phoneNumber);
}
