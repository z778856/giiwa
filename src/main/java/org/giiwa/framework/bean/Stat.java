/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
*/
package org.giiwa.framework.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.web.Language;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * The Class Stat is used to stat utility and persistence.
 * 
 * @author wujun
 *
 */
@Table(name = "gi_stat")
public class Stat extends Bean implements Comparable<Stat> {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Language lang = Language.getLanguage();

	public static enum SIZE {
		min, m10, m15, m30, hour, day, week, month, season, year
	};

	public static enum TYPE {
		delta, snapshot;
	}

	@Column(name = X.ID)
	protected long id;

	@Column(name = "module")
	protected String module; // 统计模块

	@Column(name = "date")
	protected String date; // 日期

	@Column(name = "time")
	protected long time; // 时间

	@Column(name = "size")
	protected String size;// size of the stat data

	public String getDate() {
		return date;
	}

	private static String table(String module) {
		return "gi_stat_"
				+ (module.replaceAll("\\.delta", "").replaceAll("\\.snapshot", "").replaceAll("[\\.-+]", "_"));
	}

	public String getModule() {
		return module;
	}

	/**
	 * Insert or update.
	 *
	 * @param module the module
	 * @param date   the date
	 * @param size   the size
	 * @param q0     the query
	 * @param v      the value
	 * @param n      the n
	 * @return the int
	 */
	public static int insertOrUpdate(String module, String date, SIZE size, W q0, V v, long... n) {
		if (v == null) {
			v = V.create();
		} else {
			v = v.copy();
		}

		try {
			W q = W.create().copy(v).and("date", date).and("size", size.toString()).and("module", module);

			String table = table(module);

			if (!Helper.exists(q, table, Helper.DEFAULT)) {

				long id = UID.hash(module + "_" + date + "_" + size + "_" + q0.toString());
				if (Helper.exists(W.create(X.ID, id), table, Helper.DEFAULT)) {
					// update
					for (int i = 0; i < n.length; i++) {
						v.set("n" + i, n[i]);
					}
					Helper.update(table, W.create(X.ID, id), v, Helper.DEFAULT);
				} else {
					v.append("date", date).force(X.ID, id).append("size", size.toString()).append("module", module);
					for (int i = 0; i < n.length; i++) {
						v.set("n" + i, n[i]);
					}

					return Helper.insert(v, table, Helper.DEFAULT);
				}

			} else {
				/**
				 * only update if count > original
				 */
				for (int i = 0; i < n.length; i++) {
					v.set("n" + i, n[i]);
				}
				return Helper.update(table, q, v);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable.compareTo(java.lang.Object)
	 */
	public int compareTo(Stat o) {
		if (this == o)
			return 0;

		int c = getDate().compareTo(o.getDate());
		return c;
	}

	public static void delta(long time, String name, SIZE[] sizes, V v, long... n) {
		if (sizes != null) {
			for (SIZE size : sizes) {
				delta(time, name, size, W.create(), v, n);
			}
		}
	}

	public static long[] time(SIZE size, String date) {
		if (SIZE.min == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.m10 == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.m15 == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.m30 == size) {
			long t = lang.parse(date, "yyyy-MM-dd/HH");
			t = Stat.tohour(t);
			return new long[] { t, t + X.AHOUR };

		} else if (SIZE.hour == size) {
			long t = lang.parse(date, "yyyy-MM-dd");
			t = Stat.today(t);
			return new long[] { t, t + X.ADAY };
		} else if (SIZE.day == size) {
			long t = lang.parse(date, "yyyy-MM");
			t = Stat.tomonth(t);
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(t);
			c.add(Calendar.MONTH, 1);
			return new long[] { t, c.getTimeInMillis() };
		} else if (SIZE.month == size) {
			long t = lang.parse(date, "yyyy");
			t = Stat.toyear(t);
			return new long[] { t, t + X.AYEAR };
		}

		return new long[] { 0, 0 };
	}

	public static void delta(long time, String name, SIZE size, W q, V v, long... n) {

		String date = format(time, size);
		String table = table(name);

		Stat s1 = Helper.load(table, q.and("module", name + "." + Stat.TYPE.snapshot).and("size", size.toString())
				.and("time", time, W.OP.lt).sort("time", -1), Stat.class);
		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = s1 == null ? n[i] : n[i] + s1.getLong("n" + i);
		}
		v.append("time", time);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.snapshot, date, size, q, v, d);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.delta, date, size, q, v, n);

	}

	public static void snapshot(long time, String name, SIZE[] sizes, W q, V v, long... n) {
		if (sizes != null) {
			for (SIZE size : sizes) {
				snapshot(time, name, size, q, v, n);
			}
		}
	}

	public static String format(long time, SIZE size) {

		if (SIZE.min == size) {
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.m10 == size) {
			time = time / X.AMINUTE / 10 * X.AMINUTE * 10;
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.m15 == size) {
			time = time / X.AMINUTE / 15 * X.AMINUTE * 15;
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.m30 == size) {
			time = time / X.AMINUTE / 30 * X.AMINUTE * 30;
			return lang.format(time, "yyyy-MM-dd/HH:mm");
		} else if (SIZE.hour == size) {
			return lang.format(time, "yyyy-MM-dd/HH");
		} else if (SIZE.day == size) {
			return lang.format(time, "yyyy-MM-dd");
		} else if (SIZE.week == size) {
			return lang.format(time, "yyyy|ww");
		} else if (SIZE.month == size) {
			return lang.format(time, "yyyy-MM");
		} else if (SIZE.season == size) {
			int season = X.toInt(lang.format(time, "MM")) / 3 + 1;
			return lang.format(time, "yyyy") + "/0" + season;
		} else if (SIZE.year == size) {
			return lang.format(time, "yyyy");
		} else {
			return lang.format(time, "yyyy-MM-dd");
		}
	}

	public static long parse(long time, SIZE size) {

		if (SIZE.min == size) {
			return time / X.AMINUTE * X.AMINUTE;
		} else if (SIZE.m10 == size) {
			return time / X.AMINUTE / 10 * X.AMINUTE * 10;
		} else if (SIZE.m15 == size) {
			return time / X.AMINUTE / 15 * X.AMINUTE * 15;
		} else if (SIZE.m30 == size) {
			return time / X.AMINUTE / 30 * X.AMINUTE * 30;
		} else if (SIZE.hour == size) {
			return time / X.AHOUR * X.AHOUR;
		} else if (SIZE.day == size) {
			return time / X.ADAY * X.ADAY;
		} else if (SIZE.week == size) {
			return time / X.AWEEK * X.AWEEK;
		} else if (SIZE.month == size) {
			return time / X.AMONTH * X.AMONTH;
		} else if (SIZE.season == size) {
			return time / X.AMONTH / 3 * X.AMINUTE * 3;
		} else if (SIZE.year == size) {
			return time / X.AYEAR * X.AYEAR;
		}

		return time;
	}

	/**
	 * 
	 * @param time the long of the timestamp
	 * @param name the string of the module name
	 * @param size the SIZE
	 * @param n    the data
	 */
	public static void snapshot(long time, String name, SIZE size, W q, V v, long... n) {

		String date = format(time, size);
		String table = table(name);

		Stat s1 = Helper.load(table, q.and("module", name + "." + TYPE.snapshot).and("size", size.toString())
				.and("time", time, W.OP.lt).sort("time", -1), Stat.class);
		long[] d = new long[n.length];
		for (int i = 0; i < d.length; i++) {
			d[i] = s1 == null ? n[i] : n[i] - s1.getLong("n" + i);
		}

		v.append("time", time);

		Stat.insertOrUpdate(name + "." + Stat.TYPE.snapshot, date, size, q, v, n);
		Stat.insertOrUpdate(name + "." + Stat.TYPE.delta, date, size, q, v, d);

	}

	public static Beans<Stat> load(String name, TYPE type, SIZE size, W q, int s, int n) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type).and("size", size.toString());

		String table = table(name);
		Beans<Stat> l1 = Helper.load(table, q, s, n, Stat.class);
		if (l1 != null && !l1.isEmpty()) {
			Set<String> dates = new HashSet<String>();
			for (int i = l1.size() - 1; i >= 0; i--) {
				Stat s1 = l1.get(i);
				String date = s1.getDate();
				if (dates.contains(date)) {
					l1.remove(i);
				} else {
					dates.add(date);
				}
			}
		}

		return l1;
	}

	public static Stat load(String name, TYPE type, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + type).and("size", size.toString());

		return Helper.load(table(name), q, Stat.class);
	}

	public static long max(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) Helper.max(q, field, table(name), Helper.DEFAULT));
	}

	public static long sum(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) Helper.sum(q, field, table(name), Helper.DEFAULT));
	}

	public static long avg(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) Helper.avg(q, field, table(name), Helper.DEFAULT));
	}

	public static long min(String field, String name, SIZE size, W q) {
		if (q == null) {
			q = W.create();
		}
		q.and("module", name + "." + TYPE.delta).and("size", size.toString());
		return X.toLong((Object) Helper.min(q, field, table(name), Helper.DEFAULT));
	}

	/**
	 * the start time of today
	 * 
	 * @param time the long of the timestamp
	 * 
	 * @return the truncated timestamp
	 */
	public static long today(long time) {
		return lang.parse(lang.format(time, "yyyy-MM-dd"), "yyyy-MM-dd");
	}

	public static long today() {
		return today(System.currentTimeMillis());
	}

	public static long tohour() {
		return tohour(System.currentTimeMillis());
	}

	public static long tohour(long time) {
		return lang.parse(lang.format(time, "yyyyMMddHH"), "yyyyMMddHH");
	}

	public static long tomin() {
		return tomin(System.currentTimeMillis());
	}

	public static long tomin(long time) {
		return lang.parse(lang.format(time, "yyyyMMddHHmm"), "yyyyMMddHHmm");
	}

	/**
	 * the start time of this week
	 * 
	 * @return
	 */
	public static long toweek() {
		return toweek(System.currentTimeMillis());
	}

	public static long toweek(long time) {
		return lang.parse(lang.format(time, "yyyy-w"), "yyyy-w");
	}

	/**
	 * the start time of this month
	 * 
	 * @return
	 */
	public static long tomonth() {
		return tomonth(System.currentTimeMillis());
	}

	public static long tomonth(long time) {
		return lang.parse(lang.format(time, "yyyy-MM"), "yyyy-MM");
	}

	/**
	 * the start time of this season
	 * 
	 * @return
	 */
	public static long toseason() {
		return toseason(System.currentTimeMillis());
	}

	public static long toseason(long time) {
		int season = X.toInt(lang.format(time, "MM")) / 3;
		return lang.parse(lang.format(time, "yyyy") + "-" + (season * 3), "yyyy-MM");
	}

	/**
	 * the start time of this year
	 * 
	 * @return
	 */
	public static long toyear() {
		return toyear(System.currentTimeMillis());
	}

	public static long toyear(long time) {
		return lang.parse(lang.format(time, "yyyy"), "yyyy");
	}

	@Override
	public void cleanup() {
		// min, hour, day, week,month, year

		// int days = Global.getInt("glog.keep.days", 7);
		//
		// int n = dao.delete(W.create("size", "min").and("created",
		// System.currentTimeMillis() - X.AWEEK * 2, W.OP.lt));
		// n += dao.delete(W.create("size", "hour").and("created",
		// System.currentTimeMillis() - days * X.ADAY, W.OP.lt));
		// n += dao.delete(W.create("size", "day").and("created",
		// System.currentTimeMillis() - days * X.AMONTH, W.OP.lt));
		// n += dao.delete(W.create("size", "month").and("created",
		// System.currentTimeMillis() - days * X.AYEAR, W.OP.lt));
		// n += dao.delete(
		// W.create("size", "season").and("created", System.currentTimeMillis() - days *
		// X.AYEAR, W.OP.lt));
		// n += dao.delete(W.create("size", "year").and("created",
		// System.currentTimeMillis() - days * X.AYEAR, W.OP.lt));
		//
		// if (n > 0) {
		// GLog.applog.info("dao", "cleanup", dao.tableName() + " cleanup=" + n, null,
		// null);
		// }

	}

	public static List<Stat> merge(String module, W q, String groupby, MergeFunc func) {
		// load from stat, and group

		String table = table(module);

		List<Stat> l1 = new ArrayList<Stat>();
		List<?> l2 = Helper.distinct(groupby, q.copy().and(groupby, null, W.OP.neq).and(groupby, X.EMPTY, W.OP.neq),
				table, Helper.DEFAULT);
		if (l2 != null) {
			for (Object o : l2) {
				Beans<Stat> bs = Helper.load(table, q.copy().and(groupby, o), 0, 10000, Stat.class);
				if (!bs.isEmpty()) {
					Stat s = bs.get(0);

					for (String name : s.keySet()) {
						if (name.startsWith("n")) {
							Object o1 = s.get(name);
							if (o1 instanceof Long) {
								long v = func.call(name, bs);
								s.set(name, v);
							}
						}
					}

					l1.add(s);
				}
			}
		}
		return l1;

	}

	@FunctionalInterface
	public static interface MergeFunc extends Serializable {
		public long call(String name, List<Stat> l1);
	}

	public static void main(String[] args) {
		long t = System.currentTimeMillis() + X.AMINUTE * 30;

		System.out.println(Stat.format(t, Stat.SIZE.min));
		System.out.println(Stat.format(t, Stat.SIZE.m10));
		System.out.println(Stat.format(t, Stat.SIZE.m15));
		System.out.println(Stat.format(t, Stat.SIZE.m30));

		SIZE s1 = SIZE.m10;
		String s2 = Stat.format(t, s1);
		long[] ss = Stat.time(s1, s2);

		System.out.println(s2 + ", " + Stat.format(ss[0], s1) + ", " + Stat.format(ss[1], s1));

	}

	public static List<?> distinct(String module, String field, W q) {
		String table = table(module);
		return Helper.distinct(field, q, table, Helper.DEFAULT);
	}

	public static long tom15() {
		return tom15(System.currentTimeMillis());
	}

	public static long tom15(long ms) {
		long hour = tohour(ms);

		ms = ms - hour;
		if (ms > X.AMINUTE * 45) {
			hour += X.AMINUTE * 45;
		} else if (ms > X.AMINUTE * 30) {
			hour += X.AMINUTE * 30;
		} else if (ms > X.AMINUTE * 15) {
			hour += X.AMINUTE * 15;
		}
		return hour;
	}

	public static long tom30() {
		return tom30(System.currentTimeMillis());
	}

	public static long tom30(long ms) {
		long hour = tohour(ms);

		ms = ms - hour;
		if (ms > X.AMINUTE * 30) {
			hour += X.AMINUTE * 30;
		}
		return hour;
	}

	public static long tom10() {
		return tom10(System.currentTimeMillis());
	}

	public static long tom10(long ms) {
		long hour = tohour(ms);

		ms = ms - hour;
		if (ms > X.AMINUTE * 50) {
			hour += X.AMINUTE * 50;
		} else if (ms > X.AMINUTE * 40) {
			hour += X.AMINUTE * 40;
		} else if (ms > X.AMINUTE * 30) {
			hour += X.AMINUTE * 30;
		} else if (ms > X.AMINUTE * 20) {
			hour += X.AMINUTE * 20;
		} else if (ms > X.AMINUTE * 10) {
			hour += X.AMINUTE * 10;
		}
		return hour;
	}

}
