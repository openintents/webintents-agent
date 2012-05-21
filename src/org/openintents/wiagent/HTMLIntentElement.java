package org.openintents.wiagent;

/**
 * This is a Java class corresponding to interface HTMLIntentElement in the spec
 * @author Cheng Zheng
 *
 */
public class HTMLIntentElement {
	
	public static final String ATT_ACTION = "action";
	public static final String ATT_TYPE = "type";
	public static final String ATT_HERF = "href";
	public static final String ATT_TITLE = "title";
	public static final String ATT_DISPOSITION = "disposition";
	
	private final String action;
	private final String type;
	private final String href;
	private final String title;
	private final String disposition;
	
	static final String INLINE = "inline";
	
	public HTMLIntentElement(String action, String type, String href,
			String title, String disposition) {
		super();
		this.action = action;
		this.type = type;
		this.href = href;
		this.title = title;
		this.disposition = disposition != null ? disposition : INLINE;
	}

	public String getAction() {
		return action;
	}

	public String getType() {
		return type;
	}

	public String getHref() {
		return href;
	}

	public String getTitle() {
		return title;
	}

	public String getDisposition() {
		return disposition;
	}
}
