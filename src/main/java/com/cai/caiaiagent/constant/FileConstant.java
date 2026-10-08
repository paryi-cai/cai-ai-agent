package com.cai.caiaiagent.constant;

/**
 * 文件保存常量
 * 所有工具产生的文件统一存放到隔离目录（项目根目录 /tmp），避免影响系统资源
 */
public interface FileConstant {

    /**
     * 文件保存目录（项目根目录下的 tmp，已加入 .gitignore）
     */
    String FILE_SAVE_DIR = System.getProperty("user.dir") + "/tmp";
}
