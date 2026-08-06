package com.my4cut.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

interface FcmClient {

    String send(Message message) throws FirebaseMessagingException;
}
