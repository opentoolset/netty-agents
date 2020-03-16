// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

	private static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	public static boolean sslEnabled = Constants.DEFAULT_SSL_ENABLED;

	public static boolean peerIdentificationMode = false;

	private MessageSender messageSender = new MessageSender();

	private MessageReceiver messageReceiver = new MessageReceiver();

	// ---

	public static Logger getLogger() {
		return logger;
	}

	// ---

	public MessageSender getMessageSender() {
		return messageSender;
	}

	public MessageReceiver getMessageReceiver() {
		return messageReceiver;
	}
}
