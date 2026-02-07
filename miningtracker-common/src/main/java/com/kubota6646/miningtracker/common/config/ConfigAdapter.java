package com.kubota6646.miningtracker.common.config;

/**
 * 設定アダプターインターフェース
 * BukkitとBungeecordの設定を統一的に扱うための抽象化
 */
public interface ConfigAdapter {
    
    /**
     * 文字列値を取得
     */
    String getString(String path, String defaultValue);
    
    /**
     * 整数値を取得
     */
    int getInt(String path, int defaultValue);
    
    /**
     * 長整数値を取得
     */
    long getLong(String path, long defaultValue);
}
