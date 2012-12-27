package org.kvj.lima1.pg.sync.data;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.rsfilter.OAuthClient;
import org.apache.amber.oauth2.rsfilter.OAuthDecision;
import org.apache.amber.oauth2.rsfilter.OAuthRSProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthFilterProvider implements OAuthRSProvider {

	private static final String X_CLIENT = "X-Client-IP";
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public OAuthDecision validateRequest(String rsId, String token,
			HttpServletRequest req) throws OAuthProblemException {
		log.debug("Validate request: " + rsId + ", " + token);
		final String user = UserStorage.verifyToken(
				DAO.getDataSource(req.getSession().getServletContext()), token, getRemoteAddress(req));
		OAuthDecision decision = new OAuthDecision() {

			@Override
			public boolean isAuthorized() {
				return null != user;
			}

			@Override
			public Principal getPrincipal() {
				return new Principal() {

					@Override
					public String getName() {
						return user;
					}
				};
			}

			@Override
			public OAuthClient getOAuthClient() {
				return new OAuthClient() {

					@Override
					public String getClientId() {
						return user;
					}
				};
			}
		};
		return decision;
	}

	private String getRemoteAddress(HttpServletRequest req) {
		String ip = req.getRemoteAddr();
		if (null != req.getHeader(X_CLIENT)) {
			ip = req.getHeader(X_CLIENT);
		}
		if (null != req.getHeader(X_FORWARDED_FOR)) {
			ip = req.getHeader(X_FORWARDED_FOR);
		}
		return ip;
	}

}
