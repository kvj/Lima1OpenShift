package org.kvj.lima1.pg.sync.rest.admin.model;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class UserInfo {

	public static final int APPS_ADMIN_MASK = 1;
	public static final int USERS_ADMIN_MASK = 2;
	public static final int SETTINGS_ADMIN_MASK = 4;

	public static enum UserRights {
		ApplicationsAdmin(APPS_ADMIN_MASK), UsersAdmin(USERS_ADMIN_MASK), SettingsAdmin(SETTINGS_ADMIN_MASK);
		private int mask;

		private UserRights(int mask) {
			this.mask = mask;
		}

		public int getMask() {
			return mask;
		}
	}

	public long id;
	public long created;
	public String username;
	public String name;
	public String email;
	public Set<UserRights> rights = new HashSet<UserInfo.UserRights>();

	public JSONObject toJSON() throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("created", created);
		obj.put("username", username);
		obj.put("name", name);
		obj.put("email", email);
		JSONArray _rights = new JSONArray();
		for (UserRights right : rights) {
			_rights.put(right.toString());
		}
		obj.put("rights", _rights);
		return obj;
	}

	public boolean hasRight(UserRights right) {
		return rights.contains(right);
	}
}
