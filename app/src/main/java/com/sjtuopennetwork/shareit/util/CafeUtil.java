package com.sjtuopennetwork.shareit.util;

import sjtu.opennet.hon.Handlers;
import sjtu.opennet.hon.Textile;
import sjtu.opennet.textilepb.Model;

public class CafeUtil {

    private static Boolean stopConnect=false;

    public static void connectCafe(Handlers.ErrorHandler handler){
        new Thread(){
            @Override
            public void run() {
                while(!stopConnect){
                    Textile.instance().cafes.register(
//                    "http://159.138.58.61:40601",
                            "http://202.120.38.131:40601",
//                    "http://192.168.1.109:40601",
//                    "http://202.120.40.60:40601"
                            "K6ayNanfZcfvGDBbxmf9DHkeyv4osxoGPpMGNP3vX4DQTnraUugY4h51T6DD", //131
                            handler);
                    try {
                        Thread.sleep(300000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public static void stopConnect(Handlers.ErrorHandler handler){
        new Thread(){
            @Override
            public void run() {
                stopConnect=true;
                try {
                    Model.CafeSessionList cafeSessionList = Textile.instance().cafes.sessions();
                    for(Model.CafeSession c:cafeSessionList.getItemsList()){
                        Textile.instance().cafes.deregister(c.getId(), handler);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
