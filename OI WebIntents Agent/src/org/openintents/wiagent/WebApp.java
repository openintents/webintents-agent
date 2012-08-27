package org.openintents.wiagent;

/**
 * This is a Web application wrapper class
 * @author Cheng Zheng
 *
 */
public class WebApp {

	/**
	 * The title of the Web application
	 */
	public String title;

	/**
	 * The URI of the Web application, must be unique, works as id
	 */
	public String href;

	public WebApp() { }

	public WebApp(String title, String href) {
		this.title = title;
		this.href = href;
	}

	/**
	 * Two WebApp instances are considered equal if their URIs are the same
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof WebApp) {
			return href.equals(((WebApp) o).href);
		} else {
			return false;
		}
	}
}
