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
<<<<<<< HEAD
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
=======
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
>>>>>>> 7eb855b (Detecting zygisk)

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
<<<<<<< HEAD
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;

import java.io.File;
=======
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
>>>>>>> 7eb855b (Detecting zygisk)

import io.github.reveny.injector.App;
import io.github.reveny.injector.BuildConfig;
import io.github.reveny.injector.R;
<<<<<<< HEAD
=======
import io.github.reveny.injector.core.Utility;
>>>>>>> 7eb855b (Detecting zygisk)
import io.github.reveny.injector.databinding.DialogAboutBinding;
import io.github.reveny.injector.databinding.FragmentHomeBinding;
import io.github.reveny.injector.ui.activity.SettingsActivity;
import io.github.reveny.injector.ui.dialog.BlurBehindDialogBuilder;
<<<<<<< HEAD
import io.github.reveny.injector.core.Utility;
import io.github.reveny.injector.util.chrome.LinkTransformationMethod;
import rikka.material.app.LocaleDelegate;

public class HomeFragment extends BaseFragment implements MenuProvider {
    private FragmentHomeBinding binding;
    private final SharedPreferences pref = App.getPreferences();

    @Override
    public void onPrepareMenu(Menu menu) {
        menu.findItem(R.id.menu_settings).setOnMenuItemClickListener(v -> {
            startActivity(new Intent(requireActivity(), SettingsActivity.class));
            return true;
        });
        menu.findItem(R.id.menu_about).setOnMenuItemClickListener(v -> {
            showAbout();
            return true;
        });
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

=======
import io.github.reveny.injector.util.chrome.LinkTransformationMethod;
import rikka.material.app.LocaleDelegate;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private final SharedPreferences pref = App.getPreferences();

>>>>>>> 7eb855b (Detecting zygisk)
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setupToolbar();
        setupDeviceInfo();
        return binding.getRoot();
    }

    private void setupToolbar() {
<<<<<<< HEAD
        setupToolbar(binding.toolbar, null, R.string.app_name, R.menu.menu_home);
        binding.toolbar.setNavigationIcon(null);
        binding.appBar.setLiftable(true);
        binding.nestedScrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));
=======
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
>>>>>>> 7eb855b (Detecting zygisk)
    }

    @SuppressLint("DefaultLocale")
    private void setupDeviceInfo() {
<<<<<<< HEAD
        binding.systemVersion.setText(String.format("Android %s (API %d)", android.os.Build.VERSION.RELEASE, android.os.Build.VERSION.SDK_INT));
        binding.device.setText(String.format("%s %s", Build.BRAND, Build.MODEL));
        binding.systemAbi.setText(android.os.Build.SUPPORTED_ABIS[0]);
        binding.isRooted.setText(Utility.isRooted() ? "Yes" : "No");

        String[] packageNames = {"io.github.huskydg.magisk", "me.weishu.kernelsu", "me.bmax.apatch", "io.github.vvb2060.magisk", "com.topjohnwu.magisk", "com.sukisu.ultra"};
        binding.rootSystem.setText(checkRootSolution(requireContext(), packageNames));
        binding.isEmulator.setText(Utility.isEmulator() ? "Yes" : "No");
=======
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
>>>>>>> 7eb855b (Detecting zygisk)
    }

    public String checkRootSolution(Context context, String[] packageNames) {
        PackageManager packageManager = context.getPackageManager();
<<<<<<< HEAD
=======
        
>>>>>>> 7eb855b (Detecting zygisk)
        for (String packageName : packageNames) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
                if (packageInfo != null) {
<<<<<<< HEAD
                    return packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Continue checking the next package name
            }
        }

        // If not detected, it's likely that it runs magisk with a spoofed package name
        // We can just check for magisk binary here.
        // TODO: Need to check if the path is still reliable
        if (new File("/sbin/magiskpolicy").exists()) {
            return "Magisk";
=======
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
>>>>>>> 7eb855b (Detecting zygisk)
        }

        return "Not detected";
    }

<<<<<<< HEAD
=======
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

>>>>>>> 7eb855b (Detecting zygisk)
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