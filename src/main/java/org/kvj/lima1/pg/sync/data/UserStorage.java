package org.kvj.lima1.pg.sync.data;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.sql.DataSource;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.kvj.lima1.pg.sync.rest.admin.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserStorage {

	public static class TokenInfo {

		long userID;
		String userName;
		long created;
		long accessed;
		String clientID;
		String token;

		public TokenInfo(long userID, String username, String clientID, String token) {
			this.userID = userID;
			this.userName = username;
			this.clientID = clientID;
			this.token = token;
			created = System.currentTimeMillis();
			accessed = created;
		}

		public void updateAccessed() {
			accessed = System.currentTimeMillis();
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject obj = new JSONObject();
			obj.put("token", token);
			obj.put("app", clientID);
			obj.put("created", created);
			obj.put("accessed", accessed);
			return obj;
		}
	}

	private static Logger log = LoggerFactory.getLogger(UserStorage.class);
	private static final String SALT = "lima1sync";
	private static final long TOKEN_CHECK_MSEC = 60 * 60 * 1000;
	private static Map<String, TokenInfo> webTokens = new HashMap<String, TokenInfo>();

	private static String passwordToHash(String password) {
		MessageDigest algorithm;
		try {
			algorithm = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			log.error("Error in password hashing", e);
			return password;
		}
		algorithm.reset();
		algorithm.update(new String(password + SALT).getBytes());
		byte[] messageDigest = algorithm.digest();
		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < messageDigest.length; i++) {
			hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
		}

		return hexString.toString();
	}

	public static String authorizeUser(DataSource ds, String username,
			String password, String token, String clientID, boolean web) {
		Connection c = null;
		try {
			String uName = username.toLowerCase().trim();
			String pass = passwordToHash(password);
			c = ds.getConnection();
			PreparedStatement existing = c
					.prepareStatement("select id, password from users where username=?");
			existing.setString(1, uName);
			ResultSet set = existing.executeQuery();
			long id = 0;
			if (!set.next()) {
				// Create user
				if (web) {
					// Not allowed to create users
					log.warn("User not found: " + uName);
					return "Username not found";
				}
				id = DAO.nextID(c);
				PreparedStatement createUser = c
						.prepareStatement("insert into users (id, username, password, created) values (?, ?, ?, ?)");
				createUser.setLong(1, id);
				createUser.setString(2, uName);
				createUser.setString(3, pass);
				createUser.setLong(4, System.currentTimeMillis());
				createUser.execute();
			} else {
				// Existing user
				if (!pass.equals(set.getString(2))) {
					log.warn("Password in incorrect for user " + uName);
					return "Password is incorrect";
				}
				id = set.getLong(1);
			}
			log.debug("Storing token: " + token);
			if (web) {
				// Store in memory
				synchronized (webTokens) {
					webTokens.put(token, new TokenInfo(id, uName, clientID, token));
				}
				return null;
			}
			PreparedStatement createToken = c
					.prepareStatement("insert into tokens (id, user_id, token, issued, accessed, client) values (?, ?, ?, ?, ?, ?)");
			createToken.setLong(1, DAO.nextID(c));
			createToken.setLong(2, id);
			createToken.setString(3, token);
			createToken.setLong(4, System.currentTimeMillis());
			createToken.setLong(5, System.currentTimeMillis());
			createToken.setString(6, clientID);
			createToken.execute();
			return null;
		} catch (Exception e) {
			log.error("Users error", e);
			return "DB error";
		} finally {
			DAO.closeConnection(c);
		}
	}

	public static long findUserByName(Connection c, String username)
			throws SQLException {
		PreparedStatement st = c
				.prepareStatement("select id from users where username=?");
		st.setString(1, username);
		ResultSet set = st.executeQuery();
		if (set.next()) {
			return set.getLong(1);
		}
		throw new SQLException("User " + username + " not found");
	}

	public static String verifyToken(DataSource ds, String token) {
		Connection c = null;
		try {
			synchronized (webTokens) {
				TokenInfo info = webTokens.get(token);
				if (null != info) {
					info.updateAccessed();
					return info.userName;
				}
			}
			c = ds.getConnection();
			PreparedStatement searchToken = c
					.prepareStatement("select t.id, u.username from tokens t, users u where t.user_id=u.id and t.token=?");
			searchToken.setString(1, token);
			ResultSet set = searchToken.executeQuery();
			if (!set.next()) {
				// Token not found/expired - error
				log.warn("Token {} not found - error", token);
				return null;
			}
			// Update token
			PreparedStatement updateToken = c
					.prepareStatement("update tokens set accessed=? where id=?");
			updateToken.setLong(1, System.currentTimeMillis());
			updateToken.setLong(2, set.getLong(1));
			updateToken.execute();
			return set.getString(2);
		} catch (Exception e) {
			log.error("Token error", e);
			return null;
		} finally {
			DAO.closeConnection(c);
		}
	}

	static Timer tokenTimer = new Timer("Token");
	static TimerTask tokenTask = null;

	public static void startTokenTimer() {
		tokenTask = new TimerTask() {

			@Override
			public void run() {
				synchronized (webTokens) {
					List<String> toRemove = new ArrayList<String>();
					for (String token : webTokens.keySet()) {
						TokenInfo info = webTokens.get(token);
						if (System.currentTimeMillis() - info.accessed > TOKEN_CHECK_MSEC) {
							// Expired
							toRemove.add(token);
						}
					}
					for (String token : toRemove) {
						webTokens.remove(token);
					}
				}
			}
		};
		tokenTimer.schedule(tokenTask, TOKEN_CHECK_MSEC, TOKEN_CHECK_MSEC);
	}

	public static void stopTokenTimer() {
		if (null != tokenTask) {
			tokenTask.cancel();
		}
	}

	public static UserInfo getUserInfo(DataSource ds, String username) {
		Connection c = null;
		try {
			c = ds.getConnection();
			PreparedStatement user = c
					.prepareStatement("select id, username, created, email, name, rights from users where username=?");
			user.setString(1, username);
			ResultSet set = user.executeQuery();
			if (!set.next()) {
				// Token not found/expired - error
				log.warn("User {} not found - error", username);
				return null;
			}
			return resultSetToUserInfo(set);
		} catch (Exception e) {
			log.error("Token error", e);
		} finally {
			DAO.closeConnection(c);
		}
		return null;
	}

	private static UserInfo resultSetToUserInfo(ResultSet set) throws SQLException {
		// id, username, created, email, name, rights
		UserInfo info = new UserInfo();
		info.id = set.getLong("id");
		info.created = set.getLong("created");
		info.email = set.getString("email");
		info.name = set.getString("name");
		info.username = set.getString("username");
		int rights = set.getInt("rights");
		for (UserInfo.UserRights right : UserInfo.UserRights.values()) {
			if ((rights & right.getMask()) != 0) {
				// Set
				info.rights.add(right);
			}
		}
		return info;
	}

	public static List<UserInfo> getUsers(DataSource ds) {
		Connection c = null;
		List<UserInfo> result = new ArrayList<UserInfo>();
		try {
			c = ds.getConnection();
			PreparedStatement user = c
					.prepareStatement("select id, username, created, email, name, rights from users order by created");
			ResultSet set = user.executeQuery();
			while (set.next()) {
				result.add(resultSetToUserInfo(set));
			}
		} catch (Exception e) {
			log.error("Token error", e);
		} finally {
			DAO.closeConnection(c);
		}
		return result;
	}

	public static UserInfo updateUser(DataSource ds, long id, String name, String email) {
		Connection c = null;
		try {
			c = ds.getConnection();
			PreparedStatement update = c.prepareStatement("update users set name=?, email=? where id=?");
			update.setString(1, name);
			update.setString(2, email);
			update.setLong(3, id);
			update.executeUpdate();
			PreparedStatement user = c
					.prepareStatement("select id, username, created, email, name, rights from users where id=?");
			user.setLong(1, id);
			ResultSet set = user.executeQuery();
			if (!set.next()) {
				// No such user
				log.warn("User {} not found - error", id);
				return null;
			}
			return resultSetToUserInfo(set);
		} catch (Exception e) {
			log.error("Token error", e);
		} finally {
			DAO.closeConnection(c);
		}
		return null;
	}

	public static List<TokenInfo> getTokens(DataSource ds, long id) {
		Connection c = null;
		List<TokenInfo> result = new ArrayList<TokenInfo>();
		try {
			synchronized (webTokens) {
				for (TokenInfo tokenInfo : webTokens.values()) {
					if (tokenInfo.userID == id) {
						result.add(tokenInfo);
					}
				}
			}
			c = ds.getConnection();
			PreparedStatement tokens = c
					.prepareStatement("select token, issued, accessed, client from tokens where user_id=? order by issued");
			tokens.setLong(1, id);
			ResultSet set = tokens.executeQuery();
			while (set.next()) {
				TokenInfo tokenInfo = new TokenInfo(id, "", set.getString(4), set.getString(1));
				tokenInfo.created = set.getLong(2);
				tokenInfo.accessed = set.getLong(3);
				result.add(tokenInfo);
			}
		} catch (Exception e) {
			log.error("Token error", e);
		} finally {
			DAO.closeConnection(c);
		}
		return result;
	}

	public static TokenInfo deleteToken(DataSource ds, long id, String token) {
		synchronized (webTokens) {
			TokenInfo tokenInfo = webTokens.get(token);
			if (null != tokenInfo) {
				// Found
				if (tokenInfo.userID != id) {
					log.error("User id is invalid");
					return null;
				}
				webTokens.remove(token);
				return tokenInfo;
			}
		}
		Connection c = null;
		try {
			c = ds.getConnection();
			PreparedStatement searchToken = c
					.prepareStatement("select id from tokens where user_id=? and token=?");
			searchToken.setLong(1, id);
			searchToken.setString(1, token);
			ResultSet set = searchToken.executeQuery();
			if (!set.next()) {
				// Token not found/expired - error
				log.warn("Token {} not found - error", token);
				return null;
			}
			TokenInfo tokenInfo = new TokenInfo(id, "", "", token);
			// Delete token
			PreparedStatement updateToken = c
					.prepareStatement("delete from tokens where id=?");
			updateToken.setLong(1, set.getLong(1));
			set.close();
			updateToken.execute();
			return tokenInfo;
		} catch (Exception e) {
			log.error("Token error", e);
			return null;
		} finally {
			DAO.closeConnection(c);
		}
	}
}
