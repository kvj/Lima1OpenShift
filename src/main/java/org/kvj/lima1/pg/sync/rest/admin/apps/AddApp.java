package org.kvj.lima1.pg.sync.rest.admin.apps;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.SchemaStorage;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.AppInfo;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo.UserRights;

public class AddApp extends BaseAdminServlet {

	private static final long serialVersionUID = -3583616548196959956L;

	@Override
	protected JSONObject post(JSONObject in, HttpServletRequest req) throws Exception {
		if (!userInfo.hasRight(UserRights.ApplicationsAdmin)) {
			throw new Exception("Access Denied");
		}
		AppInfo app = SchemaStorage.getInstance().addApplication(DAO.getDataSource(getServletContext()),
				in.optString("app", ""));
		return app.toJSON();
	}
}
