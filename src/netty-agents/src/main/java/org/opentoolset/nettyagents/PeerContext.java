// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.security.cert.X509Certificate;

import io.netty.channel.ChannelHandlerContext;

/**
 * Defines an object containing contextual data about a peer, such as a channel handler context object indicating the socket connection is active; certificate of the peer after TLS handshake completed amd an indicator of whether this peer is trusted or not.
 * 
 * @author hadi
 */
public class PeerContext {

	private String id;
	private ChannelHandlerContext channelHandlerContext;
	private X509Certificate cert;
	private boolean trusted = false;

	// ---

	public PeerContext() {
	}

	// ---

	/**
	 * Reutns the ID of this peer for custom usage
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns channel handler context object indicating the socket connection is active
	 * 
	 * @return
	 */
	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}

	/**
	 * Returns certificate of the peer after TLS handshake completed
	 * 
	 * @return
	 */
	public X509Certificate getCert() {
		return cert;
	}

	/**
	 * Returns an indicator of whether this peer is trusted or not
	 * 
	 * @return
	 */
	public boolean isTrusted() {
		return trusted;
	}

	// ---

	public void setId(String id) {
		this.id = id;
	}

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
