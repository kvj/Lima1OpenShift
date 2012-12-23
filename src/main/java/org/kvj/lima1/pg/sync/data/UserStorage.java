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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserStorage {

	static class TokenInfo {

		String userName;
		long created;
		long accessed;
		String clientID;

		public TokenInfo(String username, String clientID) {
			this.userName = username;
			this.clientID = clientID;
			created = System.currentTimeMillis();
			accessed = created;
		}

		public void updateAccessed() {
			accessed = System.currentTimeMillis();
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
					webTokens.put(token, new TokenInfo(uName, clientID));
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
}
