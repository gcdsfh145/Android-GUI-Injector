package io.github.reveny.injector.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.github.reveny.injector.App;
import io.github.reveny.injector.R;
import io.github.reveny.injector.core.InjectorData;
import io.github.reveny.injector.core.LogManager;
import io.github.reveny.injector.core.root.RootHandler;
import io.github.reveny.injector.core.root.RootManager;
import io.github.reveny.injector.databinding.FragmentInjectionBinding;
import io.github.reveny.injector.core.Utility;

public class InjectionFragment extends BaseFragment {
    private FragmentInjectionBinding binding;
    private List<ApplicationInfo> appInfoList = new ArrayList<>();
    private List<String> appDisplayNames = new ArrayList<>();
    private PackageManager packageManager;

    public InjectorData injectorData = new InjectorData();

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 1 || resultCode != Activity.RESULT_OK) {
            return;
        }

        if (data == null || data.getData() == null) {
            Toast.makeText(getActivity(), "File selection failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // I don't think this is the best way of converting the URI to a path, but it works for now
        Uri fileUri = data.getData();
        String path = Objects.requireNonNull(fileUri.getPath()).replace("/document/primary:", Environment.getExternalStorageDirectory().getPath() + "/");

        if (path.endsWith(".so") || path.endsWith(".dex")) {
            Toast.makeText(getActivity(), "File Selected: " + path, Toast.LENGTH_LONG).show();

            injectorData.setLibraryPath(path);
            binding.libPath.setText(path);

            if (path.endsWith(".dex")) {
                binding.injectTabs.getTabAt(1).select();
            } else {
                binding.injectTabs.getTabAt(0).select();
            }
        } else {
            Toast.makeText(getActivity(), "Invalid file type selected. Please select a .so or .dex file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTabs() {
        binding.injectTabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int position = tab.getPosition();
                injectorData.setInjectType(position);
                if (position == 0) {
                    // SO Injection
                    binding.dexSettingsContainer.setVisibility(View.GONE);
                    binding.libPathChoose.setHint("Library Path (.so): ");
                    
                    // Show SO specific settings
                    binding.remapLibraryHolder.setVisibility(View.VISIBLE);
                    binding.proxyLibraryHolder.setVisibility(View.VISIBLE);
                    binding.randomizeProxyLibraryHolder.setVisibility(View.VISIBLE);
                    binding.hideLibraryHolder.setVisibility(View.VISIBLE);
                } else {
                    // DEX Injection
                    binding.dexSettingsContainer.setVisibility(View.VISIBLE);
                    binding.libPathChoose.setHint("DEX Path (.dex): ");

                    // Hide SO specific settings
                    binding.remapLibraryHolder.setVisibility(View.GONE);
                    binding.proxyLibraryHolder.setVisibility(View.GONE);
                    binding.randomizeProxyLibraryHolder.setVisibility(View.GONE);
                    binding.hideLibraryHolder.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInjectionBinding.inflate(inflater, container, false);

        // 初始化包管理器
        packageManager = requireContext().getPackageManager();
        
        setupToolbar();
        setupApplist();
        setupTabs();
        setupAutoLaunch();
        setupSettings();

        binding.libPathChoose.setEndIconOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("*/*");

            // For .so and .dex files
            chooseFile.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream"});
            chooseFile = Intent.createChooser(chooseFile, "Choose a .so or .dex file");
            startActivityForResult(chooseFile, 1);
        });

        binding.injectionFab.setOnClickListener(v -> {
            addTextWatcher(binding.appSelector);
            addTextWatcher(binding.libPathChoose);
            if (validateInputs(binding)) {
                startInjection();
            }
        });
        return binding.getRoot();
    }

    private void setupToolbar() {
        setupToolbar(binding.toolbar, binding.clickView, R.string.injection);
        binding.toolbar.setNavigationIcon(null);
        binding.appBar.setLiftable(true);
        binding.nestedScrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setLifted(!top));
    }

    private void setupApplist() {
        // 获取所有应用信息
        loadInstalledApps();
        
        // 创建自定义适配器，支持搜索过滤
        AppListAdapter adapter = new AppListAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            appDisplayNames,
            appInfoList
        );
        
        binding.appSelectorText.setAdapter(adapter);
        binding.appSelectorText.setThreshold(1); // 输入1个字符后开始搜索

        binding.appSelectorText.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDisplayName = (String) parent.getItemAtPosition(position);
            
            // 找到对应的应用信息
            ApplicationInfo selectedApp = null;
            for (int i = 0; i < appDisplayNames.size(); i++) {
                if (appDisplayNames.get(i).equals(selectedDisplayName)) {
                    selectedApp = appInfoList.get(i);
                    break;
                }
            }
            
            if (selectedApp != null) {
                String packageName = selectedApp.packageName;
                String appName = selectedApp.loadLabel(packageManager).toString();
                
                injectorData.setPackageName(packageName);
                injectorData.setLauncherActivity(Utility.getLaunchActivity(requireContext(), packageName));

                String processID = RootManager.instance.getPid(packageName);
                binding.killProc.setText(String.format("Kill before Auto Launch [PID: %s]", processID));

                LogManager.AddLog("Selected: " + appName + " (" + packageName + ")");
            }
        });
    }

    // 加载已安装的应用
    private void loadInstalledApps() {
        appInfoList.clear();
        appDisplayNames.clear();
        
        // 获取所有非系统应用
        List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        
        // 按包名进行排序
        Collections.sort(packages, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return a.packageName.compareToIgnoreCase(b.packageName);
            }
        });
        
        for (ApplicationInfo packageInfo : packages) {
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                appInfoList.add(packageInfo);
                String appName = packageInfo.loadLabel(packageManager).toString();
                appDisplayNames.add(appName + " (" + packageInfo.packageName + ")");
            }
        }
    }

    // 自定义适配器类，支持搜索过滤
    private class AppListAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> originalData;
        private List<ApplicationInfo> originalAppInfo;
        private List<String> filteredData;
        private List<ApplicationInfo> filteredAppInfo;

        public AppListAdapter(@NonNull Context context, int resource, 
                             @NonNull List<String> objects, 
                             @NonNull List<ApplicationInfo> appInfoList) {
            super(context, resource, objects);
            this.originalData = new ArrayList<>(objects);
            this.originalAppInfo = new ArrayList<>(appInfoList);
            this.filteredData = new ArrayList<>(objects);
            this.filteredAppInfo = new ArrayList<>(appInfoList);
        }

        @Override
        public int getCount() {
            return filteredData.size();
        }

        @Override
        public String getItem(int position) {
            return filteredData.get(position);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<String> filteredList = new ArrayList<>();
                    List<ApplicationInfo> filteredAppList = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        filteredList.addAll(originalData);
                        filteredAppList.addAll(originalAppInfo);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        
                        for (int i = 0; i < originalData.size(); i++) {
                            String appInfo = originalData.get(i);
                            if (appInfo.toLowerCase().contains(filterPattern)) {
                                filteredList.add(appInfo);
                                filteredAppList.add(originalAppInfo.get(i));
                            }
                        }
                    }

                    results.values = filteredList;
                    results.count = filteredList.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredData.clear();
                    filteredAppInfo.clear();
                    
                    if (results.values != null) {
                        filteredData.addAll((List<String>) results.values);
                        for (int i = 0; i < filteredData.size(); i++) {
                            // 找到对应的应用信息
                            String displayName = filteredData.get(i);
                            for (int j = 0; j < originalData.size(); j++) {
                                if (originalData.get(j).equals(displayName)) {
                                    filteredAppInfo.add(originalAppInfo.get(j));
                                    break;
                                }
                            }
                        }
                    }
                    
                    notifyDataSetChanged();
                }
            };
        }
    }

    private void setupAutoLaunch() {
        binding.autoLaunch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            injectorData.setShouldAutoLaunch(isChecked);
            LogManager.AddLog("Auto Launch: " + isChecked);
        });

        binding.killProc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            injectorData.setShouldKillBeforeLaunch(isChecked);
            LogManager.AddLog("Kill Process: " + isChecked);
        });
    }

    private void setupSettings() {
        binding.injectZygote.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.enableProxy.setChecked(false);
                binding.autoLaunch.setChecked(true);
            }

            if (Utility.isEmulator()) {
                Toast.makeText(requireContext(), "Emulator detected, zygote injection is not supported here yet!", Toast.LENGTH_LONG).show();
                if (isChecked) binding.bypassRestrictions.setChecked(false);
                return;
            }

            injectorData.setInjectZygote(isChecked);
            LogManager.AddLog("Inject Zygote: " + isChecked);
        });

        binding.remapLib.setOnCheckedChangeListener((buttonView, isChecked) -> {
            injectorData.setRemapLibrary(isChecked);
            LogManager.AddLog("Remap Library: " + isChecked);
        });

        binding.enableProxy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            injectorData.setUseProxy(isChecked);
            LogManager.AddLog("Use Proxy: " + isChecked);
        });

        binding.enableProxyRandomize.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) binding.enableProxy.setChecked(true);

            injectorData.setRandomizeProxyName(isChecked);
            LogManager.AddLog("Randomize Proxy: " + isChecked);
        });

        binding.hideLibrary.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) binding.enableProxy.setChecked(true);

            injectorData.setHideLibrary(isChecked);
            LogManager.AddLog("Hide Library: " + isChecked);
        });

        binding.bypassRestrictions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Utility.isEmulator()) {
                Toast.makeText(requireContext(), "Emulator detected, bypassing restrictions does not work here!", Toast.LENGTH_LONG).show();
                if (isChecked) binding.bypassRestrictions.setChecked(false);
                return;
            }

            injectorData.setBypassNamespaceRestrictions(isChecked);
            LogManager.AddLog("Bypass Restrictions: " + isChecked);
        });
    }

    private void startInjection() {
        if (injectorData.getInjectType() == 1) {
            injectorData.setDexClassName(binding.dexClassName.getText().toString());
            injectorData.setDexMethodName(binding.dexMethodName.getText().toString());
        }

        LogManager.AddLog("Starting Injection...");
        LogManager.AddLog("Injection Data: " + injectorData.toString());

        // Make sure we have root access
        if (!RootManager.instance.hasRootAccess) {
            Toast.makeText(requireContext(), "Root Access not granted, please restart the app", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start the injection process
        RootHandler rootHandler = new RootHandler();
        rootHandler.Inject(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private boolean validateInputs(FragmentInjectionBinding injectionBinding) {
        boolean isValid = validateAndSetError(injectionBinding.appSelector, "Please select a target package");
        isValid &= validateAndSetError(injectionBinding.libPathChoose, "Please select a library path");
        return isValid;
    }
}