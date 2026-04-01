package io.github.reveny.injector.core.root;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import io.github.reveny.injector.core.InjectorData;
import io.github.reveny.injector.core.LogManager;

public class RootHandler implements Handler.Callback {
    public static RootHandler instance;

    // Root Connection
    public MessageConnection messageConnection;
    public Messenger remoteMessenger;
    public final Messenger replyMessenger = new Messenger(new Handler(Looper.getMainLooper(), this));
    private WeakReference<Activity> activityRef;
    private InjectorData pendingData;
    private InjectionCallback callback;

    public RootHandler() {
        instance = this;
    }

    public void Inject(Activity activity) {
        Inject(activity, InjectorData.instance, null);
    }

    public void Inject(Activity activity, InjectorData data, InjectionCallback callback) {
        activityRef = new WeakReference<>(activity);
        pendingData = data;
        this.callback = callback;

        if (data == null) {
            notifyFinished(false, "No injection data available", new String[0]);
            return;
        }

        if (remoteMessenger != null) {
            dispatchPendingInjection();
            return;
        }

        if (messageConnection == null) {
            RootService.bind(new Intent(activity, RootService.class), new MessageConnection(this));
        }
    }

    void dispatchPendingInjection() {
        if (remoteMessenger == null || pendingData == null) {
            return;
        }

        Message message = pendingData.toMessage(1);
        message.replyTo = replyMessenger;

        try {
            remoteMessenger.send(message);
            LogManager.AddLog("Injection request dispatched to RootService");
        } catch (Exception e) {
            LogManager.AddLog("Failed to send message: " + e.getMessage());
            remoteMessenger = null;
            notifyFinished(false, "Failed to communicate with RootService", new String[0]);
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        int result = msg.getData().getInt("result");
        String detail = msg.getData().getString("message", result == -1 ? "Injection failed" : "Injection succeeded");

        if (result == -1) {
            LogManager.AddLog("Injection failed: " + result);
        } else {
            LogManager.AddLog("Injection success: " + result);
        }

        // Add Log from native
        String[] logs = msg.getData().getStringArray("logs");
        if (logs == null) {
            LogManager.AddLog("Failed to get native Log");
            logs = new String[0];
        }

        LogManager.logs.addAll(Arrays.asList(logs));
        notifyFinished(result != -1, detail, logs);
        pendingData = null;

        return false;
    }

    private void notifyFinished(boolean success, String message, String[] logs) {
        if (callback != null) {
            callback.onFinished(success, message, logs);
            return;
        }

        Activity activity = activityRef != null ? activityRef.get() : null;
        if (activity != null) {
            LogManager.AddLog(message);
        }
    }

    public interface InjectionCallback {
        void onFinished(boolean success, String message, String[] logs);
    }
}
