package cc.uncarbon.test;

import cc.uncarbon.module.sys.util.PwdUtil;

public class GenEncryptPassword {

    static void main() {
        String out = PwdUtil.hash("admin");
        System.out.println(out);
    }
}
