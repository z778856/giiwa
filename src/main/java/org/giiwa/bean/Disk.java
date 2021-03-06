package org.giiwa.bean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Node;
import org.giiwa.conf.Local;
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
import org.giiwa.dfile.DFile;
import org.giiwa.dfile.MyDFile;
import org.giiwa.misc.Base32;
import org.giiwa.misc.IOUtil;
import org.giiwa.web.Controller;

/**
 * Demo bean
 * 
 * @author joe
 * 
 */
@Table(name = "gi_disk", memo = "GI-磁盘")
public class Disk extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Disk.class);

	public static BeanDAO<Long, Disk> dao = BeanDAO.create(Disk.class);

	public static final String TYPE_ALL = "all";
	public static final String TYPE_DATA = "data";
	public static final String TYPE_NGINX = "nginx";

	@Column(memo = "唯一序号")
	long id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "路径")
	String path;

	@Column(memo = "type", value = "data, nginx")
	String type = "data";

	@Column(memo = "优先级")
	public int priority;

	@Column(name = "checktime")
	long checktime;

	@Column(memo = "开关")
	public int enabled; // 1: ok, 0: disabled

	@Column(memo = "是否可用", value = "0: ok, 1: bad")
	int bad; // 0:ok, 1: bad

	@Column(memo = "总空间")
	public long total;

	@Column(memo = "可用空间")
	public long free;

	@Column(memo = "文件数")
	public long count;

	public long getTotal() {
		return total;
	}

	public long getFree() {
		return free;
	}

	public boolean isLocal() {
		return X.isSame(node, Local.id());
	}

	public boolean getLive() {
		return (bad != 1) && (this.getNode_obj() != null) && (this.getNode_obj().getState() == 1)
				&& (System.currentTimeMillis() - this.getNode_obj().getUpdated() < Node.LOST);
	}

	public long getId() {
		return id;
	}

	public int getPriority() {
		return priority;
	}

	public String getPath() {
		return path;
	}

	public static long create(V v) {
		/**
		 * generate a unique id in distribute system
		 */
		long id = UID.next("disk.id");
		try {
			while (dao.exists(id)) {
				id = UID.next("disk.id");
			}
			dao.insert(v.force(X.ID, id));
			return id;
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return -1;
	}

	transient File file_obj;

	public File getFile_obj() {
		if (file_obj == null) {
			file_obj = new File(path);
			if (!file_obj.exists()) {
				file_obj.mkdirs();
			}
		}
		return file_obj;
	}

	transient FileStore filestore_obj;

	public FileStore getFilestore_obj() throws IOException {
		if (filestore_obj == null) {
			filestore_obj = Files.getFileStore(Paths.get(this.getFile_obj().toURI()).toRealPath());
		}
		return filestore_obj;
	}

	public long reloadTotal() {

		File f = this.getFile_obj();
		if (f.getTotalSpace() > 0) {
			return f.getTotalSpace();
		}

		try {
			FileStore s = this.getFilestore_obj();
			if (s != null) {
				return s.getTotalSpace();
			}
		} catch (Exception e) {
			log.error(path, e);
		}
		return 0;
	}

	public long reloadFree() {
		File f = this.getFile_obj();
		if (f.getFreeSpace() > 0) {
			return f.getFreeSpace();
		}

		try {
			FileStore s = this.getFilestore_obj();
			if (s != null) {
				return s.getUsableSpace();
			}
		} catch (Exception e) {
			log.error(path, e);
		}
		return 0;

	}

	// ---------------
	public long getUsed() {
//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).getUsed();
//		}

		return this.total - this.free;
	}

	public long reloadCount() {
		// scan files
		return IOUtil.count(this.getFile_obj());
	}

	public int getUsage() {
//		if (_disk != null) {
//			long total = _disk.get(TYPE_DATA).getTotal();
//			return (int) (_disk.get(TYPE_DATA).getUsed() * 100 / total);
//		}

		if (this.total > 0) {
			return (int) ((this.total - this.free) * 100 / this.total);
		}
		return 0;
	}

	public static DFile seek(Path filename) throws Exception {
//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).seek(filename);
//		}
		return seek(filename.toString());
	}

	public static DFile get(String id) throws Exception {
		return seek(new String(Base32.decode(id)));
	}

	public static DFile getByUrl(String url) throws Exception {
		if (url.startsWith("/f/g/") || url.startsWith("/f/d/")) {
			String[] ss = X.split(url, "/");
			if (ss.length > 2) {
				return seek(new String(Base32.decode(ss[2])));
			}
		}
		return seek(url);
	}

	public static DFile seek(String filename) throws Exception {

//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).seek(filename);
//		}

		if (Helper.isConfigured()) {
			filename = X.getCanonicalPath(filename);

			Beans<Disk> bs = disks(TYPE_DATA);

			for (Disk e : bs) {
				DFile d = MyDFile.create(e, filename);
				if (d.exists()) {
					return d;
				}
			}

			// log.debug("seek, not found, filename=" + filename, new Exception());
			DFile f = MyDFile.create(Disk._get(TYPE_DATA), filename);

			return f;
		}

		return null;

	}

	public static DFile seek(String filename, String type) throws Exception {

//		if (_disk != null) {
//			return _disk.get(type).seek(filename);
//		}

		if (Helper.isConfigured()) {

			filename = X.getCanonicalPath(filename);

			Beans<Disk> bs = disks(type);
			if (bs != null) {
				for (Disk e : bs) {
					DFile d = MyDFile.create(e, filename);
					if (d.exists()) {
						return d;
					}
				}
			}

			// log.debug("seek, not found, filename=" + filename, new Exception());
			DFile f = MyDFile.create(Disk._get(type), filename);

			return f;
		}

		return null;

	}

	public static boolean exists(String filename) throws Exception {

		Beans<Disk> bs = disks(TYPE_DATA);

		for (Disk e : bs) {
			DFile d = MyDFile.create(e, filename);
			if (d.exists()) {
				return true;
			}
		}

		return false;
	}

	private static Disk _get(String type) throws IOException {
		Selector s = Selector.get(type);
		return s.pick();
	}

	public static Collection<DFile> list(String filename) throws Exception {

//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).list(filename);
//		}

		Map<String, DFile> l1 = new TreeMap<String, DFile>();

		Beans<Disk> bs = disks(TYPE_ALL);

		for (Disk e : bs) {
			DFile f = MyDFile.create(e, filename);
			try {
				if (f.exists()) {
					DFile[] ff = f.listFiles();
					if (ff != null) {
						for (DFile f1 : ff) {
							String name = (f1.isDirectory() ? 0 : 1) + f1.getName();
							DFile f2 = l1.get(name);
							if (f2 == null || f1.lastModified() > f2.lastModified()) {
								l1.put(name, f1);
							}
						}
					}
					if (log.isDebugEnabled())
						log.debug("l1=" + l1);
				} else {
					log.info("exists? " + f.exists() + ", filename=" + f.getCanonicalPath() + ", path=" + e.path);
				}
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
		}

		return l1.values();
	}

	public static void delete(String filename, long age) throws Exception {
		delete(filename, age, true);
	}

	public static void delete(String filename) throws Exception {
		delete(filename, -1, true);
	}

	public static void delete(String filename, long age, boolean global) throws Exception {

//		if (_disk != null) {
//			_disk.get(TYPE_DATA).delete(filename, age, global);
//			return;
//		}

		Beans<Disk> bs = disks(TYPE_DATA);

		for (Disk e : bs) {

			DFile f = MyDFile.create(e, filename);

			f.delete(age);

		}

	}

	public static long move(DFile src, DFile dest) throws Exception {

//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).move(src, dest);
//		}

		long len = 0;
		if (src.isDirectory()) {
			// for
			dest.mkdirs();
			DFile[] ff = src.listFiles();
			if (ff != null) {
				for (DFile f : ff) {
					len += move(f, Disk.seek(dest.getFilename() + "/" + f.getName()));
				}
			}
			src.delete();
		} else {
			len = IOUtil.copy(src.getInputStream(), dest.getOutputStream());
			src.delete();
		}
		return len;
	}

	public static long copy(DFile src, DFile dest) throws Exception {

//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).copy(src, dest);
//		}

		long len = 0;

		if (src.isDirectory()) {
			dest.mkdirs();

			DFile[] ff = src.listFiles();
			if (ff != null) {
				for (DFile f : ff) {
					len += copy(f, Disk.seek(dest.getFilename() + "/" + f.getName()));
				}
			}
		} else {
			len = copy(src.getInputStream(), dest.getOutputStream());
		}
		return len;
	}

	public static long copy(InputStream in, OutputStream out) throws IOException {
		return IOUtil.copy(in, out);
	}

	private static Map<String, Beans<Disk>> _disks = new HashMap<String, Beans<Disk>>();

	private static Beans<Disk> disks(String type) throws Exception {

		Beans<Disk> d1 = _disks.get(type);

		if (X.isEmpty(d1) || System.currentTimeMillis() - d1.created > X.AMINUTE) {
			W q = W.create().and("enabled", 1).sort("priority", -1).sort("path", 1);
			if (type != TYPE_ALL) {
				q.and("type", type);
			}

			d1 = dao.load(q, 0, 100);
			_disks.put(type, d1);
		}

		if (X.isEmpty(d1)) {
			throw new Exception("not disk configured! [" + type + "]");
		}
		return d1;
	}

	public static void reset() {
		_disks.clear();
	}

	public String getNode() {
		return node;
	}

	private transient Node node_obj;

	public Node getNode_obj() {
		if (node_obj == null) {
			node_obj = Node.dao.load(node);
		}
		return node_obj;
	}

	public static void repair() {

		if (Helper.isConfigured()) {
			int s = 0;
			W q = W.create().sort("created", 1);
			Beans<Disk> bs = dao.load(q, s, 10);
			if (bs == null || bs.isEmpty()) {
				// add a default
				try {
					File f = new File(Controller.GIIWA_HOME + "/data");
					if (!f.exists()) {
						f.mkdirs();
					}
					Disk.create(V.create("path", f.getCanonicalPath()).append("type", "data").append("enabled", 1)
							.append("priority", 1).append("node", Local.id()));

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	public void check() {

		V v = V.create("checktime", System.currentTimeMillis());
		v.append("total", this.reloadTotal()).append("free", this.reloadFree());
		dao.update(id, v);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Disk other = (Disk) obj;
		if (id != other.id)
			return false;
		return true;
	}

	static class Selector {

		private long age = System.currentTimeMillis();
		private static Map<String, Selector> _inst = new HashMap<String, Selector>();

		TreeMap<Long, Disk> ss = new TreeMap<Long, Disk>();

		static synchronized Selector get(String type) {

			Selector d1 = _inst.get(type);

			if (d1 == null || System.currentTimeMillis() - d1.age > X.AMINUTE) {
				d1 = new Selector();

				W q = W.create().sort("priority", -1).sort("path", 1);
				if (type != Disk.TYPE_DATA) {
					q.and(W.create("type", null).or("type", type));
				}
				Beans<Disk> bs = dao.load(q, 0, 1000);
				for (Disk e : bs) {
					long f1 = e.free * e.priority;
					d1.add(e, f1);
				}
				_inst.put(type, d1);
			}

			return d1;
		}

		private void add(Disk d, long rate) {
			ss.put(rate, d);
		}

		Disk pick() throws IOException {

			if (!ss.isEmpty())
				return ss.lastEntry().getValue();

//			return DEFAULT;
			throw new IOException("no disk, ss=" + ss.size());
		}

	}

	/**
	 * @deprecated
	 * @return
	 */
	public static long getTotalSpace() {
//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).getTotal();
//		}

		long total = 0;
		if (Helper.isConfigured()) {
			int s = 0;
			W q = W.create().sort("created", 1);
			Beans<Disk> bs = dao.load(q, s, 10);
			while (bs != null && !bs.isEmpty()) {

				for (Disk e : bs) {
					total += e.total;
				}
				s += bs.size();
				bs = dao.load(q, s, 10);
			}
		}
		return total;
	}

	/**
	 * @deprecated
	 * @return
	 */
	public static long getFreeSpace() {

//		if (_disk != null) {
//			return _disk.get(TYPE_DATA).getTotal() - _disk.get(TYPE_DATA).getUsed();
//		}

		long total = 0;
		if (Helper.isConfigured()) {
			int s = 0;
			W q = W.create().sort("created", 1);
			Beans<Disk> bs = dao.load(q, s, 10);
			while (bs != null && !bs.isEmpty()) {

				for (Disk e : bs) {
					total += e.free;
				}
				s += bs.size();
				bs = dao.load(q, s, 10);

			}
		}
		return total;
	}

	public static void touch() {
		Beans<Disk> l1 = dao.load(W.create("node", Local.id()), 0, 100);
		l1.forEach(e -> {
			e.check();
		});
	}

}
