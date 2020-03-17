// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

	private static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	public static boolean sslEnabled = Constants.DEFAULT_SSL_ENABLED;

	private boolean trustNegotiationMode = false;

	private MessageSender messageSender = new MessageSender(this);

	private MessageReceiver messageReceiver = new MessageReceiver();

	private Map<String, X509Certificate> trustedCerts = new HashMap<>();

	// ---

	public static Logger getLogger() {
		return logger;
	}

	// ---

	public boolean isTrustNegotiationMode() {
		return trustNegotiationMode;
	}

	public void setTrustNegotiationMode(boolean peerIdentificationMode) {
		this.trustNegotiationMode = peerIdentificationMode;
	}

	public MessageSender getMessageSender() {
		return messageSender;
	}

	public MessageReceiver getMessageReceiver() {
		return messageReceiver;
	}

	public Map<String, X509Certificate> getTrustedCerts() {
		return trustedCerts;
	}
}
