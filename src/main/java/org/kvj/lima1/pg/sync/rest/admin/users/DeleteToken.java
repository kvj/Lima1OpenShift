package org.kvj.lima1.pg.sync.rest.admin.users;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.UserStorage;
import org.kvj.lima1.pg.sync.data.UserStorage.TokenInfo;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo.UserRights;

public class DeleteToken extends BaseAdminServlet {

	private static final long serialVersionUID = 1153003257559928875L;

	@Override
	protected JSONObject post(JSONObject in, HttpServletRequest req) throws Exception {
		initUserInfo(req);
		long id = in.optLong("user_id", 0);
		String token = in.optString("token", "");
		if (!userInfo.hasRight(UserRights.UsersAdmin) && id != userInfo.id) {
			throw new Exception("Access denied");
		}
		TokenInfo tokenInfo = UserStorage.deleteToken(DAO.getDataSource(getServletContext()), id, token);
		if (null == tokenInfo) {
			throw new Exception("Token not found");
		}
		return tokenInfo.toJSON();
	}

}
