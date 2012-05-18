package org.kvj.lima1.pg.sync.rest;

import javax.servlet.http.HttpServletRequest;

import org.apache.amber.oauth2.common.OAuth;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.data.DAO;
import org.kvj.lima1.pg.sync.data.DataStorage;

public class OutServlet extends OAuthSecuredServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected JSONObject get(HttpServletRequest req) throws Exception {
		String from = req.getParameter("from");
		long fromLong = Long.parseLong(from);
		boolean incremental = "yes".equals(req.getParameter("inc"));
		JSONObject result = DataStorage.getData(
				DAO.getDataSource(getServletContext()),
				req.getParameter("app"),
				(String) req.getAttribute(OAuth.OAUTH_CLIENT_ID),
				(String) req.getAttribute(OAuth.OAUTH_TOKEN), fromLong,
				incremental);
		if (null == result) {
			throw new Exception("Error getting data");
		}
		return result;
	}
}
