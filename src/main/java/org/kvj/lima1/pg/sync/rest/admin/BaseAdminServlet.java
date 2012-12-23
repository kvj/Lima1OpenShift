package org.kvj.lima1.pg.sync.rest.admin;

import javax.servlet.http.HttpServletRequest;

import org.apache.amber.oauth2.common.OAuth;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.UserStorage;
import org.kvj.lima1.pg.sync.rest.OAuthSecuredServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo;

@SuppressWarnings("serial")
public class BaseAdminServlet extends OAuthSecuredServlet {

	protected UserInfo userInfo = null;

	protected void initUserInfo(HttpServletRequest req) throws Exception {
		userInfo = UserStorage.getUserInfo(DAO.getDataSource(getServletContext()),
				(String) req.getAttribute(OAuth.OAUTH_CLIENT_ID));
		if (null == userInfo) {
			throw new Exception("Invalid user");
		}
	}

}
