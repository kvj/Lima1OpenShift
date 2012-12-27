package org.kvj.lima1.pg.sync.rest.admin.model;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class AppInfo {

	public String app;
	public String name;
	public String description;
	public int revision = 0;
	public String schemaString = null;
	public JSONObject schema = null;

	public AppInfo(String app, String name, String description) {
		this.app = app;
		this.name = name;
		this.description = description;
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("app", app);
		obj.put("name", name);
		obj.put("desc", description);
		obj.put("rev", revision);
		if (null != schemaString) {
			obj.put("schema", schemaString);
		}
		return obj;
	}
}
