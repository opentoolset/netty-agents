// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import io.netty.channel.ChannelHandlerContext;

public class PeerContext {

	private ChannelHandlerContext channelHandlerContext;

	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}

	public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
		this.channelHandlerContext = channelHandlerContext;
	}
}
