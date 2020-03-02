// ---
// Copyright 2020 Mustafa Hadi Dilek
// All rights reserved
// ---
package tr.com.mhadidilek.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<MessageWrapper> {

	@Override
	protected void encode(ChannelHandlerContext ctx, MessageWrapper messageWrapper, ByteBuf out) throws Exception {
		String serializedMessageWrapper = messageWrapper.serialize();
		out.writeInt(serializedMessageWrapper.length());
		out.writeCharSequence(serializedMessageWrapper, Constants.DEFAULT_CHARSET);
	}
}