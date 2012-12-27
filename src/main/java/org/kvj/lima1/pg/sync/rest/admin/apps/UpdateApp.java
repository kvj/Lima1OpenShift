package org.kvj.lima1.pg.sync.rest.admin.apps;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.SchemaStorage;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.AppInfo;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo.UserRights;

public class UpdateApp extends BaseAdminServlet {

	private static final long serialVersionUID = -3949702825335894290L;

	@Override
	protected JSONObject post(JSONObject in, HttpServletRequest req) throws Exception {
		initUserInfo(req);
		if (!userInfo.hasRight(UserRights.ApplicationsAdmin)) {
			throw new Exception("Access Denied");
		}
		String app = in.optString("app", "");
		String name = in.optString("name", "");
		String desc = in.optString("desc", "");
		String schema = in.optString("schema", "");
		AppInfo info = SchemaStorage.getInstance().updateApplication(DAO.getDataSource(getServletContext()), app, name,
				desc, schema);
		return info.toJSON();
	}
}
