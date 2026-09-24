package com.vidlix.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                break;
            case PackageInstaller.STATUS_SUCCESS:
                break;
            case PackageInstaller.STATUS_FAILURE:
                break;
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                break;
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                break;
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                break;
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                break;
            case PackageInstaller.STATUS_FAILURE_INVALID:
                break;
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                break;
            default:
                break;
        }
    }
}
