package org.giiwa.bean;

import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

@Table(name = "gi_footprint", memo = "GI-数据痕迹")
public class Footprint extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static BeanDAO<String, Footprint> dao = BeanDAO.create(Footprint.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(name = "_table", memo = "数据表")
	String table;

	@Column(memo = "数据ID")
	String dataid;

	@Column(memo = "字段名")
	String field;

	@Column(memo = "用户ID")
	long uid;

	@Column(memo = "数据")
	Object data;

	transient User uid_obj;

	public User getUid_obj() {
		if (uid_obj == null) {
			uid_obj = User.dao.load(uid);
		}
		return uid_obj;
	}

	public String getTable() {
		return table;
	}

	public String getDataid() {
		return dataid;
	}

	public String getField() {
		return field;
	}

	public long getUid() {
		return uid;
	}

	public Object getData() {
		return data;
	}

	public static boolean create(Bean p, V v, long uid) {
		/**
		 * compare each data in V
		 */
		if (p == null || v == null)
			return false;

		String table = Helper.getTable(p.getClass());
		if (X.isEmpty(table)) {
			return false;
		}

		String dataid = p.get(X.ID).toString();

		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v0 = p == null ? null : p.get(name);
			Object v1 = v.value(name);
			if (!X.isSame(v0, v1)) {
				dao.insert(V.create("_table", table).append(X.ID, UID.uuid()).append("dataid", dataid)
						.append("field", name).append("data", v1).append("uid", uid));
			}
		}

		return true;
	}

	public static boolean changed(Bean p, V v) {
		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v0 = p == null ? null : p.get(name);
			Object v1 = v.value(name);
			if (!X.isSame(v0, v1)) {
				return true;
			}
		}
		return false;
	}

	public static boolean create(String table, V v, long uid) {
		/**
		 * compare each data in V
		 */
		if (X.isEmpty(table) || v == null)
			return false;

		String dataid = v.value(X.ID).toString();
		if (dataid == null)
			return false;

		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v1 = v.value(name);
			dao.insert(V.create("_table", table).append("dataid", dataid).append("field", name).append("data", v1)
					.append("uid", uid));
		}

		return true;
	}

	public static boolean create(BeanDAO<?, ? extends Bean> b, V v, long uid) {
		/**
		 * compare each data in V
		 */
		String table = b.tableName();
		if (X.isEmpty(table) || v == null)
			return false;

		String dataid = v.value(X.ID).toString();
		if (dataid == null)
			return false;

		for (String name : v.names()) {
			if (X.isIn(name, "updated", "created", "_id", "id"))
				continue;
			Object v1 = v.value(name);
			dao.insert(V.create("_table", table).append("dataid", dataid).append("field", name).append("data", v1)
					.append("uid", uid));
		}

		return true;
	}

	public static Beans<Footprint> load(String table, String dataid, String field, int s, int n) {
		return dao.load(W.create().and("_table", table).and("dataid", dataid).and("field", field).sort("created", -1),
				s, n);
	}

	public static Beans<Footprint> load(BeanDAO<?, ? extends Bean> b, String dataid, String field, int s, int n) {
		return dao.load(
				W.create().and("_table", b.tableName()).and("dataid", dataid).and("field", field).sort("created", -1),
				s, n);
	}

}
