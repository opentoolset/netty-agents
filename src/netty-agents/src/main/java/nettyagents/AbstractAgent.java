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
import java.util.ArrayList;
import java.util.List;
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

	// ---

	protected void startup() {
	}

	protected Context getContext() {
		return context;
	}

	public SslContext getSslContext() {
		return sslContext;
	}

	public void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	// ---

	public static class AbstractConfig {

		private PrivateKey priKey;
		private X509Certificate cert;

		private List<Peer> trustedPpeers = new ArrayList<>();

		// ---

		public PrivateKey getPriKey() {
			return priKey;
		}

		public X509Certificate getCert() {
			return cert;
		}

		public List<Peer> getTrustedPeers() {
			return trustedPpeers;
		}

		// ---

		public AbstractConfig setPriKey(String priKeyStr) throws InvalidKeyException {
			return setPriKey(Utils.base64Decode(priKeyStr));
		}

		@SuppressWarnings("restriction")
		public AbstractConfig setPriKey(byte[] priKeyBytes) throws InvalidKeyException {
			RSAPrivateKey priKey = sun.security.rsa.RSAPrivateCrtKeyImpl.newKey(priKeyBytes);
			return setPriKey(priKey);
		}

		public AbstractConfig setPriKey(PrivateKey priKey) {
			this.priKey = priKey;
			return this;
		}

		public AbstractConfig setCert(String certStr) throws CertificateException {
			return setCert(Utils.base64Decode(certStr));
		}

		@SuppressWarnings("restriction")
		public AbstractConfig setCert(byte[] certBytes) throws CertificateException {
			sun.security.x509.X509CertImpl cert = new sun.security.x509.X509CertImpl(certBytes);
			return setCert(cert);
		}

		public AbstractConfig setCert(X509Certificate cert) {
			this.cert = cert;
			return this;
		}

		public static class Peer {

			private String id;
			private String ipAddress;
			private String cert;

			public Peer(String cert) {
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

			public String getCert() {
				return cert;
			}
		}
	}

	// ---

	public static class TrustManager implements X509TrustManager {

		private List<Peer> trustedPeers = new ArrayList<>();

		public TrustManager(List<Peer> trustedPeers) throws GeneralSecurityException, IOException {
			this.trustedPeers = trustedPeers;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			Utils.verifyCertChain(peerCertChain, this.trustedPeers);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			Utils.verifyCertChain(peerCertChain, this.trustedPeers);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
}
