package org.kvj.lima1.pg.sync.rest;

import javax.servlet.http.HttpServletRequest;

import org.apache.amber.oauth2.common.OAuth;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.DataStorage;
import org.kvj.lima1.pg.sync.data.SchemaStorage;

public class PingServlet extends OAuthSecuredServlet {

	private static final long serialVersionUID = 5298590358488203846L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		String app = req.getParameter("app");
		JSONObject schema = SchemaStorage.getInstance().getSchema(app);
		if (null == schema) {
			log.error("Schema not found for {}", app);
			throw new Exception("Schema not found");
		}
		int schemaRev = schema.optInt("_rev", 0);
		int rev = schemaRev;
		if (null != req.getParameter("rev")) {
			rev = Integer.parseInt(req.getParameter("rev"));
		}
		Boolean haveData = null;
		if (rev != schemaRev) {
			haveData = true;
		}
		if (null == haveData) {
			// Revision is same
			String from = req.getParameter("from");
			long fromLong = Long.parseLong(from);
			haveData = DataStorage.haveData(
					DAO.getDataSource(getServletContext()),
					app,
					(String) req.getAttribute(OAuth.OAUTH_CLIENT_ID),
					(String) req.getAttribute(OAuth.OAUTH_TOKEN), fromLong);
		}
		if (null == haveData) {
			throw new Exception("Error getting data");
		}
		JSONObject object = new JSONObject();
		object.put("d", haveData.booleanValue());
		return object;
	}
}
