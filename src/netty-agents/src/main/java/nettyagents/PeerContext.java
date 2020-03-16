// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.security.cert.Certificate;

import io.netty.channel.ChannelHandlerContext;

public class PeerContext {

	private ChannelHandlerContext channelHandlerContext;
	private Certificate cert;
	private boolean trusted = false;

	// ---

	public PeerContext() {
	}

	// ---

	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}

	public Certificate getCert() {
		return cert;
	}

	public boolean isTrusted() {
		return trusted;
	}

	// ---

	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}

	public void setCert(Certificate cert) {
		this.cert = cert;
	}

	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}
}
