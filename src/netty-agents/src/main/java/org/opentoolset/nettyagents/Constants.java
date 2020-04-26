// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Constants {

	Charset CRYPTO_CHARSET = StandardCharsets.US_ASCII;
	Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	int DEFAULT_REQUEST_TIMEOUT_SEC = 20;
	int DEFAULT_CHANNEL_WAIT_SEC = 10;
	String DEFAULT_SERVER_HOST = "127.0.0.1";
	int DEFAULT_SERVER_PORT = 4444;
	boolean DEFAULT_TLS_ENABLED = false;
	int DEFAULT_TLS_HANDSHAKE_TIMEOUT_SEC = 60;
}
