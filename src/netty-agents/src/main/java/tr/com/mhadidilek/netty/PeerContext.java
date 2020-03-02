// ---
// Copyright 2020 Mustafa Hadi Dilek
// All rights reserved
// ---
package tr.com.mhadidilek.netty;

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
