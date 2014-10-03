package com.fuyo.mde;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import android.content.SharedPreferences;

public class CustomUncaughtExceptionHandler implements UncaughtExceptionHandler {
	 
    private Context mContext;
    private UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;
 
    public CustomUncaughtExceptionHandler(Context context) {
        mContext = context;
 
        // デフォルト例外ハンドラを保持する。
        mDefaultUncaughtExceptionHandler = Thread
                .getDefaultUncaughtExceptionHandler();
    }
 
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // スタックトレースを文字列にします。
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();
 
        // スタックトレースを SharedPreferences に保存します。
        SharedPreferences preferences = mContext.getSharedPreferences(
                ItemListActivity.PREF_NAME_SAMPLE, Context.MODE_PRIVATE);
        preferences.edit().putString(ItemListActivity.EX_STACK_TRACE, stackTrace)
                .commit();
 
        // デフォルト例外ハンドラを実行し、強制終了します。
        mDefaultUncaughtExceptionHandler.uncaughtException(thread, ex);
    }
}