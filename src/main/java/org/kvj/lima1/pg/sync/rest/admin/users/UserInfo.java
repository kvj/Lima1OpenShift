package org.kvj.lima1.pg.sync.rest.admin.users;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;

public class UserInfo extends BaseAdminServlet {

	private static final long serialVersionUID = -5801625934561337744L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		initUserInfo(req);
		return userInfo.toJSON();
	}

}
