// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

	private static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	public static boolean sslEnabled = Constants.DEFAULT_SSL_ENABLED;

	private static boolean peerIdentificationMode = false;

	private MessageSender messageSender = new MessageSender();

	private MessageReceiver messageReceiver = new MessageReceiver();

	private Map<String, PeerContext> trustedPeers = new HashMap<>();

	// ---

	public static Logger getLogger() {
		return logger;
	}

	public static boolean isPeerIdentificationMode() {
		return peerIdentificationMode;
	}

	public static void setPeerIdentificationMode(boolean peerIdentificationMode) {
		Context.peerIdentificationMode = peerIdentificationMode;
	}

	// ---

	public MessageSender getMessageSender() {
		return messageSender;
	}

	public MessageReceiver getMessageReceiver() {
		return messageReceiver;
	}

	public Map<String, PeerContext> getTrustedPeers() {
		return trustedPeers;
	}
}
