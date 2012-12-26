package org.kvj.lima1.pg.sync.rest.admin.users;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.UserStorage;
import org.kvj.lima1.pg.sync.data.UserStorage.TokenInfo;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo.UserRights;

public class TokenList extends BaseAdminServlet {

	private static final long serialVersionUID = -6953811850854646636L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		initUserInfo(req);
		long id = Long.parseLong(req.getParameter("id"), 10);
		if (!userInfo.hasRight(UserRights.UsersAdmin) && id != userInfo.id) {
			throw new Exception("Access denied");
		}
		List<TokenInfo> tokens = UserStorage.getTokens(DAO.getDataSource(getServletContext()), id);
		JSONObject result = new JSONObject();
		JSONArray arr = new JSONArray();
		for (TokenInfo token : tokens) {
			arr.put(token.toJSON());
		}
		result.put("list", arr);
		return result;
	}

}
