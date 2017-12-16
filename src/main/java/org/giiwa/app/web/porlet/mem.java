package org.giiwa.app.web.porlet;

import org.giiwa.core.base.Host;

public class mem extends porlet {

	@Override
	public void get() {
		try {
			this.set("mem", Host.getMem());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		this.show("/porlet/mem.html");
	}

	@Override
	public void setup() {
	}

}
