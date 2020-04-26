// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents.agents;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;

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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;

/**
 * Client Agent is a type of agent which makes connection attempts to a server-peer, and maintain communication with its peer.
 * 
 * @author hadi
 */
public class ClientAgent extends AbstractAgent {

	private static final String DEFAULT_IN_MEMORY_KEYSTORE_PW = "";

	private EventLoopGroup workerGroup = new NioEventLoopGroup();
	private Bootstrap bootstrap = new Bootstrap();

	private Config config = new Config();

	private PeerContext server = new PeerContext();

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

	/**
	 * Returns the oject defining the context of the peer-server.
	 * 
	 * @return
	 */
	public PeerContext getServer() {
		return server;
	}

	@Override
	public void stopPeerIdentificationMode() {
		super.stopPeerIdentificationMode();
		if (!this.server.isTrusted()) {
			this.server.getChannelHandlerContext().close();
			this.server.setChannelHandlerContext(null);
		}
	}

	@Override
	public void startup() {
		super.startup();

		this.shutdownRequested = false;

		buildSSLContextIfEnabled();

		this.bootstrap.group(workerGroup);
		this.bootstrap.channel(NioSocketChannel.class);
		this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		this.bootstrap.handler(new ClientChannelInitializer());
		this.bootstrap.remoteAddress(new InetSocketAddress(config.getRemoteHost(), config.getRemotePort()));

		new Thread(() -> maintainConnection()).start();
	}

	public void shutdown() {
		try {
			this.shutdownRequested = true;

			ChannelHandlerContext channelHandlerContext = this.server.getChannelHandlerContext();
			if (channelHandlerContext != null) {
				channelHandlerContext.close();
			}

			this.workerGroup.shutdownGracefully();
		} catch (Exception e) {
			logger.warn(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Sends a request to the server and waits until receiving the response
	 * 
	 * @param <TReq>
	 * @param <TResp>
	 * @param request
	 * @return
	 */
	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request) {
		return getContext().getMessageSender().doRequest(request, this.server);
	}

	/**
	 * Sends a request to the server and waits until receiving the response or reaching to the specified timeout duration
	 * 
	 * @param <TReq>
	 * @param <TResp>
	 * @param request
	 * @param timeoutSec
	 * @return
	 */
	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request, int timeoutSec) {
		return getContext().getMessageSender().doRequest(request, this.server, timeoutSec);
	}

	/**
	 * Sends a message to the server without waiting a response
	 * 
	 * @param message
	 */
	public void sendMessage(AbstractMessage message) {
		getContext().getMessageSender().sendMessage(message, this.server);
	}

	// ---

	private void maintainConnection() {
		while (!this.shutdownRequested) {
			try {
				ChannelFuture channelFuture = null;
				while (!this.shutdownRequested && (channelFuture = connectSafe()) == null) {
					TimeUnit.SECONDS.sleep(1);
				}

				if (channelFuture != null) {
					channelFuture.channel().closeFuture().sync();
				}
			} catch (InterruptedException e) {
				logger.debug(e.getLocalizedMessage(), e);
			}
		}
	}

	private ChannelFuture connectSafe() throws InterruptedException {
		try {
			if (!this.shutdownRequested) {
				return this.bootstrap.connect().sync();
			}
		} catch (Exception e) {
			logger.debug(e.getLocalizedMessage(), e);
		}

		return null;
	}

	private void buildSSLContextIfEnabled() {
		if (getConfig().isTlsEnabled()) {
			try {
				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				{
					KeyStore keystore = KeyStore.getInstance("JKS");
					{
						PrivateKey key = getConfig().getPriKey();
						Certificate cert = getConfig().getCert();

						keystore.load(null);
						keystore.setCertificateEntry("my-cert", cert);
						keystore.setKeyEntry("my-key", key, DEFAULT_IN_MEMORY_KEYSTORE_PW.toCharArray(), new Certificate[] { cert });
					}

					keyManagerFactory.init(keystore, DEFAULT_IN_MEMORY_KEYSTORE_PW.toCharArray());
				}

				SslContextBuilder builder = SslContextBuilder.forClient();
				builder.keyManager(keyManagerFactory);
				builder.trustManager(new TrustManager(() -> getContext()));
				SslContext sslContext = builder.build();
				setSslContext(sslContext);
			} catch (IOException | GeneralSecurityException e) {
				logger.error(e.getLocalizedMessage(), e);
			}
		}
	}

	// ---

	public class Config extends AbstractConfig {

		private String remoteHost = Constants.DEFAULT_SERVER_HOST;
		private int remotePort = Constants.DEFAULT_SERVER_PORT;

		public String getRemoteHost() {
			return remoteHost;
		}

		public int getRemotePort() {
			return remotePort;
		}

		public Config setRemoteHost(String remoteHost) {
			this.remoteHost = remoteHost;
			return this;
		}

		public Config setRemotePort(int remotePort) {
			this.remotePort = remotePort;
			return this;
		}
	}

	// ---

	private final class ClientChannelInitializer extends ChannelInitializer<SocketChannel> implements InboundMessageHandler.Provider {

		private SslHandler sslHandler;

		@Override
		public void initChannel(SocketChannel channel) throws Exception {
			try {
				ChannelPipeline pipeline = channel.pipeline();

				SslContext sslContext = getSslContext();
				if (sslContext != null) {
					this.sslHandler = sslContext.newHandler(channel.alloc(), ClientAgent.this.getConfig().getRemoteHost(), ClientAgent.this.getConfig().getRemotePort());
					this.sslHandler.setHandshakeTimeout(Constants.DEFAULT_TLS_HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS);
					pipeline.addLast(this.sslHandler);
				}

				pipeline.addLast(new MessageEncoder(), new MessageDecoder(), new InboundMessageHandler(this));
				pipeline.addLast(new ClientChannelHandler(this.sslHandler));
			} catch (Exception e) {
				logger.debug(e.getLocalizedMessage(), e);
			}
		}

		@Override
		public AbstractConfig getConfig() {
			return ClientAgent.this.getConfig();
		}

		@Override
		public Context getContext() {
			return ClientAgent.this.getContext();
		}

		@Override
		public boolean verifyChannelHandlerContext(ChannelHandlerContext ctx) {
			boolean result = true;
			result = result || getContext().isTrustNegotiationMode();
			result = result || Utils.verifyChannelHandlerContext(ctx, ClientAgent.this.server);
			return result;
		}
	}

	private final class ClientChannelHandler implements ChannelHandler {

		private SslHandler sslHandler;

		public ClientChannelHandler(SslHandler sslHandler) {
			this.sslHandler = sslHandler;
		}

		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			if (getConfig().isTlsEnabled()) {
				sslHandler.handshakeFuture().addListener(future -> onHandshakeCompleted(ctx));
			} else {
				ClientAgent.this.server.setChannelHandlerContext(ctx);
			}
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			ctx.close();
			ClientAgent.this.server.setChannelHandlerContext(null);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		}

		private void onHandshakeCompleted(ChannelHandlerContext ctx) {
			try {
				Certificate[] peerCerts = this.sslHandler.engine().getSession().getPeerCertificates();
				if (!getContext().isTrustNegotiationMode()) {
					Utils.verifyCertChain(peerCerts, getContext().getTrustedCerts());
				}

				Certificate peerCert = peerCerts[0];
				if (peerCert instanceof X509Certificate) {
					PeerContext server = ClientAgent.this.server;
					server.setCert((X509Certificate) peerCert);
					server.setChannelHandlerContext(ctx);
					if (!getContext().isTrustNegotiationMode()) {
						server.setTrusted(true);
					}
				}
			} catch (Exception e) {
				logger.debug(e.getLocalizedMessage(), e);
			}
		}
	}
}
