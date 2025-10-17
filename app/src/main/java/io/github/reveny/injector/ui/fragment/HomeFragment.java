package io.github.reveny.injector.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import io.github.reveny.injector.App;
import io.github.reveny.injector.BuildConfig;
import io.github.reveny.injector.R;
import io.github.reveny.injector.core.Utility;
import io.github.reveny.injector.databinding.DialogAboutBinding;
import io.github.reveny.injector.databinding.FragmentHomeBinding;
import io.github.reveny.injector.ui.activity.SettingsActivity;
import io.github.reveny.injector.ui.dialog.BlurBehindDialogBuilder;
import io.github.reveny.injector.util.chrome.LinkTransformationMethod;
import rikka.material.app.LocaleDelegate;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private final SharedPreferences pref = App.getPreferences();

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupToolbar();
        setupDeviceInfo();
        return binding.getRoot();
    }

    private void setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_home);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.menu_settings) {
                startActivity(new Intent(requireActivity(), SettingsActivity.class));
                return true;
            } else if (id == R.id.menu_about) {
                showAbout();
                return true;
            }
            
            return false;
        });
    }

    @SuppressLint("DefaultLocale")
    private void setupDeviceInfo() {
        binding.systemVersion.setText(String.format("Android %s (API %d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        binding.device.setText(String.format("%s %s", Build.BRAND, Build.MODEL));
        binding.systemAbi.setText(getPrimaryAbi());
        
        binding.isRooted.setText(Utility.isRooted() ? "Yes" : "No");
        
        String[] packageNames = {
            "io.github.huskydg.magisk", 
            "me.weishu.kernelsu", 
            "me.bmax.apatch", 
            "io.github.vvb2060.magisk", 
            "com.topjohnwu.magisk", 
            "com.sukisu.ultra"
        };
        binding.rootSystem.setText(checkRootSolution(requireContext(), packageNames));
        
        new Thread(() -> {
            String zygiskStatus = checkZygiskStatusWithRoot();
            requireActivity().runOnUiThread(() -> binding.zygiskStatus.setText(zygiskStatus));
        }).start();
        
        binding.isEmulator.setText(Utility.isEmulator() ? "Yes" : "No");
        
        new Thread(() -> {
            String securityStatus = checkSecurityStatusWithRoot();
            requireActivity().runOnUiThread(() -> binding.securityStatus.setText(securityStatus));
        }).start();
    }

    private String getPrimaryAbi() {
        if (Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS[0];
        }
        return "Unknown";
    }

    private String checkZygiskStatusWithRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su -c ls /data/adb/modules");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder modules = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                modules.append(line).append("\n");
            }
            process.waitFor();
            
            String modulesList = modules.toString();
            if (modulesList.contains("zygisksu") || modulesList.contains("zygisk")) {
                return "Zygisk Detected";
            }
            
        } catch (Exception e) {
            return checkZygiskStatusFallback();
        }
        
        return "Not detected";
    }

    private String checkZygiskStatusFallback() {
        File modulesDir = new File("/data/adb/modules");
        if (modulesDir.exists() && modulesDir.isDirectory()) {
            File[] modules = modulesDir.listFiles();
            if (modules != null) {
                for (File module : modules) {
                    if (module.getName().contains("zygisk")) {
                        return "Zygisk (" + module.getName() + ")";
                    }
                }
            }
        }
        return "Not detected";
    }

    private String checkSecurityStatusWithRoot() {
        try {
            Process process = Runtime.getRuntime().exec("su -c getenforce");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String selinuxStatus = reader.readLine();
            process.waitFor();
            
            if (selinuxStatus != null && !selinuxStatus.trim().isEmpty()) {
                return "SELinux: " + selinuxStatus.trim();
            }
            
        } catch (Exception e) {
        }
        
        return checkSecurityStatusFallback();
    }

    private String checkSecurityStatusFallback() {
        String selinuxStatus = getSELinuxStatus();
        return "SELinux: " + selinuxStatus;
    }

    public String checkRootSolution(Context context, String[] packageNames) {
        PackageManager packageManager = context.getPackageManager();
        
        for (String packageName : packageNames) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                if (packageInfo != null) {
                    String appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
                    return appName;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        String[] magiskPaths = {
            "/sbin/magisk",
            "/system/bin/magisk",
            "/system/xbin/magisk",
            "/data/adb/magisk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su"
        };

        for (String path : magiskPaths) {
            if (new File(path).exists()) {
                return "Magisk";
            }
        }

        return "Not detected";
    }

    private String getSELinuxStatus() {
        try {
            Process process = Runtime.getRuntime().exec("getenforce");
            process.waitFor();
            java.io.InputStream inputStream = process.getInputStream();
            java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
            String result = scanner.hasNext() ? scanner.next().trim() : "";
            
            if (!result.isEmpty() && (result.equals("Enforcing") || result.equals("Permissive") || result.equals("Disabled"))) {
                return result;
            }
            
            return "Unknown";
            
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static class AboutDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            DialogAboutBinding binding = DialogAboutBinding.inflate(getLayoutInflater(), null, false);
            binding.designAboutTitle.setText(R.string.app_name);
            binding.designAboutInfo.setMovementMethod(LinkMovementMethod.getInstance());
            binding.designAboutInfo.setTransformationMethod(new LinkTransformationMethod(requireActivity()));
            binding.designAboutInfo.setText(HtmlCompat.fromHtml(getString(
                    R.string.about_view_source_code,
                    "<b><a href=\"https://github.com/reveny\">GitHub</a></b>",
                    "<b><a href=\"https://t.me/reveny1\">Telegram</a></b>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
            binding.designAboutVersion.setText(String.format(LocaleDelegate.getDefaultLocale(), "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
            return new BlurBehindDialogBuilder(requireContext())
                    .setView(binding.getRoot()).create();
        }
    }

    private void showAbout() {
        new AboutDialog().show(getChildFragmentManager(), "about");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}