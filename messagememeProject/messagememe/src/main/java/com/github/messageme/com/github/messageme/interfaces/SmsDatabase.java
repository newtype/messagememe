package com.github.messageme.com.github.messageme.interfaces;

import java.util.List;

/**
 * Grouping related functionality for accessing the SMS
 * database.  Not sure if we'll actually want this
 * interface in the long-term, but it's a fun way to
 * make a to-do list.
 *
 * Created by keith on 12/16/13.
 */
public interface SmsDatabase {
    /**
     * Mark all messages from this contact as read.
     * @param phoneNumber phone number (not normalized)
     */
    void markRead(String phoneNumber);

    /**
     * Get all unread messages from this contact in
     * the order they were received.
     * @param phoneNumber phone number (not normalized)
     * @return list of the messages, in chronological order
     */
    List<String> getUnread(String phoneNumber);

    /**
     * Write this sent message to the SMS database.
     * @param phoneNumber recipient's phone number
     * @param messageBody the text
     */
    void writeSentMessage(String phoneNumber, String messageBody);
}
