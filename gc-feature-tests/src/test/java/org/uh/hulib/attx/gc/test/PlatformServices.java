/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.hulib.attx.gc.test;

/**
 *
 * @author jkesanie
 */
public class PlatformServices {

    private final String ESSIREN = "http://essiren";
    private final int ESSIREN_PORT = 9200;

    private final String ES5 = "http://es5";
    private final int ES5_PORT = 9210;

    private final String FUSEKI = "http://fuseki";
    private final int FUSEKI_PORT = 3030;

    private final String GMAPI = "http://gmapi";
    private final int GMAPI_PORT = 4302;


    private final String UV = "http://frontend";
    private final int UV_PORT = 8080;

    private final String WFAPI = "http://wfapi";
    private final int WFAPI_PORT = 4301;

    private boolean isLocalhost = false;

    public PlatformServices() {
    }

    public PlatformServices(boolean isLocalhost) {
        this.isLocalhost = isLocalhost;
    }

    public String getESSiren() {
        return "http://" + System.getProperty("essiren.host") + ":" + Integer.parseInt(System.getProperty("essiren.port"));
//        if (isLocalhost) {
//            return "http://localhost:" + ESSIREN_PORT;
//        } else {
//            return ESSIREN + ":" + ESSIREN_PORT;
//        }
    }

    public String getES5() {
        return "http://" + System.getProperty("es5.host") + ":" + Integer.parseInt(System.getProperty("es5.port"));
//        if (isLocalhost) {
//            return "http://localhost:" + ES5_PORT;
//        } else {
//            return ES5 + ":" + ES5_PORT;
//        }
    }

    public String getFuseki() {
        return "http://" + System.getProperty("fuseki.host") + ":" + Integer.parseInt(System.getProperty("fuseki.port"));
//        if (isLocalhost) {
//            return "http://localhost:" + FUSEKI_PORT;
//        } else {
//            return FUSEKI + ":" + FUSEKI_PORT;
//        }
    }

    public String getUV() {
        return "http://" + System.getProperty("frontend.host") + ":" + Integer.parseInt(System.getProperty("frontend.port"));
//        if (isLocalhost) {
//            return "http://localhost:" + UV_PORT;
//        } else {
//            return UV + ":" + UV_PORT;
//        }
    }

    public String getGmapi() {
        return "http://" + System.getProperty("gmapi.host") + ":" + Integer.parseInt(System.getProperty("gmapi.port"));
//        if (isLocalhost) {
//            return "http://localhost:" + GMAPI_PORT;
//        } else {
//            return GMAPI + ":" + GMAPI_PORT;
//        }
    }

    public String getWfapi() {
        return "http://" + System.getProperty("wfapi.host") + ":" + Integer.parseInt(System.getProperty("wfapi.port"));
//        if (isLocalhost) {
//            return "http://localhost:" + WFAPI_PORT;
//        } else {
//            return WFAPI + ":" + WFAPI_PORT;
//        }
    }
}
