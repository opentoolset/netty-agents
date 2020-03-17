// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents.agents;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;

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
import nettyagents.AbstractAgent;
import nettyagents.AbstractMessage;
import nettyagents.AbstractRequest;
import nettyagents.Constants;
import nettyagents.Context;
import nettyagents.InboundMessageHandler;
import nettyagents.MessageDecoder;
import nettyagents.MessageEncoder;
import nettyagents.PeerContext;
import nettyagents.Utils;

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

	public PeerContext getServer() {
		return server;
	}

	@Override
	public void stopPeerIdentificationMode() {
		super.stopPeerIdentificationMode();
		if (!this.server.isTrusted()) {
			this.server.setChannelHandlerContext(null);
			this.server.getChannelHandlerContext().close();
		}
	}

	@Override
	public void startup() {
		super.startup();

		if (Context.sslEnabled) {
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

		this.shutdownRequested = false;

		this.bootstrap.group(workerGroup);
		this.bootstrap.channel(NioSocketChannel.class);
		this.bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {

			private SslHandler sslHandler;

			@Override
			public void initChannel(SocketChannel channel) throws Exception {
				try {
					ChannelPipeline pipeline = channel.pipeline();

					SslContext sslContext = getSslContext();
					if (sslContext != null) {
						sslHandler = sslContext.newHandler(channel.alloc(), getConfig().getRemoteHost(), getConfig().getRemotePort());
						pipeline.addLast(sslHandler);
					}

					pipeline.addLast(new MessageEncoder(), new MessageDecoder(), new InboundMessageHandler(new InboundMessageHandler.DataProvider() {

						@Override
						public Context getContext() {
							return ClientAgent.this.getContext();
						}

						@Override
						public boolean verifyChannelHandlerContext(ChannelHandlerContext ctx) {
							return getContext().isTrustNegotiationMode() || Utils.verifyChannelHandlerContext(ctx, ClientAgent.this.server);
						}
					}));

					pipeline.addLast(new ChannelHandler() {

						@Override
						public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
							if (Context.sslEnabled) {
								sslHandler.handshakeFuture().addListener(future -> {
									try {
										Certificate[] peerCerts = sslHandler.engine().getSession().getPeerCertificates();
										if (!getContext().isTrustNegotiationMode()) {
											Utils.verifyCertChain(peerCerts, getContext().getTrustedPeers().values());
										}

										Certificate peerCert = peerCerts[0];
										PeerContext server = ClientAgent.this.server;
										server.setCert((X509Certificate) peerCert);
										server.setChannelHandlerContext(ctx);

										if (!getContext().isTrustNegotiationMode()) {
											server.setTrusted(true);
										}
									} catch (Exception e) {
										// logger.debug(e.getLocalizedMessage(), e);
									}
								});
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
					});
				} catch (Exception e) {
					logger.debug(e.getLocalizedMessage(), e);
				}
			}
		});

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

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request) {
		return getContext().getMessageSender().doRequest(request, this.server);
	}

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> TResp doRequest(TReq request, int timeoutSec) {
		return getContext().getMessageSender().doRequest(request, this.server, timeoutSec);
	}

	public void sendMessage(AbstractMessage message) {
		getContext().getMessageSender().sendMessage(message, this.server);
	}

	// ---

	private void maintainConnection() {
		try {
			while (!shutdownRequested) {
				ChannelFuture channelFuture = null;
				while ((channelFuture = connectSafe()) == null && !shutdownRequested) {
					TimeUnit.SECONDS.sleep(1);
				}

				channelFuture.channel().closeFuture().sync();
			}
		} catch (InterruptedException e) {
			logger.debug(e.getLocalizedMessage(), e);
		}
	}

	private ChannelFuture connectSafe() throws InterruptedException {
		try {
			if (!shutdownRequested) {
				return this.bootstrap.connect().sync();
			}
		} catch (Exception e) {
			logger.debug(e.getLocalizedMessage(), e);
		}

		return null;
	}

	// ---

	public class Config extends AbstractConfig {

		private String remoteHost = Constants.DEFAULT_SERVER_HOST;
		private int remotePort = Constants.DEFAULT_SERVER_PORT;

		// ---

		public String getRemoteHost() {
			return remoteHost;
		}

		public int getRemotePort() {
			return remotePort;
		}

		// ---

		public Config setRemoteHost(String remoteHost) {
			this.remoteHost = remoteHost;
			return this;
		}

		public Config setRemotePort(int remotePort) {
			this.remotePort = remotePort;
			return this;
		}
	}
}
