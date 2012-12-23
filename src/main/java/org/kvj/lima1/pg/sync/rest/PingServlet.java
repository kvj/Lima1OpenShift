package org.kvj.lima1.pg.sync.rest;

import javax.servlet.http.HttpServletRequest;

import org.apache.amber.oauth2.common.OAuth;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.DataStorage;

public class PingServlet extends OAuthSecuredServlet {

	private static final long serialVersionUID = 5298590358488203846L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		String from = req.getParameter("from");
		long fromLong = Long.parseLong(from);
		Boolean haveData = DataStorage.haveData(
				DAO.getDataSource(getServletContext()),
				req.getParameter("app"),
				(String) req.getAttribute(OAuth.OAUTH_CLIENT_ID),
				(String) req.getAttribute(OAuth.OAUTH_TOKEN), fromLong);
		if (null == haveData) {
			throw new Exception("Error getting data");
		}
		JSONObject object = new JSONObject();
		object.put("d", haveData.booleanValue());
		return object;
	}

}
