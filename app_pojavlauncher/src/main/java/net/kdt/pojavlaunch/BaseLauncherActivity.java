package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.DIR_GAME_NEW;
import static net.kdt.pojavlaunch.Tools.getFileName;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.webkit.MimeTypeMap;
import android.widget.*;

import androidx.annotation.Nullable;

import java.io.*;

import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.tasks.*;

import androidx.appcompat.app.AlertDialog;
import net.kdt.pojavlaunch.value.*;

import org.apache.commons.io.IOUtils;

public abstract class BaseLauncherActivity extends BaseActivity {
	public Button mPlayButton;
    public Button mAddInstanceButton;
    public ProgressBar mLaunchProgress;
	public Spinner mVersionSelector;
	public MultiRTConfigDialog mRuntimeConfigDialog;
	public TextView mLaunchTextStatus;
    
    public JMinecraftVersionList mVersionList = new JMinecraftVersionList();
	public MinecraftDownloaderTask mTask;
	public MinecraftAccount mProfile;
	//public String[] mAvailableVersions;
    
	public boolean mIsAssetsProcessing = false;
    protected boolean canBack = false;
    
    public abstract void statusIsLaunching(boolean isLaunching);


    /**
     * Used by the reset options button from the layout_main_v4
     * @param view The view triggering the function
     */
    public void resetOptionsTXT(View view){
        try {
            Tools.copyAssetFile(view.getContext(), "options.txt", DIR_GAME_NEW, true);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Used by the install button from the layout_main_v4
     * @param view The view triggering the function
     */
    public void installJarFile(View view){
        installMod(false);
    }


    public static final int RUN_MOD_INSTALLER = 2050;
    private void installMod(boolean customJavaArgs) {
        if (MultiRTUtils.getExactJreName(8) == null) {
            Toast.makeText(this, R.string.multirt_nojava8rt, Toast.LENGTH_LONG).show();
            return;
        }
        if (customJavaArgs) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alerttitle_installmod);
            builder.setNegativeButton(android.R.string.cancel, null);
            final AlertDialog dialog;
            final EditText edit = new EditText(this);
            edit.setSingleLine();
            edit.setHint("-jar/-cp /path/to/file.jar ...");
            builder.setPositiveButton(android.R.string.ok, (di, i) -> {
                Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                intent.putExtra("skipDetectMod", true);
                intent.putExtra("javaArgs", edit.getText().toString());
                startActivity(intent);
            });
            dialog = builder.create();
            dialog.setView(edit);
            dialog.show();
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("jar");
            if(mimeType == null) mimeType = "*/*";
            intent.setType(mimeType);
            startActivityForResult(intent,RUN_MOD_INSTALLER);
        }

    }

    public void launchGame(View v) {
        if (!canBack && mIsAssetsProcessing) {
            mIsAssetsProcessing = false;
            statusIsLaunching(false);
        } else if (canBack) {
            v.setEnabled(false);
            mTask = new MinecraftDownloaderTask(this);
            // TODO: better check!!!
            /*if (mProfile.accessToken.equals("0")) {
                File verJsonFile = new File(Tools.DIR_HOME_VERSION,
                  mProfile.selectedVersion + "/" + mProfile.selectedVersion + ".json");
                if (verJsonFile.exists()) {
                    mTask.onPostExecute(null);
                } else {
                    new AlertDialog.Builder(this)
                        .setTitle(R.string.global_error)
                        .setMessage(R.string.mcl_launch_error_localmode)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                }
            } else {*/
            mTask.execute(mProfile.selectedVersion);
            //}
        }
    }
    
    @Override
    public void onBackPressed() {
        if (canBack) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        System.out.println("call to onPostResume");
        Tools.updateWindowSize(this);
        System.out.println("call to onPostResume; E");
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        System.out.println("call to onResume");
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
        System.out.println("call to onResume; E");
    }

    SharedPreferences.OnSharedPreferenceChangeListener listRefreshListener = null;
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if(listRefreshListener == null) {
            listRefreshListener = (sharedPreferences, key) -> {
                if(key.startsWith("vertype_")) {
                    System.out.println("Verlist update needed!");
                    new RefreshVersionListTask(this).execute();
                }
            };
        }
        LauncherPreferences.DEFAULT_PREF.registerOnSharedPreferenceChangeListener(listRefreshListener);
        new RefreshVersionListTask(this).execute();
        System.out.println("call to onResumeFragments");
        mRuntimeConfigDialog = new MultiRTConfigDialog();
        mRuntimeConfigDialog.prepare(this);

        ((Button)findViewById(R.id.installJarButton)).setOnLongClickListener(view -> {
            installMod(true);
            return true;
        });

        //TODO ADD CRASH CHECK AND FOCUS
        System.out.println("call to onResumeFragments; E");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == Activity.RESULT_OK) {
            final ProgressDialog barrier = new ProgressDialog(this);
            barrier.setMessage(getString(R.string.global_waiting));
            barrier.setProgressStyle(barrier.STYLE_SPINNER);
            barrier.setCancelable(false);
            barrier.show();

            // Install the runtime
            if (requestCode == MultiRTConfigDialog.MULTIRT_PICK_RUNTIME) {
                if (data == null) return;

                final Uri uri = data.getData();
                Thread t = new Thread(() -> {
                    try {
                        String name = getFileName(this, uri);
                        MultiRTUtils.installRuntimeNamed(getContentResolver().openInputStream(uri), name,
                                (resid, stuff) -> BaseLauncherActivity.this.runOnUiThread(
                                        () -> barrier.setMessage(BaseLauncherActivity.this.getString(resid, stuff))));
                        MultiRTUtils.postPrepare(BaseLauncherActivity.this, name);
                    } catch (IOException e) {
                        Tools.showError(BaseLauncherActivity.this, e);
                    }
                    BaseLauncherActivity.this.runOnUiThread(() -> {
                        barrier.dismiss();
                        mRuntimeConfigDialog.refresh();
                        mRuntimeConfigDialog.mDialog.show();
                    });
                });
                t.start();
            }

            // Run a mod installer
            if (requestCode == RUN_MOD_INSTALLER) {
                if (data == null) return;

                final Uri uri = data.getData();
                barrier.setMessage(BaseLauncherActivity.this.getString(R.string.multirt_progress_caching));
                Thread t = new Thread(()->{
                    try {
                        final String name = getFileName(this, uri);
                        final File modInstallerFile = new File(getCacheDir(), name);
                        FileOutputStream fos = new FileOutputStream(modInstallerFile);
                        IOUtils.copy(getContentResolver().openInputStream(uri), fos);
                        fos.close();
                        BaseLauncherActivity.this.runOnUiThread(() -> {
                            barrier.dismiss();
                            Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                            intent.putExtra("modFile", modInstallerFile);
                            startActivity(intent);
                        });
                    }catch(IOException e) {
                        Tools.showError(BaseLauncherActivity.this,e);
                    }
                });
                t.start();
            }

        }
    }

    protected abstract void initTabs(int pageIndex);
}
