// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;

import io.netty.handler.ssl.SslContext;
import nettyagents.AbstractAgent.AbstractConfig.Peer;

public abstract class AbstractAgent {

	protected static Logger logger = Context.getLogger();

	private Context context = new Context();

	private SslContext sslContext;

	// ---

	protected abstract AbstractConfig getConfig();

	// ---

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> void setRequestHandler(Class<TReq> classOfRequest, Function<TReq, TResp> function) {
		this.context.getMessageReceiver().setRequestHandler(classOfRequest, function);
	}

	public <T extends AbstractMessage> void setMessageHandler(Class<T> classOfMessage, Consumer<T> consumer) {
		this.context.getMessageReceiver().setMessageHandler(classOfMessage, consumer);
	}

	public void startPeerIdentificationMode() {
		Context.peerIdentificationMode = true;
	}

	public void stopPeerIdentificationMode() {
		Context.peerIdentificationMode = false;
	}

	// ---

	protected void startup() {
	}

	protected Context getContext() {
		return context;
	}

	protected SslContext getSslContext() {
		return sslContext;
	}

	protected void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	// ---

	public static class AbstractConfig {

		private PrivateKey priKey;
		private X509Certificate cert;

		private Map<String, Peer> trustedPpeers = new HashMap<>();

		// ---

		public PrivateKey getPriKey() {
			return priKey;
		}

		public X509Certificate getCert() {
			return cert;
		}

		public Map<String, Peer> getTrustedPeers() {
			return trustedPpeers;
		}

		// ---

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

		public static class Peer {

			private String id;
			private String ipAddress;
			private X509Certificate cert;

			public Peer(X509Certificate cert) {
				this.id = id;
				this.ipAddress = ipAddress;
				this.cert = cert;
			}

			public String getId() {
				return id;
			}

			public String getIpAddress() {
				return ipAddress;
			}

			public X509Certificate getCert() {
				return cert;
			}
		}
	}

	// ---

	public static class TrustManager implements X509TrustManager {

		private Collection<Peer> trustedPeers = new HashSet<>();

		public TrustManager(Collection<Peer> trustedPeers) throws GeneralSecurityException, IOException {
			this.trustedPeers = trustedPeers;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			if (!Context.peerIdentificationMode) {
				Utils.verifyCertChain(peerCertChain, this.trustedPeers);
			}
		}

		@Override
		public void checkServerTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			if (!Context.peerIdentificationMode) {
				Utils.verifyCertChain(peerCertChain, this.trustedPeers);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
}
