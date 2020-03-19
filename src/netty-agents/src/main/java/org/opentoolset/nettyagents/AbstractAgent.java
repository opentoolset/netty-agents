// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;

import io.netty.handler.ssl.SslContext;

/**
 * Abstract Agent class contains common members for its childs ie. Client and Server Agents.
 * 
 * @author hadi
 */
public abstract class AbstractAgent {

	protected static Logger logger = Context.getLogger();

	private Context context = new Context();

	private SslContext sslContext;

	// ---

	/**
	 * Configuration object including configuration parameters for this agent.<br />
	 * Configuration parameters can be changed if needed. <br />
	 * All configuration adjustments should be made before calling the method "startup".
	 * 
	 * @return Configuration object
	 */
	protected abstract AbstractConfig getConfig();

	// ---

	/**
	 * Creates a request handler for a specific request type
	 * 
	 * @param <TReq>
	 * @param <TResp>
	 * @param classOfRequest Specifies the request type
	 * @param function Specifies the function which will be executed when this request is made
	 */
	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> void setRequestHandler(Class<TReq> classOfRequest, Function<TReq, TResp> function) {
		this.context.getMessageReceiver().setRequestHandler(classOfRequest, function);
	}

	/**
	 * Creates a request handler for a specific message type
	 * 
	 * @param <T>
	 * @param classOfMessage Specifies the message type
	 * @param consumer Specifies the consumer which will accept and process this message when it reaches to this agent
	 */
	public <T extends AbstractMessage> void setMessageHandler(Class<T> classOfMessage, Consumer<T> consumer) {
		this.context.getMessageReceiver().setMessageHandler(classOfMessage, consumer);
	}

	/**
	 * Starts peer identification mode. In this mode it is only allowed to exchage certificates between peers. No other communication is allowed. Any peer may give trust to other peers in this mode if they are authentic.
	 */
	public void startPeerIdentificationMode() {
		this.context.setTrustNegotiationMode(true);
	}

	/**
	 * Ends peer identification mod. After ending this mode, peers can communicate with each other.
	 */
	public void stopPeerIdentificationMode() {
		this.context.setTrustNegotiationMode(false);
	}

	// ---

	/**
	 * Returns context object holding several state variables during the life-cycle of this agent
	 * 
	 * @return
	 */
	public Context getContext() {
		return context;
	}

	/**
	 * Starts the agent up ie. by entering listening mode (for server) or making connection attempts to configured server-peer
	 */
	protected void startup() {
	}

	/**
	 * Returns SSL context object for using in SSL related operations
	 * 
	 * @return
	 */
	protected SslContext getSslContext() {
		return sslContext;
	}

	/**
	 * Sets SSL context object for using in SSL related operations
	 * 
	 * @return
	 */
	protected void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	// ---

	public static class AbstractConfig {

		private boolean tlsEnabled = Constants.DEFAULT_TLS_ENABLED;
		private PrivateKey priKey;
		private X509Certificate cert;

		// ---

		public boolean isTlsEnabled() {
			return tlsEnabled;
		}

		public PrivateKey getPriKey() {
			return priKey;
		}

		public X509Certificate getCert() {
			return cert;
		}

		// ---

		public AbstractConfig setTlsEnabled(boolean tlsEnabled) {
			this.tlsEnabled = tlsEnabled;
			return this;
		}

		public AbstractConfig setPriKey(String priKeyStr) throws InvalidKeyException {
			return setPriKey(Utils.buildPriKey(priKeyStr));
		}

		public AbstractConfig setPriKey(byte[] priKeyBytes) throws InvalidKeyException {
			RSAPrivateKey priKey = Utils.buildPriKey(priKeyBytes);
			return setPriKey(priKey);
		}

		public AbstractConfig setPriKey(PrivateKey priKey) {
			this.priKey = priKey;
			return this;
		}

		public AbstractConfig setCert(String certStr) throws CertificateException {
			return setCert(Utils.buildCert(certStr));
		}

		public AbstractConfig setCert(byte[] certBytes) throws CertificateException {
			X509Certificate cert = Utils.buildCert(certBytes);
			return setCert(cert);
		}

		public AbstractConfig setCert(X509Certificate cert) {
			this.cert = cert;
			return this;
		}
	}

	// ---

	public static class TrustManager implements X509TrustManager {

		private Supplier<Context> supplier;

		public TrustManager(Supplier<Context> supplier) throws GeneralSecurityException, IOException {
			this.supplier = supplier;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			if (!supplier.get().isTrustNegotiationMode()) {
				Utils.verifyCertChain(peerCertChain, supplier.get().getTrustedCerts());
			}
		}

		@Override
		public void checkServerTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			if (!supplier.get().isTrustNegotiationMode()) {
				Utils.verifyCertChain(peerCertChain, supplier.get().getTrustedCerts());
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
}
