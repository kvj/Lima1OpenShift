package org.kvj.lima1.pg.sync.rest.admin.data;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.DataStorage;
import org.kvj.lima1.pg.sync.rest.admin.BaseAdminServlet;
import org.kvj.lima1.pg.sync.rest.admin.model.StatInfo;

public class GetStats extends BaseAdminServlet {

	private static final long serialVersionUID = -661321714349934828L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		initUserInfo(req);
		String app = req.getParameter("app");
		StatInfo stat = DataStorage.getStat(DAO.getDataSource(getServletContext()), userInfo, app);
		JSONObject obj = new JSONObject();
		obj.put("f", stat.fileCount);
		obj.put("fs", stat.fileSize);
		JSONArray arr = new JSONArray();
		int count = 0;
		for (String stream : stat.objects.keySet()) {
			JSONObject one = new JSONObject();
			one.put("s", stream);
			one.put("c", stat.objects.get(stream));
			count += stat.objects.get(stream);
			arr.put(one);
		}
		obj.put("d", arr);
		obj.put("t", count);
		return obj;
	};
}
