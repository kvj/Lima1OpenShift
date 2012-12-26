package org.kvj.lima1.pg.sync.rest.admin.users;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.UserStorage;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo.UserRights;

public class UpdateUser extends BaseAdminServlet {

	private static final long serialVersionUID = -7178399770761754370L;

	@Override
	protected JSONObject post(JSONObject in, HttpServletRequest req) throws Exception {
		initUserInfo(req);
		long id = in.optLong("id", -1);
		String name = in.optString("name", "");
		String email = in.optString("email", "");
		if (!userInfo.hasRight(UserRights.UsersAdmin) && id != userInfo.id) {
			throw new Exception("Access denied");
		}
		UserInfo updated = UserStorage.updateUser(DAO.getDataSource(getServletContext()), id, name, email);
		if (null == updated) {
			throw new Exception("User not found");
		}
		return updated.toJSON();
	}
}
