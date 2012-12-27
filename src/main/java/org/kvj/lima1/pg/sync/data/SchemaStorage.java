package org.kvj.lima1.pg.sync.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.rest.admin.model.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaStorage {

	private static final String SCHEMA_FILE = "/schema.json";

	private static SchemaStorage instance = null;

	private Map<String, JSONObject> schemas = new HashMap<String, JSONObject>();
	private Logger log = LoggerFactory.getLogger(getClass());

	private SchemaStorage() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream(SCHEMA_FILE), "utf-8"));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			reader.close();
			JSONObject object = new JSONObject(builder.toString());
			for (@SuppressWarnings("unchecked")
			Iterator<String> it = object.keys(); it.hasNext();) {
				String key = it.next();
				log.info("Loaded schema for app: {}", key);
				schemas.put(key, object.getJSONObject(key));
			}
		} catch (Exception e) {
			log.error("Error loading schema storage", e);
		}
	}

	public static SchemaStorage getInstance() {
		if (null == instance) {
			instance = new SchemaStorage();
		}
		return instance;
	}

	public JSONObject getSchema(String app) {
		log.debug("Getting app schema: {}", app);
		return schemas.get(app);
	}

	public List<AppInfo> getApplications(DataSource ds) {
		List<AppInfo> apps = new ArrayList<AppInfo>();
		Connection c = null;
		try {
			c = ds.getConnection();
			PreparedStatement select = c
					.prepareStatement("select app, \"name\", description, id from apps order by app");
			PreparedStatement selectSchema = c
					.prepareStatement("select \"schema\" from shemas where app_id=? order by rev desc limit 1");
			ResultSet set = select.executeQuery();
			while (set.next()) {
				AppInfo info = new AppInfo(set.getString(1), set.getString(2), set.getString(3));
				long id = set.getLong(4);
				selectSchema.setLong(1, id);
				ResultSet schemaSet = selectSchema.executeQuery();
				if (schemaSet.next()) {
					// Have schema
					String schemaString = schemaSet.getString(1);
					try {
						JSONObject obj = new JSONObject(schemaString);
						int rev = obj.getInt("_rev");
						info.revision = rev;
						info.schema = obj;
						info.schemaString = schemaString;
					} catch (JSONException e) {
						log.warn("Error parsing schema");
					}
				}
				schemaSet.close();
				apps.add(info);
			}
			set.close();
		} catch (Exception e) {
			log.error("Apps select error", e);
		} finally {
			DAO.closeConnection(c);
		}
		return apps;
	}

	public AppInfo addApplication(DataSource ds, String app) throws StorageException {
		String application = app.trim().toLowerCase();
		if ("".equals(application)) {
			throw new StorageException("Application is empty");
		}
		Connection c = null;
		try {
			c = ds.getConnection();
			PreparedStatement select = c
					.prepareStatement("select app from apps where app=?");
			select.setString(1, application);
			ResultSet set = select.executeQuery();
			if (set.next()) {
				// Already created
				throw new StorageException("Application already exists");
			}
			set.close();
			PreparedStatement insert = c.prepareStatement("insert into apps (id, app) values (?, ?)");
			insert.setLong(1, DAO.nextID(c));
			insert.setString(2, application);
			insert.execute();
			AppInfo info = new AppInfo(application, "", "");
			return info;
		} catch (StorageException e) {
			throw e;
		} catch (Exception e) {
			log.error("Apps select error", e);
			throw new StorageException("Database error");
		} finally {
			DAO.closeConnection(c);
		}
	}

	public AppInfo updateApplication(DataSource ds, String app, String name, String desc, String schema)
			throws StorageException {
		Connection c = null;
		try {
			c = ds.getConnection();
			PreparedStatement select = c
					.prepareStatement("select app from apps where app=?");
			select.setString(1, app);
			ResultSet set = select.executeQuery();
			if (!set.next()) {
				// Not found
				throw new StorageException("Application not found");
			}
			long id = set.getLong(1);
			set.close();
			PreparedStatement selectSchema = c
					.prepareStatement("select \"schema\", rev from shemas where app_id=? order by rev desc limit 1");
			int rev = 0;
			JSONObject schemaObject = null;
			try {
				schemaObject = new JSONObject(schema);
				rev = schemaObject.optInt("_rev", 0);
			} catch (JSONException e) {
				throw new StorageException("Invalid schema");
			}
			ResultSet schemaSet = selectSchema.executeQuery();
			int nowRev = 0;
			if (schemaSet.next()) {
				nowRev = schemaSet.getInt(2);
			}
			if (rev > nowRev) {
				PreparedStatement insertSchema = c
						.prepareStatement("insert into shemas (id, app_id, created, rev, \"schema\") values (?, ?, ?, ?, ?)");
				insertSchema.setLong(1, DAO.nextID(c));
				insertSchema.setLong(2, id);
				insertSchema.setLong(3, System.currentTimeMillis());
				insertSchema.setInt(4, rev);
				insertSchema.setString(5, schema);
				insertSchema.execute();
			}
			PreparedStatement update = c.prepareStatement("update apps set name=?, desc=? where id=?");
			update.setString(1, name);
			update.setString(2, desc);
			update.setLong(3, id);
			update.execute();
			AppInfo info = new AppInfo(app, name, desc);
			return info;
		} catch (StorageException e) {
			throw e;
		} catch (Exception e) {
			log.error("Apps update error", e);
			throw new StorageException("Database error");
		} finally {
			DAO.closeConnection(c);
		}
	}
}
