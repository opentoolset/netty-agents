// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Context {

	private static Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

//	private boolean tlsEnabled = Constants.DEFAULT_TLS_ENABLED;

	private boolean trustNegotiationMode = false;

	private MessageSender messageSender = new MessageSender(this);

	private MessageReceiver messageReceiver = new MessageReceiver();

	private Map<String, X509Certificate> trustedCerts = new HashMap<>();

	// ---

	public static Logger getLogger() {
		return logger;
	}

	// ---

//	public boolean isTlsEnabled() {
//		return tlsEnabled;
//	}
	
	public boolean isTrustNegotiationMode() {
		return trustNegotiationMode;
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

	// ---
	
//	public void setTlsEnabled(boolean tlsEnabled) {
//		this.tlsEnabled = tlsEnabled;
//	}
	
	public void setTrustNegotiationMode(boolean peerIdentificationMode) {
		this.trustNegotiationMode = peerIdentificationMode;
	}
}
