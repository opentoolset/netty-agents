// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import org.opentoolset.nettyagents.AbstractAgent.AbstractConfig;
import org.opentoolset.nettyagents.MessageSender.OperationContext;
import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class InboundMessageHandler extends ChannelInboundHandlerAdapter {

	private static Logger logger = Context.getLogger();

	private Provider provider;

	public interface Provider {

		AbstractConfig getConfig();

		Context getContext();

		boolean verifyChannelHandlerContext(ChannelHandlerContext ctx);
	}

	// ---

	public InboundMessageHandler(Provider provider) {
		this.provider = provider;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (this.provider.getConfig().isTlsEnabled() && !this.provider.verifyChannelHandlerContext(ctx)) {
			return;
		}

		if (msg instanceof MessageWrapper) {
			MessageWrapper messageWrapper = (MessageWrapper) msg;

			String correlationId = messageWrapper.getCorrelationId();
			if (correlationId != null) {
				OperationContext operationContext = this.provider.getContext().getMessageSender().getWaitingRequests().get(correlationId);
				if (operationContext != null) {
					operationContext.setResponseWrapper(messageWrapper);
					Thread thread = operationContext.getThread();
					synchronized (thread) {
						if (thread.isAlive()) {
							thread.notify();
						}
					}
				} else {
					logger.warn("Response was ignored because of timeout");
				}
			} else {
				String id = messageWrapper.getId();
				if (id != null) {
					AbstractMessage response = this.provider.getContext().getMessageReceiver().handleRequest(messageWrapper);
					MessageWrapper responseWrapper = MessageWrapper.createResponse(response, id);
					ctx.writeAndFlush(responseWrapper);
				} else {
					this.provider.getContext().getMessageReceiver().handleMessage(messageWrapper);
				}
			}
		} else {
			logger.warn("Message couldn't be recognized");
		}
	}
}