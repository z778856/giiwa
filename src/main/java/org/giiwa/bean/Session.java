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
package org.giiwa.bean;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.*;
import org.giiwa.cache.*;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * Session of http request
 * 
 * @author yjiang
 * 
 */
public class Session implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Log log = LogFactory.getLog(Session.class);

	private static int MAX = 128;

	String sid;

	Map<String, Object> a = new TreeMap<String, Object>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.toString()
	 */
	public String toString() {
		return new StringBuilder("Session@{sid=").append(sid).append(",data=").append(a).append("}").toString();
	}

	/**
	 * Exists.
	 * 
	 * @param sid the sid
	 * @return true, if successful
	 */
	public static boolean exists(String sid) {
		Session o = Cache.get("session/" + sid);
		return o != null;
	}

	/**
	 * Delete.
	 * 
	 * @param sid the sid
	 */
	public static void delete(String sid) {
		Cache.remove("session/" + sid);
		SID.dao.delete(sid);
	}

	/**
	 * Load session by sid and ip
	 * 
	 * @param sid the sid
	 * @return the session
	 */
	public static Session load(String sid, String ip) {

		if (X.isEmpty(sid))
			return null;

//		log.debug("new session", new Exception());

		Session o = (Session) Cache.get("session/" + sid);

		if (o == null || (Global.getInt("session.baseip", 0) == 1 && !X.isSame(ip, o.get("ip")))) {
			o = new Session();

			/**
			 * set the session expired time
			 */
			o.sid = sid;
			try {
				o.set("ip", ip);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			log.debug("new session, sid=" + sid);

		}

		return o;
	}

	/**
	 * Checks for.
	 * 
	 * @param key the key
	 * @return true, if successful
	 */
	public boolean has(String key) {
		return a.containsKey(key);
	}

	/**
	 * Removes the.
	 * 
	 * @param key the key
	 * @return the session
	 */
	public Session remove(String key) {
		a.remove(key);
		return this;
	}

	/**
	 * Store the session with configured expired
	 * 
	 * @return the session
	 */
	public Session store() {
		long expired = Global.getLong("session.alive", X.AWEEK / X.AHOUR) * X.AHOUR;
		if (expired < 0) {
			expired = 7 * 24 * X.AHOUR;
		}

		return store(expired);
	}

	/**
	 * store the session with the expired
	 * 
	 * @param expired the expired timestamp, ms in future
	 * @return Session
	 */
	public Session store(long expired) {

		log.debug("store session, sid=" + sid + ", expired=" + expired);

		if (!Cache.set("session/" + sid, this, expired)) {
			log.error("set session failed !", new Exception("store session failed"));
		}

		return this;
	}

	/**
	 * Sets the.
	 * 
	 * @param key the key
	 * @param o   the o
	 * @return the session
	 */
	public Session set(String key, Object o) throws Exception {
		if (a.size() < MAX) {
			a.put(key, o);
			if (o instanceof User) {
				long uid = ((User) o).getId();
				SID.update(sid, uid);
			}
		} else {
			throw new Exception("exceed the MAX=" + MAX);
		}

		return this;
	}

	/**
	 * Sid.
	 * 
	 * @return the string
	 */
	public String sid() {
		return sid;
	}

	/**
	 * Gets the.
	 * 
	 * @param <T> the object
	 * @param key the key
	 * @return the object
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) a.get(key);
	}

	/**
	 * Gets the int.
	 * 
	 * @param key the key
	 * @return the int
	 */
	public int getInt(String key) {
		Integer i = (Integer) a.get(key);
		if (i != null) {
			return i;
		}
		return 0;
	}

	/**
	 * Clear.
	 */
	public void clear() {
		a.clear();
	}

	public static void expired(long uid) {
		W q = W.create().and("uid", uid).sort("sid", 1);
		Beans<SID> l1 = SID.dao.load(q, 0, 100);
		while (l1 != null && !l1.isEmpty()) {
			for (SID e : l1) {
				Session.delete(e.sid);
			}
			l1 = SID.dao.load(q, 0, 100);
		}

	}

	@Table(name = "gi_sid")
	static class SID extends Bean {
		private static final long serialVersionUID = 1L;

		public static final BeanDAO<String, SID> dao = BeanDAO.create(SID.class);

		@Column(memo = "会话ID")
		String sid;

		@Column(memo = "用户ID")
		long uid;

		public static void update(String sid, long uid) {
			try {
				if (dao.exists(sid)) {
					dao.update(sid, V.create("uid", uid));
				} else {
					dao.insert(V.create(X.ID, sid).append("sid", sid).append("uid", uid));
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	}

}
