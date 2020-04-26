// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Hex;

import io.netty.channel.ChannelHandlerContext;

public class Utils {

	public static void waitFor(int timeSec) {
		try {
			TimeUnit.SECONDS.sleep(timeSec);
		} catch (InterruptedException e) {
			Context.getLogger().warn(e.getLocalizedMessage(), e);
		}
	}

	public static boolean waitUntil(Supplier<Boolean> tester, int timeoutSec) {
		try {
			for (int i = 0; i < timeoutSec; i++) {
				if (tester.get()) {
					break;
				}
				TimeUnit.SECONDS.sleep(1);
			}
		} catch (InterruptedException e) {
			Context.getLogger().warn(e.getLocalizedMessage(), e);
		}

		return tester.get();
	}

	public static void verifyCertChain(Certificate[] peerCertChain, Map<String, X509Certificate> trustedCerts) throws CertificateException {
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		Certificate cert = peerCertChain[0];
		CertPath certPath = certFactory.generateCertPath(Arrays.asList(cert));

		try {
			CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");

			String fingerprint = getFingerprintAsHex(cert);
			X509Certificate trustedCert = trustedCerts.get(fingerprint);
			if (trustedCert != null) {
				try {
					Set<TrustAnchor> trustAnchors = new HashSet<>();
					TrustAnchor trustAnchor = new TrustAnchor(trustedCert, null);
					trustAnchors.add(trustAnchor);

					PKIXParameters params = new PKIXParameters(trustAnchors);
					params.setRevocationEnabled(false);

					PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) certPathValidator.validate(certPath, params);
					@SuppressWarnings("unused")
					PublicKey publicKey = result.getPublicKey();
					// Context.getLogger().info("Certificate verified. Public key: {}", publicKey);
					return;
				} catch (GeneralSecurityException e) {
					// Context.getLogger().debug(e.getLocalizedMessage(), e);
				}
			}
		} catch (NoSuchAlgorithmException e) {
			Context.getLogger().error(e.getLocalizedMessage(), e);
		}

		throw new CertificateException("Certificate couldn't be verified by custom trust manager...");
	}

	public static RSAPrivateKey buildPriKey(String priKeyStr) throws InvalidKeyException {
		byte[] priKeyBytes = Utils.base64Decode(priKeyStr);
		return buildPriKey(priKeyBytes);
	}

	public static RSAPrivateKey buildPriKey(byte[] priKeyBytes) throws InvalidKeyException {
		@SuppressWarnings("restriction")
		RSAPrivateKey priKey = sun.security.rsa.RSAPrivateCrtKeyImpl.newKey(priKeyBytes);
		return priKey;
	}

	public static X509Certificate buildCert(String peerCertStr) throws CertificateException {
		byte[] peerCertBytes = Utils.base64Decode(peerCertStr);
		return buildCert(peerCertBytes);
	}

	public static X509Certificate buildCert(byte[] peerCertBytes) throws CertificateException {
		@SuppressWarnings("restriction")
		X509Certificate peerCert = new sun.security.x509.X509CertImpl(peerCertBytes);
		return peerCert;
	}

	public static String getFingerprintAsHex(Certificate cert) {
		byte[] fingerPrintBytes = getFingerprint(cert);
		String hex = Hex.encodeHexString(fingerPrintBytes);
		return hex;
	}

	public static byte[] getFingerprint(Certificate cert) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.reset();
			byte[] fingerprint = md.digest(cert.getEncoded());
			return fingerprint;
		} catch (NoSuchAlgorithmException | CertificateEncodingException e) {
			return null;
		}
	}

	public static String base64Encode(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static byte[] base64Decode(String str) {
		return Base64.getDecoder().decode(str);
	}

	public static boolean verifyChannelHandlerContext(ChannelHandlerContext ctx, PeerContext peer) {
		return ctxBelongsToATrustedPeer(ctx, peer);
	}

	public static boolean ctxBelongsToATrustedPeer(ChannelHandlerContext ctx, PeerContext peer) {
		boolean result = true;
		result = result && ctx != null;
		result = result && ctx.channel() != null;
		result = result && peer != null;
		result = result && peer.isTrusted();
		result = result && peer.getChannelHandlerContext() != null;
		result = result && Objects.equals(ctx.channel(), peer.getChannelHandlerContext().channel());
		return result;
	}

	// ---
}
