// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.security.cert.X509Certificate;

import io.netty.channel.ChannelHandlerContext;

public class PeerContext {

	private ChannelHandlerContext channelHandlerContext;
	private X509Certificate cert;
	private boolean trusted = false;

	// ---

	public PeerContext() {
	}

	// ---

	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}

	public X509Certificate getCert() {
		return cert;
	}

	public boolean isTrusted() {
		return trusted;
	}

	// ---

	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}

	public void setCert(X509Certificate cert) {
		this.cert = cert;
	}

	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}
}
