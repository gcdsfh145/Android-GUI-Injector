package io.github.reveny.injector.core.root;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.reveny.injector.core.InjectorData;
import io.github.reveny.injector.core.LogManager;
import io.github.reveny.injector.core.Native;

public class RootService extends com.topjohnwu.superuser.ipc.RootService implements Handler.Callback {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        LogManager.AddLog("RootService: onBind");

        Handler handler = new Handler(Looper.getMainLooper(), this);
        return new Messenger(handler).getBinder();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        LogManager.AddLog("RootService: handleMessage");

        if (msg.what != 1) {
            return false;
        }

        final InjectorData data = new InjectorData().fromMessage(msg);
        final Messenger replyTo = msg.replyTo;
        EXECUTOR.execute(() -> {
            int result = -1;
            String detail = "Injection failed";

            try {
                result = Native.Inject(data);
                detail = result == -1 ? "Native injector returned failure" : "Injection completed";
            } catch (Throwable throwable) {
                LogManager.AddLog("Native inject crashed: " + throwable.getMessage());
                detail = "Native inject crashed: " + throwable.getClass().getSimpleName();
            }

            String[] logs;
            try {
                logs = Native.GetNativeLogs();
            } catch (Throwable throwable) {
                LogManager.AddLog("Failed to read native logs: " + throwable.getMessage());
                logs = new String[0];
            }

            Message message = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putInt("result", result);
            bundle.putString("message", detail);
            bundle.putStringArray("logs", logs);
            message.setData(bundle);

            try {
                replyTo.send(message);
            } catch (Exception e) {
                LogManager.AddLog("Failed to send message: " + e.getMessage());
            }
        });

        return false;
    }
}
