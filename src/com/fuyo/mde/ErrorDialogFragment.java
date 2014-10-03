package com.fuyo.mde;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ErrorDialogFragment extends DialogFragment {
	 
    private String mExStackTrace;
 
    public ErrorDialogFragment(String exStackTrace) {
        mExStackTrace = exStackTrace;
    }
 
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("前回強制終了したときのエラー情報を送信します。\nよろしいですか？");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + "riahruoy.dev+matome@gmail.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "不具合の報告");
                intent.putExtra(Intent.EXTRA_TEXT, mExStackTrace);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("キャンセル", null);
        return builder.create();
    }
}