package com.uangel.ccaas.msggw;

import com.uangel.ccaas.msggw.service.AppInstance;
import com.uangel.ccaas.msggw.service.ServiceManager;

import java.io.IOException;
import java.nio.file.Path;

public class MsgGwMain {

    public static void main(String[] args) throws IOException, NoSuchFieldException {
        AppInstance.getInstance().setConfigPath(Path.of(args[0]));
        ServiceManager.getInstance().loop();
    }
}