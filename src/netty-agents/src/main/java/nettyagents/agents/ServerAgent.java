// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents.agents;

import java.net.SocketAddress;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import nettyagents.AbstractAgent;
import nettyagents.AbstractMessage;
import nettyagents.AbstractRequest;
import nettyagents.Constants;
import nettyagents.Context;
import nettyagents.InboundMessageHandler;
import nettyagents.MessageDecoder;
import nettyagents.MessageEncoder;
import nettyagents.PeerContext;

public class ServerAgent extends AbstractAgent {

	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private ServerBootstrap bootstrap = new ServerBootstrap();

	private Config config = new Config();

	private Map<SocketAddress, PeerContext> clients = new ConcurrentHashMap<>();

	private SslContext sslCtx = null;

	private boolean shutdownRequested = false;

	// ---

	/**
	 * Configuration object including configuration parameters for this agent.<br />
	 * Configuration parameters can be changed if needed. <br />
	 * All configuration adjustments should be made before calling the method "startup".
	 * 
	 * @return Configuration object
	 */
	public Config getConfig() {
		return config;
	}

	public Map<SocketAddress, PeerContext> getClients() {
		return clients;
	}

	public void startup() {
		this.shutdownRequested = false;

		if (Context.sslEnabled) {
			try {
				SelfSignedCertificate ssc = new SelfSignedCertificate();
				sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
			} catch (CertificateException | SSLException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}

		this.bootstrap.group(this.bossGroup, this.workerGroup);
		this.bootstrap.channel(NioServerSocketChannel.class);
		this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

			@Override
			public void initChannel(SocketChannel channel) throws Exception {
				try {
					ChannelPipeline pipeline = channel.pipeline();

					if (sslCtx != null) {
						pipeline.addLast(sslCtx.newHandler(channel.alloc()));
					}

					pipeline.addLast(new MessageEncoder(), new MessageDecoder(), new InboundMessageHandler(context));
					pipeline.addLast(new ChannelHandler() {

						@Override
						public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
							SocketAddress remoteAddress = ctx.channel().remoteAddress();
							ServerAgent.this.clients.compute(remoteAddress, (key, value) -> addOrUpdateClientContext(key, value, ctx));
						}

						@Override
						public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
							SocketAddress remoteAddress = ctx.channel().remoteAddress();
							ServerAgent.this.clients.remove(remoteAddress);
						}

						@Override
						public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						}
					});
				} catch (Exception e) {
					logger.debug(e.getLocalizedMessage(), e);
				}
			}
		});

		this.bootstrap.option(ChannelOption.SO_BACKLOG, 128);
		this.bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		// this.bootstrap.localAddress(new InetSocketAddress(CVApiConstants.DEFAULT_MANAGER_HOST, CVApiConstants.DEFAULT_MANAGER_PORT));
		new Thread(() -> maintainConnection()).start();
	}

	public void shutdown() {
		this.context.getMessageSender().shutdown();
		this.bossGroup.shutdownGracefully();
		this.workerGroup.shutdownGracefully();
	}

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request, PeerContext peerContext) {
		return this.context.getMessageSender().doRequest(request, peerContext);
	}

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request, PeerContext peerContext, int timeoutSec) {
		return this.context.getMessageSender().doRequest(request, peerContext, timeoutSec);
	}

	public void sendMessage(AbstractMessage message, PeerContext peerContext) {
		this.context.getMessageSender().sendMessage(message, peerContext);
	}

	// ---

	private void maintainConnection() {
		try {
			while (!shutdownRequested) {
				ChannelFuture channelFuture = this.bootstrap.bind(this.config.getLocalPort()).sync();
				channelFuture.channel().closeFuture().sync();
			}
		} catch (InterruptedException e) {
			logger.error("Interrupted", e);
		}
	}

	private PeerContext addOrUpdateClientContext(SocketAddress key, PeerContext value, ChannelHandlerContext channelHandlerContext) {
		value = value != null ? value : new PeerContext();
		value.setChannelHandlerContext(channelHandlerContext);
		return value;
	}

	public class Config {

		private int localPort = Constants.DEFAULT_SERVER_PORT;

		// ---

		public int getLocalPort() {
			return localPort;
		}

		// ---

		public Config setLocalPort(int localPort) {
			this.localPort = localPort;
			return this;
		}
	}
}