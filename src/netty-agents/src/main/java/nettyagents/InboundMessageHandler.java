// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import nettyagents.MessageSender.OperationContext;

public class InboundMessageHandler extends ChannelInboundHandlerAdapter {

	private static Logger logger = Context.getLogger();

	private DataProvider dataProvider;

	public interface DataProvider {

		Context getContext();

		boolean verifyChannelHandlerContext(ChannelHandlerContext ctx);
	}

	// ---

	public InboundMessageHandler(DataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (Context.tlsEnabled && !this.dataProvider.verifyChannelHandlerContext(ctx)) {
			return;
		}

		if (msg instanceof MessageWrapper) {
			MessageWrapper messageWrapper = (MessageWrapper) msg;

			String correlationId = messageWrapper.getCorrelationId();
			if (correlationId != null) {
				OperationContext operationContext = this.dataProvider.getContext().getMessageSender().getWaitingRequests().get(correlationId);
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
					AbstractMessage response = this.dataProvider.getContext().getMessageReceiver().handleRequest(messageWrapper);
					MessageWrapper responseWrapper = MessageWrapper.createResponse(response, id);
					ctx.writeAndFlush(responseWrapper);
				} else {
					this.dataProvider.getContext().getMessageReceiver().handleMessage(messageWrapper);
				}
			}
		} else {
			logger.warn("Message couldn't be recognized");
		}
	}
}