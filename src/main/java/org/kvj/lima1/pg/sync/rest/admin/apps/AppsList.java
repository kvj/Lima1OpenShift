package org.kvj.lima1.pg.sync.rest.admin.apps;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.SchemaStorage;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.AppInfo;

public class AppsList extends BaseAdminServlet {

	private static final long serialVersionUID = -43896740995878497L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		initUserInfo(req);
		List<AppInfo> apps = SchemaStorage.getInstance().getApplications(DAO.getDataSource(getServletContext()));
		JSONObject result = new JSONObject();
		JSONArray arr = new JSONArray();
		for (AppInfo info : apps) {
			arr.put(info.toJSON());
		}
		result.put("list", arr);
		return result;
	}
}
