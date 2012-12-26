package org.kvj.lima1.pg.sync.rest.admin.users;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.UserStorage;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo.UserRights;

public class UserList extends BaseAdminServlet {

	private static final long serialVersionUID = -2510739547849077550L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		initUserInfo(req);
		if (!userInfo.hasRight(UserRights.UsersAdmin)) {
			throw new Exception("Access denied");
		}
		List<UserInfo> users = UserStorage.getUsers(DAO.getDataSource(getServletContext()));
		JSONObject result = new JSONObject();
		JSONArray arr = new JSONArray();
		for (UserInfo user : users) {
			arr.put(user.toJSON());
		}
		result.put("list", arr);
		return result;
	}

}
