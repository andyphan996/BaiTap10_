package vn.iotstar.services;

import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

@Service
public class JwtService {
	@Value("${security.jwt.secret-key}")
	private String secretKey;

	@Value("${security.jwt.expiration-time}")
	private long jwtExpiration;

	public String extractUsername(String token) throws ParseException, BadJOSEException, JOSEException {
		return extractClaim(token, JWTClaimsSet::getSubject);
	}

	public <T> T extractClaim(String token, ClaimsResolver<T> claimsResolver)
			throws ParseException, BadJOSEException, JOSEException {
		final JWTClaimsSet claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	public String generateToken(UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
		return buildToken(extraClaims, userDetails, jwtExpiration);
	}

	public long getExpirationTime() {
		return jwtExpiration;
	}

	private String buildToken(
		Map<String, Object> extraClaims,
		UserDetails userDetails,
		long expiration
	) {
		JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
		extraClaims.forEach(claimsBuilder::claim);

		JWTClaimsSet claimsSet = claimsBuilder
			.subject(userDetails.getUsername())
			.issueTime(new Date(System.currentTimeMillis()))
			.expirationTime(new Date(System.currentTimeMillis() + expiration))
			.build();

		// Header: { "typ": "JWT", "alg": "HS256" }
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
			.type(JOSEObjectType.JWT)
			.build();

		SignedJWT signedJWT = new SignedJWT(header, claimsSet);
		try {
			signedJWT.sign(new MACSigner(getSignInKey()));
		} catch (JOSEException e) {
			throw new IllegalStateException("Cannot sign JWT", e);
		}
		return signedJWT.serialize();
	}

	public boolean isTokenValid(String token, UserDetails userDetails)
			throws ParseException, BadJOSEException, JOSEException {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) throws ParseException, BadJOSEException, JOSEException {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) throws ParseException, BadJOSEException, JOSEException {
		return extractClaim(token, JWTClaimsSet::getExpirationTime);
	}

	/**
	 * Parse chuoi token, kiem tra chu ky HS256 va thoi han (exp) roi tra ve payload (claims).
	 * Nem ParseException neu sai dinh dang, BadJWSException neu sai chu ky,
	 * ExpiredJWTException neu token da het han.
	 */
	private JWTClaimsSet extractAllClaims(String token) throws ParseException, BadJOSEException, JOSEException {
		ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
		jwtProcessor.setJWSKeySelector(new JWSVerificationKeySelector<>(
			JWSAlgorithm.HS256,
			new ImmutableSecret<>(getSignInKey())
		));
		jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
			null,
			Set.of("sub", "iat", "exp")
		));
		return jwtProcessor.process(token, null);
	}

	private byte[] getSignInKey() {
		return Base64.getDecoder().decode(secretKey);
	}

	@FunctionalInterface
	public interface ClaimsResolver<T> {
		T apply(JWTClaimsSet claims) throws ParseException;
	}
}
