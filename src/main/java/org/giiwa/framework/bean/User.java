/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.framework.bean;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.base.GImage;
import org.giiwa.core.base.MD5;
import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.dfile.DFile;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Language;
import org.giiwa.framework.web.Model;

/**
 * 
 * The {@code User} Class is base user class, all the login/access controlled in
 * giiwa was depended on the user, it contains all the user-info, and is
 * expandable. <br>
 * table="gi_user" <br>
 * 
 * MOST important field
 * 
 * <pre>
 * id: long, global unique,
 * name: login name, global unique
 * password: string of hashed
 * nickname: string of nickname
 * title: title of the user
 * hasAccess: test whether has the access token for the user
 * </pre>
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_user")
public class User extends Bean {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<Long, User> dao = BeanDAO.create(User.class);

	@Column(name = X.ID)
	private long id;

	@Column(name = "name")
	private String name;

	@Column(name = "nickname")
	private String nickname;

	@Column(name = "title")
	private String title;

	@Column(name = "password")
	private String password;

	@Column(name = "md5passwd")
	private String md5passwd;

	@Column(name = "createdip")
	private String createdip;

	@Column(name = "photo")
	private String photo;

	@Column(name = "createdua")
	private String createdua;

	@Column(name = "createdby")
	private long createdby;

	transient User createdby_obj;

	public User getCreatedby_obj() {
		if (createdby_obj == null) {
			createdby_obj = dao.load(createdby);
		}
		return createdby_obj;
	}

	public String getPhoto() {
		return photo;
	}

	/**
	 * get the unique ID of the user
	 * 
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * get the login name
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * get the nick name
	 * 
	 * @return String
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * get the phone number
	 * 
	 * @return String
	 */
	public String getPhone() {
		return this.getString("phone");
	}

	/**
	 * get the email address
	 * 
	 * @return String
	 */
	public String getEmail() {
		return this.getString("email");
	}

	/**
	 * get the title of the user
	 * 
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.bean.Bean.toString()
	 */
	public String toString() {
		return "User@{id=" + this.getId() + ",name=" + this.getString("name") + "}";
	}

	/**
	 * Instantiates a new user.
	 */
	public User() {

	}

	/**
	 * Checks if is role.
	 * 
	 * @param r the r
	 * @return true, if is role
	 */
	public boolean isRole(Role r) {
		try {
			return UserRole.dao.exists(W.create("uid", this.getId()).and("rid", r.getId()));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	private static void _check(V v, String... ss) throws Exception {
		if (ss == null || ss.length == 0)
			return;

		for (String s : ss) {
			String o = (String) v.value(s);
			if (X.isSame("name", s)) {
				String rule = Global.getString("user.name.rule", "^[a-zA-Z0-9]{4,16}$");
				if (!X.isEmpty(rule) && !o.matches(rule)) {
					throw new Exception(Language.getLanguage().get("user.bad.name"));
				}
			} else if (X.isSame("password", s)) {
				String rule = Global.getString("user.passwd.rule", "^[a-zA-Z0-9]{6,16}$");
				if (X.isEmpty(o) || (!X.isEmpty(rule) && !o.matches(rule))) {
					throw new Exception(Language.getLanguage().get("user.bad.passwd"));
				}
			}
		}
	}

	/**
	 * @deprecated
	 * @param v
	 * @return
	 * @throws Exception
	 */
	public synchronized static long create(V v) throws Exception {
		String name = v.value("name").toString();
		return create(name, v);
	}

	/**
	 * Creates a user with the values, <br>
	 * if the values contains "password" field, it will auto encrypt the password
	 * field.
	 *
	 * @param v the values
	 * @return long of the user id, if failed, return -1
	 * @throws Exception throw Exception if name or password not matches the setting
	 */
	public synchronized static long create(String name, V v) throws Exception {

		if (dao.exists(W.create("name", name))) {
			throw new Exception(Language.getLanguage().get("user.name.exists"));
		}

		v.append("name", name);
		// check name and password
		_check(v, "name", "password");

		String s = (String) v.value("password");
		if (s != null) {
			v.force("md5passwd", User.md5encrypt(s));
			v.force("password", encrypt(s));
		}

		Long id = (Long) v.value("id");
		if (id == null) {
			id = UID.next("user.id");
			try {
				while (dao.exists(id)) {
					id = UID.next("user.id");
				}
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
		}
		if (log.isDebugEnabled())
			log.debug("v=" + v);

		// check photo
		_checkphoto(-1, v);

		dao.insert(
				v.set(X.ID, id).set(X.CREATED, System.currentTimeMillis()).set(X.UPDATED, System.currentTimeMillis()));

		return id;
	}

	private static void _checkphoto(long id, V v) {

		Object nickname = v.value("nickname");

		if (X.isEmpty(nickname)) {
			return;
		}

		User u = dao.load(id);
		if (u != null && !X.isEmpty(u.photo)) {
			if (X.isSame(nickname, u.nickname) || !u.photo.startsWith("/user/photo/auto/")) {
				return;
			}
		}

		char c = nickname.toString().charAt(0);
		Language lang = Language.getLanguage();
		DFile f = Disk.seek("/user/photo/auto/" + lang.format(System.currentTimeMillis(), "yyyy/MM/dd") + "/"
				+ System.currentTimeMillis() + ".png");

		if (f != null) {
			try {
				GImage.cover(145, Character.toString(c).toUpperCase(), new Color((int) (128 * Math.random()),
						(int) (156 * Math.random()), (int) (156 * Math.random())), f.getOutputStream());
				v.append("photo", f.getFilename());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	}

	/**
	 * Load user by name and password.
	 *
	 * @param name     the name of the user
	 * @param password the password
	 * @return User, if not match anyoone, return null
	 */
	public static User load(String name, String password) {

		password = encrypt(password);

		if (log.isDebugEnabled())
			log.debug("name=" + name + ", passwd=" + password);
		// System.out.println("name=" + name + ", passwd=" + password);

		return dao.load(W.create("name", name).and("password", password).and("deleted", 1, W.OP.neq));

	}

	public boolean isDeleted() {
		return getInt("deleted") == 1;
	}

	/**
	 * Load user by name.
	 *
	 * @param name the name of the name
	 * @return User
	 */
	public static User load(String name) {
		return dao.load(W.create("name", name).and("deleted", 1, W.OP.neq).sort(X.UPDATED, -1));
	}

	/**
	 * Load users by access token name.
	 *
	 * @param access the access token name
	 * @return list of user who has the access token
	 */
	public static List<User> loadByAccess(String access) {

		Beans<Role> bs = Role.loadByAccess(access, 0, 1000);

		if (bs == null || bs.isEmpty()) {
			return null;
		}

		List<Long> l1 = new ArrayList<Long>();
		for (Role a : bs) {
			l1.add(a.getId());
		}

		Beans<UserRole> b2 = UserRole.dao.load(W.create().and("rid", l1), 0, 1000);

		if (b2 == null || b2.isEmpty()) {
			return null;
		}

		l1.clear();
		for (UserRole a : b2) {
			l1.add(a.getLong("uid"));
		}

		Beans<User> us = dao.load(W.create().and("id", l1).and("delete", 1, W.OP.neq).sort("name", 1), 0,
				Integer.MAX_VALUE);
		return us;

	}

	/**
	 * Validate the user with the password.
	 *
	 * @param password the password
	 * @return true, if the password was match
	 */
	public boolean validate(String password) {

		/**
		 * if the user has been locked, then not allow to login
		 */
		if (this.isLocked())
			return false;

		password = encrypt(password);
		return get("password") != null && get("password").equals(password);
	}

	/**
	 * whether the user has been locked
	 * 
	 * @return boolean
	 */
	public boolean isLocked() {
		return getInt("locked") > 0;
	}

	/**
	 * Checks whether has the access token.
	 * 
	 * @param name the name of the access token
	 * @return true, if has anyone
	 */
	public boolean hasAccess(String... name) {
		if (this.getId() == 0L) {
			for (String s : name) {
				if (X.isSame(s, "access.config.admin"))
					return true;
			}
			return false;
		}

		// log.debug("uid=" + this.getId() + ", access=" + Helper.toString(name));
		if (role == null) {
			getRole();
		}

		try {
			return role.hasAccess(id, name);
		} catch (Exception e) {
			// ignore
		}
		return false;
	}

	transient Roles role = null;

	/**
	 * get the roles for the user
	 * 
	 * @return Roles
	 */
	public Roles getRole() {
		if (role == null) {
			Beans<UserRole> bs = UserRole.dao.load(W.create("uid", this.getId()), 0, 100);
			if (bs != null) {
				List<Long> roles = new ArrayList<Long>();
				for (UserRole r : bs) {
					roles.add(r.getLong("rid"));
				}
				role = new Roles(roles);
			}
		}
		return role;
	}

	/**
	 * set a role to a user with role id
	 * 
	 * @param rid the role id
	 */
	public void setRole(long rid) {
		try {
			if (!UserRole.dao.exists(W.create("uid", this.getId()).and("rid", rid))) {
				UserRole.dao.insert(V.create("uid", this.getId()).set("rid", rid));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Removes the role.
	 * 
	 * @param rid the rid
	 */
	public void removeRole(long rid) {
		UserRole.dao.delete(W.create("uid", this.getId()).and("rid", rid));
	}

	/**
	 * Removes the all roles.
	 */
	public void removeAllRoles() {
		UserRole.dao.delete(W.create("uid", this.getId()));

	}

	/**
	 * encrypt the password
	 * 
	 * @param passwd the password
	 * @return the string
	 */
	public static String encrypt(String passwd) {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}
		return UID.id(passwd);
	}

	public static String md5encrypt(String passwd) {
		if (X.isEmpty(passwd)) {
			return X.EMPTY;
		}
		return MD5.md5(passwd);
	}

	/**
	 * Load the users by the query.
	 *
	 * @param q      the query of the condition
	 * @param offset the start number
	 * @param limit  the number
	 * @return Beans
	 */
	public static Beans<User> load(W q, int offset, int limit) {
		return dao.load(q.and(X.ID, 0, W.OP.gt), offset, limit);
	}

	/**
	 * Update the user with the V.
	 *
	 * @param v the values
	 * @return int
	 * @throws Exception
	 */
	public int update(V v) throws Exception {
		for (String name : v.names()) {
			this.set(name, v.value(name));
		}
		return update(this.getId(), v);
	}

	/**
	 * update the user by the values, <br>
	 * if the values contains "password" field, it will auto encrypt the password
	 * field.
	 *
	 * @param id the user id
	 * @param v  the values
	 * @return int, 0 no user updated
	 * @throws Exception
	 */
	public static int update(long id, V v) throws Exception {

		v.remove("name");

		String passwd = (String) v.value("password");
		if (!X.isEmpty(passwd)) {

			_check(v, "password");
			v.force("md5passwd", md5encrypt(passwd));
			passwd = encrypt(passwd);
			v.force("password", passwd);
		} else {
			v.remove("password");
		}

		_checkphoto(id, v);
		return dao.update(id, v);
	}

	/**
	 * update the user by query
	 * 
	 * @param q the query
	 * @param v the value
	 * @return the number of updated
	 * @throws Exception
	 */
	public static int update(W q, V v) throws Exception {

		v.remove("name");

		String passwd = (String) v.value("password");
		if (!X.isEmpty(passwd)) {

			_check(v, "password");

			v.force("md5passwd", md5encrypt(passwd));
			passwd = encrypt(passwd);
			v.force("password", passwd);
		} else {
			v.remove("password");
		}
		return dao.update(q, v);
	}

	/***
	 * replace all the roles for the user
	 * 
	 * @param roles the list of role id
	 */
	public void setRoles(List<Long> roles) {
		this.removeAllRoles();
		for (long rid : roles) {
			this.setRole(rid);
		}
	}

	/**
	 * record the login failure, and record the user lock info.
	 *
	 * @param ip        the ip that login come from
	 * @param sid       the session id
	 * @param useragent the browser agent
	 * @return int of the locked times
	 */
	public int failed(String ip, String sid, String useragent) {
		set("failtimes", getInt("failtimes") + 1);

		if (this.getInt("failtimes") >= 3) {
			User.dao.update(getId(), V.create("locked", 1));
		}

		return Lock.locked(getId(), sid, ip, useragent);
	}

	/**
	 * record the logout info in database for the user.
	 *
	 * @return the int
	 */
	public int logout() {
		return dao.update(getId(), V.create("sid", X.EMPTY));
	}

	/**
	 * record login info in database for the user.
	 *
	 * @param sid the session id
	 * @param ip  the ip that the user come from
	 * @param v   the V object
	 * 
	 * @return the int
	 */
	public int logined(String sid, String ip, V v) {

		// update
		set("logintimes", getInt("logintimes") + 1);

		Lock.removed(getId(), sid);

		/**
		 * cleanup the old sid for the old logined user
		 */
		dao.update(W.create("sid", sid), V.create("sid", X.EMPTY));

		return dao.update(getId(),
				v.append("logintimes", getInt("logintimes")).append("ip", ip).append("failtimes", 0).append("locked", 0)
						.append("lockexpired", 0).append("sid", sid).append(X.UPDATED, System.currentTimeMillis()));

	}

	@Table(name = "gi_userrole")
	public static class UserRole extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, UserRole> dao = BeanDAO.create(UserRole.class);

		@Column(name = "uid")
		long uid;

		@Column(name = "rid")
		long rid;

	}

	/**
	 * The {@code Lock} Class used to record login failure log, was used by webgiiwa
	 * framework. <br>
	 * collection="gi_userlock"
	 * 
	 * @author joe
	 *
	 */
	@Table(name = "gi_userlock")
	public static class Lock extends Bean {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, Lock> dao = BeanDAO.create(Lock.class);

		/**
		 * Locked.
		 *
		 * @param uid       the uid
		 * @param sid       the sid
		 * @param host      the host
		 * @param useragent the useragent
		 * @return the int
		 */
		public static int locked(long uid, String sid, String host, String useragent) {

			return Lock.dao.insert(
					V.create("uid", uid).append(X.ID, UID.id(uid, sid, System.currentTimeMillis())).set("sid", sid)
							.set("host", host).set("useragent", useragent).set(X.CREATED, System.currentTimeMillis()));
		}

		/**
		 * Removed.
		 *
		 * @param uid the uid
		 * @return the int
		 */
		public static int removed(long uid) {
			return Lock.dao.delete(W.create("uid", uid));
		}

		/**
		 * Removed.
		 *
		 * @param uid the uid
		 * @param sid the sid
		 * @return the int
		 */
		public static int removed(long uid, String sid) {
			return Lock.dao.delete(W.create("uid", uid).and("sid", sid));
		}

		/**
		 * Load.
		 *
		 * @param uid  the uid
		 * @param time the time
		 * @return the list
		 */
		public static List<Lock> load(long uid, long time) {
			Beans<Lock> bs = Lock.dao.load(W.create("uid", uid).and(X.CREATED, time, W.OP.gt).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE);
			return bs;
		}

		/**
		 * Load by sid.
		 *
		 * @param uid  the uid
		 * @param time the time
		 * @param sid  the sid
		 * @return the list
		 */
		public static List<Lock> loadBySid(long uid, long time, String sid) {
			Beans<Lock> bs = Lock.dao.load(
					W.create("uid", uid).and(X.CREATED, time, W.OP.gt).and("sid", sid).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE);
			return bs;
		}

		/**
		 * Load by host.
		 *
		 * @param uid  the uid
		 * @param time the time
		 * @param host the host
		 * @return the list
		 */
		public static List<Lock> loadByHost(long uid, long time, String host) {
			Beans<Lock> bs = Lock.dao.load(
					W.create("uid", uid).and(X.CREATED, time, W.OP.gt).and("host", host).sort(X.CREATED, 1), 0,
					Integer.MAX_VALUE);
			return bs;
		}

		/**
		 * delete all user lock info for the user id
		 * 
		 * @param uid the user id
		 * @return the number deleted
		 */
		public static int cleanup(long uid) {
			return Lock.dao.delete(W.create("uid", uid));
		}

		public long getUid() {
			return getLong("uid");
		}

		public long getCreated() {
			return getLong(X.CREATED);
		}

		public String getSid() {
			return getString("sid");
		}

		public String getHost() {
			return getString("host");
		}

		public String getUseragent() {
			return getString("useragent");
		}

	}

	/**
	 * Delete the user by ID.
	 *
	 * @param id the id of the user
	 * @return int how many was deleted
	 */
	public static int delete(long id) {

		Lock.cleanup(id);

		return dao.delete(id);
	}

	private List<AuthToken> token_obj;

	public List<AuthToken> getTokens() {
		if (token_obj == null) {
			Beans<AuthToken> bs = AuthToken.load(this.getId());
			if (bs != null) {
				token_obj = bs;
			}
		}
		return token_obj;

	}

	/**
	 * check the database, if there is no "config.admin" user, then create the
	 * "admin" user, with "admin" as password
	 */
	public static void checkAndInit() {
		if (Helper.isConfigured()) {
			try {

				Configuration conf = Config.getConf();
				if (conf.getInt("reset", 0) == 1) {
					// remove root
					dao.delete(0L);
					Lock.dao.delete(W.create("uid", 0L));

					conf.setProperty("reset", 0);
					Config.save();
				}

				if (!dao.exists(0L)) {
					List<User> list = User.loadByAccess("access.config.admin");
					if (list == null || list.size() == 0) {
						String passwd = UID.random(16);
						try {
							PrintStream out = new PrintStream(Model.GIIWA_HOME + "/root.pwd");
							out.print(passwd);
							out.close();
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
						User.create("root",
								V.create("id", 0L).set("name", "root").set("password", passwd).set("nickname", "root"));
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static void to(JSON j) {
		int s = 0;
		W q = W.create().and(X.ID, 0, W.OP.gt).sort(X.ID, 1);

		List<JSON> l1 = new ArrayList<JSON>();
		Beans<User> bs = User.load(q, s, 100);
		while (bs != null && !bs.isEmpty()) {
			for (User e : bs) {
				l1.add(e.getJSON());
			}
			s += bs.size();
			bs = User.load(q, s, 100);
		}

		j.append("users", l1);
	}

	public static int from(JSON j) {
		int total = 0;
		Collection<JSON> l1 = j.getList("users");
		if (l1 != null) {
			for (JSON e : l1) {
				long id = e.getLong(X.ID);
				V v = V.fromJSON(e);
				v.remove(X.ID, "_id");
				User s = dao.load(W.create(X.ID, id));
				if (s != null) {
					dao.update(id, v);
				} else {
					dao.insert(v.append(X.ID, id));
				}
				total++;
			}
		}
		return total;
	}

	public static void repair() {
		Beans<User> bs = User.dao.load(W.create().sort("created", 1), 0, 1000);
		if (bs != null) {
			for (User u : bs) {
				if (X.isEmpty(u.getPhoto())) {
					V v = V.create();
					v.append("name", u.getName());
					v.append("nickname", u.getNickname());

					_checkphoto(u.getId(), v);

					dao.update(u.getId(), v);
				}
			}
		}
	}

	public Set<String> getAccesses() {
		if (role == null) {
			getRole();
		}

		return role.getAccesses();

	}

}
