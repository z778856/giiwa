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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_counter")
public class Counter extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Counter.class);

	public static final BeanDAO<String, Counter> dao = BeanDAO.create(Counter.class);

	@Column(name = X.ID, index = true)
	private String id;

	@Column(name = "node")
	private String node;

	@Column(name = "name")
	private String name;

	@Column(name = "count")
	private long count;

	public synchronized static long set(String name, long count) {
		try {
			String id = UID.id(Local.id(), name);
			if (dao.exists(id)) {
				dao.update(id, V.create("count", count));
			} else {
				dao.insert(V.create(X.ID, id).append("node", Local.id()).append("name", name).append("count", count));
			}
			return count;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public static void increase(String name) {
		String id = UID.id(Local.id(), name);
		dao.inc(W.create(X.ID, id), "count", 1, null);
	}

	public static void release(String name) {
		String id = UID.id(Local.id(), name);
		dao.inc(W.create(X.ID, id), "count", -1, null);
	}

}
