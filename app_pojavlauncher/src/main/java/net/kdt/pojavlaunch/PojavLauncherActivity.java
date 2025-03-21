package net.kdt.pojavlaunch;

import static android.os.Build.VERSION_CODES.P;
import static net.kdt.pojavlaunch.Tools.ignoreNotch;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_HIDE_SIDEBAR;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_NOTCH_SIZE;

import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.fragments.*;
import net.kdt.pojavlaunch.modmanager.ModManager;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PojavLauncherActivity extends BaseLauncherActivity
{

    // An equivalent ViewPager2 adapter class
    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {

        private final PojavLauncherActivity fa;

        public ScreenSlidePagerAdapter(PojavLauncherActivity fa) {
            super(fa);
            this.fa = fa;
        }

        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new LauncherFragment();
            if (position == 1) return new ConsoleFragment();
            if (position == 2) return new CrashFragment();
            if (position == 3) return new ModsFragment(fa);
            if (position == 4) return new LauncherPreferenceFragment();
            return null;
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }


    private TextView tvConnectStatus;
    private Spinner accountSelector;
    private ViewPager2 viewPager;
    private final Button[] Tabs = new Button[5];
    private View selectedTab;
    private ImageView accountFaceImageView;

    private Button logoutBtn; // MineButtons
    private ExtraListener backPreferenceListener;

    public PojavLauncherActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojav_launcher);

        //Boilerplate linking/initialisation
        viewPager = findViewById(R.id.launchermainTabPager);
        selectedTab = findViewById(R.id.viewTabSelected);
        tvConnectStatus = findViewById(R.id.launchermain_text_accountstatus);
        accountFaceImageView = findViewById(R.id.launchermain_account_image);
        accountSelector = findViewById(R.id.launchermain_spinner_account);
        mVersionSelector = findViewById(R.id.launchermain_spinner_version);
        mLaunchProgress = findViewById(R.id.progressDownloadBar);
        mLaunchTextStatus = findViewById(R.id.progressDownloadText);
        logoutBtn = findViewById(R.id.installJarButton);
        mPlayButton = findViewById(R.id.launchermainPlayButton);
        mAddInstanceButton = findViewById(R.id.add_instance_button);
        Tabs[0] = findViewById(R.id.btnTab1);
        Tabs[1] = findViewById(R.id.btnTab2);
        Tabs[2] = findViewById(R.id.btnTab3);
        Tabs[3] = findViewById(R.id.btnTab4);
        Tabs[4] = findViewById(R.id.btnTab5);

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "Launcher process id: " + android.os.Process.myPid(), Toast.LENGTH_LONG).show();
        }

        // Setup the viewPager to slide across fragments
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setTabActive(position);
            }
        });
        initTabs(0);

        //Setup listener to the backPreference system
        backPreferenceListener = (key, value) -> {
            if(value.equals("true")){
                onBackPressed();
                ExtraCore.setValue(key, "false");
            }
            return false;
        };
        ExtraCore.addExtraListener("back_preference", backPreferenceListener);


        // Try to load the temporary account
        final List<String> accountList = new ArrayList<>();
        final MinecraftAccount tempProfile = PojavProfile.getTempProfileContent();
        if (tempProfile != null) {
            accountList.add(tempProfile.username);
        }
        for (String s : new File(Tools.DIR_ACCOUNT_NEW).list()) {
            accountList.add(s.substring(0, s.length() - 5));
        }

        // Setup account spinner
        pickAccount();
        ArrayAdapter<String> adapterAcc = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, accountList);
        adapterAcc.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        accountSelector.setAdapter(adapterAcc);

        if (tempProfile != null) {
            accountSelector.setSelection(0);
        } else {
            for (int i = 0; i < accountList.size(); i++) {
                String account = accountList.get(i);
                if (account.equals(mProfile.username)) {
                    accountSelector.setSelection(i);
                }
            }
        }

        accountSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4) {
                if (tempProfile != null && position == 0) {
                    PojavProfile.setCurrentProfile(PojavLauncherActivity.this, tempProfile);
                } else {
                    PojavProfile.setCurrentProfile(PojavLauncherActivity.this,
                        accountList.get(position + (tempProfile != null ? 1 : 0)));
                }
                pickAccount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p1) {
                // TODO: Implement this method
            }
        });

        // Setup the minecraft version list
        /*List<String> versions = new ArrayList<>();
        final File fVers = new File(Tools.DIR_HOME_VERSION);

        try {
            if (fVers.listFiles().length < 1) {
                throw new Exception(getString(R.string.error_no_version));
            }

            for (File fVer : fVers.listFiles()) {
                if (fVer.isDirectory())
                    versions.add(fVer.getName());
            }
        } catch (Exception e) {
            versions.add(getString(R.string.global_error) + ":");
            versions.add(e.getMessage());

        } finally {
            mAvailableVersions = versions.toArray(new String[0]);
        }*/

        //mAvailableVersions;
        //ArrayAdapter<String> adapterVer = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        //adapterVer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        //mVersionSelector.setAdapter(adapterVer);


        ModManager.init(this);


        statusIsLaunching(false);



        //Add the preference changed listener
        LauncherPreferences.DEFAULT_PREF.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if(key.equals("hideSidebar")){
                changeLookAndFeel(sharedPreferences.getBoolean("hideSidebar",false));
                return;
            }

            if(key.equals("ignoreNotch")){
                ignoreNotch(sharedPreferences.getBoolean("ignoreNotch", true), PojavLauncherActivity.this);
                return;
            }
        });
        changeLookAndFeel(PREF_HIDE_SIDEBAR);
    }

    public void selectTabPage(int pageIndex){
        viewPager.setCurrentItem(pageIndex);
        setTabActive(pageIndex);
    }

    private void pickAccount() {
        try {
            mProfile = PojavProfile.getCurrentProfileContent(this);
            accountFaceImageView.setImageBitmap(mProfile.getSkinFace());

            //TODO FULL BACKGROUND LOGIN
            tvConnectStatus.setText(mProfile.accessToken.equals("0") ? R.string.mcl_account_local : R.string.mcl_account_connected);
        } catch(Exception e) {
            mProfile = new MinecraftAccount();
            Tools.showError(this, e, true);
        }
    }

    public void statusIsLaunching(boolean isLaunching) {
        int launchVisibility = isLaunching ? View.VISIBLE : View.GONE;
        mLaunchProgress.setVisibility(launchVisibility);
        mLaunchTextStatus.setVisibility(launchVisibility);

        logoutBtn.setEnabled(!isLaunching);
        mVersionSelector.setEnabled(!isLaunching);
        canBack = !isLaunching;
    }

    public void onTabClicked(View view) {
        for(int i=0; i<Tabs.length;i++){
            if(view.getId() == Tabs[i].getId()) {
                selectTabPage(i);
                return;
            }
        }
    }

    public void showAddInstancePopup(View view) {
        CreateInstancePopupFragment popup = new CreateInstancePopupFragment(this);
        popup.show(this.getSupportFragmentManager(), "");
    }

    private void setTabActive(int index){
        for (Button tab : Tabs) {
            tab.setTypeface(null, Typeface.NORMAL);
            tab.setTextColor(Color.rgb(220,220,220)); //Slightly less bright white.
        }
        Tabs[index].setTypeface(Tabs[index].getTypeface(), Typeface.BOLD);
        Tabs[index].setTextColor(Color.WHITE);

        //Animating the white bar on the left
        ValueAnimator animation = ValueAnimator.ofFloat(selectedTab.getY(), Tabs[index].getY()+(Tabs[index].getHeight()- selectedTab.getHeight())/2f);
        animation.setDuration(250);
        animation.addUpdateListener(animation1 -> selectedTab.setY((float) animation1.getAnimatedValue()));
        animation.start();
    }

    protected void initTabs(int activeTab){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            //Do something after 100ms
            selectTabPage(activeTab);
        });
    }

    private void changeLookAndFeel(boolean useOldLook){
        Guideline guideLine = findViewById(R.id.guidelineLeft);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();

        if(useOldLook || getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            //UI v1 Style
            //Hide the sidebar
            params.guidePercent = 0; // 0%, range: 0 <-> 1
            guideLine.setLayoutParams(params);

            //Remove the selected Tab and the head image
            selectedTab.setVisibility(View.GONE);
            accountFaceImageView.setVisibility(View.GONE);

            //Enlarge the button, but just a bit.
            params = (ConstraintLayout.LayoutParams) mPlayButton.getLayoutParams();
            params.matchConstraintPercentWidth = 0.35f;
        }else{
            //UI v2 Style
            //Show the sidebar back
            params.guidePercent = 0.23f; // 23%, range: 0 <-> 1
            guideLine.setLayoutParams(params);

            //Show the selected Tab
            selectedTab.setVisibility(View.VISIBLE);
            accountFaceImageView.setVisibility(View.VISIBLE);

            //Set the default button size
            params = (ConstraintLayout.LayoutParams) mPlayButton.getLayoutParams();
            params.matchConstraintPercentWidth = 0.25f;
        }
        mPlayButton.setLayoutParams(params);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        //Try to get the notch so it can be taken into account in settings
        if (Build.VERSION.SDK_INT >= P){
            try {
                PREF_NOTCH_SIZE = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout().getBoundingRects().get(0).width();
            }catch (Exception e){
                Log.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode");
                PREF_NOTCH_SIZE = -1;
            }
            Tools.updateWindowSize(this);
        }
    }

    /**
     * Custom back stack system. Use the classic backstack when the focus is on the setting screen,
     * finish the activity and remove the back_preference listener otherwise
     */
    @Override
    public void onBackPressed() {
        int count = getSupportFragmentManager().getBackStackEntryCount();

        if(count > 0 && viewPager.getCurrentItem() == 3){
            getSupportFragmentManager().popBackStack();
        }else{
            super.onBackPressed();
            //additional code
            ExtraCore.removeExtraListenerFromValue("back_preference", backPreferenceListener);
            finish();
        }
    }
}

