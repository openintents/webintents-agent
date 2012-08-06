package org.openintents.wiagent;

/**
 * This is a Web application wrapper class
 * @author Cheng Zheng
 *
 */
public class WebApp {
    
    public String title;
    public String href;
    
    public WebApp() {
        super();
    }

    public WebApp(String title, String href) {
        this.title = title;
        this.href = href;
    }
}
