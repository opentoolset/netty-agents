// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents.agents;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.opentoolset.nettyagents.AbstractAgent;
import org.opentoolset.nettyagents.AbstractMessage;
import org.opentoolset.nettyagents.AbstractRequest;
import org.opentoolset.nettyagents.Constants;
import org.opentoolset.nettyagents.Context;
import org.opentoolset.nettyagents.InboundMessageHandler;
import org.opentoolset.nettyagents.MessageDecoder;
import org.opentoolset.nettyagents.MessageEncoder;
import org.opentoolset.nettyagents.PeerContext;
import org.opentoolset.nettyagents.Utils;

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
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

/**
 * Server Agent is a type of agent which makes listens incoming connection requests to this agent, and maintain communication with each connected peer.
 * 
 * @author hadi
 */
public class ServerAgent extends AbstractAgent {

	private EventLoopGroup bossGroup = new NioEventLoopGroup();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private ServerBootstrap bootstrap = new ServerBootstrap();

	private Config config = new Config();

	private Map<SocketAddress, PeerContext> clients = new ConcurrentHashMap<>();

	private boolean shutdownRequested = false;

	// ---

	@Override
	public Config getConfig() {
		return config;
	}

	/**
	 * Returns map of clients with socket address as key and PeerContext object as value
	 * 
	 * @return map of clients
	 */
	public Map<SocketAddress, PeerContext> getClients() {
		return clients;
	}

	@Override
	public void stopPeerIdentificationMode() {
		super.stopPeerIdentificationMode();

		for (Entry<SocketAddress, PeerContext> entry : new ArrayList<>(clients.entrySet())) {
			SocketAddress key = entry.getKey();
			PeerContext client = entry.getValue();
			if (!client.isTrusted()) {
				clients.remove(key);
				client.getChannelHandlerContext().close();
				client.setChannelHandlerContext(null);
			}
		}
	}

	@Override
	public void startup() {
		super.startup();

		this.shutdownRequested = false;

		buildSSLContextIfEnabled();

		this.bootstrap.group(this.bossGroup, this.workerGroup);
		this.bootstrap.channel(NioServerSocketChannel.class);
		this.bootstrap.childHandler(new ServerChannelInitializer());
		this.bootstrap.option(ChannelOption.SO_BACKLOG, 128);
		this.bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		// this.bootstrap.localAddress(new InetSocketAddress(CVApiConstants.DEFAULT_MANAGER_HOST, CVApiConstants.DEFAULT_MANAGER_PORT));
		new Thread(() -> maintainConnection()).start();
	}

	public void shutdown() {
		this.shutdownRequested = true;

		this.bossGroup.shutdownGracefully();
		this.workerGroup.shutdownGracefully();

		getContext().getMessageSender().shutdown();
	}

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request, PeerContext peerContext) {
		return getContext().getMessageSender().doRequest(request, peerContext);
	}

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request, PeerContext peerContext, int timeoutSec) {
		return getContext().getMessageSender().doRequest(request, peerContext, timeoutSec);
	}

	public void sendMessage(AbstractMessage message, PeerContext peerContext) {
		getContext().getMessageSender().sendMessage(message, peerContext);
	}

	// ---

	private void maintainConnection() {
		try {
			while (!this.shutdownRequested) {
				ChannelFuture channelFuture = this.bootstrap.bind(this.config.getLocalPort()).sync();
				channelFuture.channel().closeFuture().sync();
			}
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage(), e);
		}
	}

	private PeerContext addOrUpdateClientContext(SocketAddress key, PeerContext peerContext, ChannelHandlerContext channelHandlerContext, X509Certificate peerCert) {
		peerContext = peerContext != null ? peerContext : new PeerContext();
		peerContext.setCert(peerCert);
		peerContext.setChannelHandlerContext(channelHandlerContext);
		return peerContext;
	}

	private void buildSSLContextIfEnabled() {
		if (getConfig().isTlsEnabled()) {
			try {
				PrivateKey key = getConfig().getPriKey();
				X509Certificate cert = getConfig().getCert();

				SslContextBuilder builder = SslContextBuilder.forServer(key, cert);
				builder.trustManager(new TrustManager(() -> getContext()));
				builder.clientAuth(ClientAuth.REQUIRE);
				SslContext sslContext = builder.build();
				setSslContext(sslContext);
			} catch (IOException | GeneralSecurityException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
	}

	// ---

	public static class Config extends AbstractConfig {

		private int localPort = Constants.DEFAULT_SERVER_PORT;

		public int getLocalPort() {
			return localPort;
		}

		public Config setLocalPort(int localPort) {
			this.localPort = localPort;
			return this;
		}
	}

	// ---

	private final class ServerChannelInitializer extends ChannelInitializer<SocketChannel> implements InboundMessageHandler.Provider {

		private SslHandler sslHandler;

		@Override
		public void initChannel(SocketChannel channel) throws Exception {
			try {
				ChannelPipeline pipeline = channel.pipeline();

				SslContext sslContext = getSslContext();
				if (sslContext != null) {
					this.sslHandler = sslContext.newHandler(channel.alloc());
					this.sslHandler.setHandshakeTimeout(Constants.DEFAULT_TLS_HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS);
					pipeline.addLast(this.sslHandler);
				}

				pipeline.addLast(new MessageEncoder(), new MessageDecoder(), new InboundMessageHandler(this));
				pipeline.addLast(new ServerChannelHandler(this.sslHandler));
			} catch (Exception e) {
				logger.debug(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public AbstractConfig getConfig() {
			return ServerAgent.this.getConfig();
		}

		@Override
		public Context getContext() {
			return ServerAgent.this.getContext();
		}

		@Override
		public boolean verifyChannelHandlerContext(ChannelHandlerContext ctx) {
			boolean result = true;
			result = result || getContext().isTrustNegotiationMode();
			result = result || ServerAgent.this.clients.values().stream().anyMatch(client -> Utils.verifyChannelHandlerContext(ctx, client));
			return result;
		}
	}

	private final class ServerChannelHandler implements ChannelHandler {

		private SslHandler sslHandler;

		public ServerChannelHandler(SslHandler sslHandler) {
			this.sslHandler = sslHandler;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			SocketAddress remoteAddress = ctx.channel().remoteAddress();

			if (getConfig().isTlsEnabled()) {
				this.sslHandler.handshakeFuture().addListener(future -> onHandshakeCompleted(ctx, remoteAddress));
			} else {
				ServerAgent.this.clients.compute(remoteAddress, (key, value) -> addOrUpdateClientContext(key, value, ctx, null));
			}
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			SocketAddress remoteAddress = ctx.channel().remoteAddress();
			ServerAgent.this.clients.remove(remoteAddress);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		}

		private void onHandshakeCompleted(ChannelHandlerContext ctx, SocketAddress remoteAddress) {
			try {
				Certificate[] peerCerts = this.sslHandler.engine().getSession().getPeerCertificates();
				Context context = getContext();
				if (!context.isTrustNegotiationMode()) {
					Utils.verifyCertChain(peerCerts, context.getTrustedCerts());
				}

				Certificate peerCert = peerCerts[0];
				if (peerCert instanceof X509Certificate) {
					PeerContext client = ServerAgent.this.clients.compute(remoteAddress, (key, value) -> addOrUpdateClientContext(key, value, ctx, (X509Certificate) peerCert));
					if (!context.isTrustNegotiationMode()) {
						client.setTrusted(true);
					}
				}
			} catch (Exception e) {
				logger.debug(e.getLocalizedMessage(), e);
			}
		}
	}
}