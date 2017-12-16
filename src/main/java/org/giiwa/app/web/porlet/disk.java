package org.giiwa.app.web.porlet;

import org.giiwa.core.base.Host;

public class disk extends porlet {

	@Override
	public void get() {
		try {
			this.set("disk", Host.getDisks());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		this.show("/porlet/disk.html");
	}

	@Override
	public void setup() {
	}

	public String eclipse(String path) {
		return path.replaceAll("\\\\", "/");
	}

}
