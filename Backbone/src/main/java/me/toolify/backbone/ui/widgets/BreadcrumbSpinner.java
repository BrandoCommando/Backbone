/*
 * Copyright (C) 2013 BrandroidTools
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.toolify.backbone.ui.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.toolify.backbone.adapters.BreadcrumbSpinnerAdapter;
import me.toolify.backbone.bus.BusProvider;
import me.toolify.backbone.bus.events.FilesystemStatusUpdateEvent;
import me.toolify.backbone.fragments.NavigationFragment;
import me.toolify.backbone.model.DiskUsage;
import me.toolify.backbone.model.MountPoint;
import me.toolify.backbone.tasks.FilesystemAsyncTask;
import me.toolify.backbone.util.FileHelper;
import me.toolify.backbone.util.StorageHelper;

/**
 * A view that holds a navigation breadcrumb pattern.
 */
public class BreadcrumbSpinner extends Spinner implements Breadcrumb, OnItemSelectedListener {

    private Context mContext;
    /**
     * @hide
     */
    private MountPoint mMountPointInfo;
    /**
     * @hide
     */
    private DiskUsage mDiskUsageInfo;
    private FilesystemAsyncTask mFilesystemAsyncTask;

    private int mFreeDiskSpaceWarningLevel = 95;

    private List<BreadcrumbListener> mBreadcrumbListeners;

    private BreadcrumbSpinnerAdapter mAdapter;

    private String mCurrentPath;
    private NavigationFragment mNavigationFragment;
    /**
     * @hide
     */
    private boolean mPauseSpinnerClicks;

    /**
     * Constructor of <code>BreadcrumbSpinner</code>.
     *
     * @param context The current context
     */
    public BreadcrumbSpinner(Context context) {
        super(context);
        this.mContext = context;
        init();
    }

    /**
     * Constructor of <code>BreadcrumbSpinner</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     */
    public BreadcrumbSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    /**
     * Constructor of <code>BreadcrumbSpinner</code>.
     *
     * @param context The current context
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyle The default style to apply to this view. If 0, no style
     *        will be applied (beyond what is included in the theme). This may
     *        either be an attribute resource, whose value will be retrieved
     *        from the current theme, or an explicit style resource.
     */
    public BreadcrumbSpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        init();
    }

    /**
     * Method that initializes the view. This method loads all the necessary
     * information and create an appropriate layout for the view
     */
    private void init() {
        // Spinner selection fires by default
        mPauseSpinnerClicks = false;

        // Initialize the listeners
        this.mBreadcrumbListeners =
              Collections.synchronizedList(new ArrayList<BreadcrumbListener>());

        setOnItemSelectedListener(this);

        // Change the image of filesystem (this is not called after a changeBreadcrumbPath call,
        // so if need to be theme previously to protect from errors)
        BusProvider.postEvent(new FilesystemStatusUpdateEvent(
                FilesystemStatusUpdateEvent.INDICATOR_WARNING));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFreeDiskSpaceWarningLevel(int percentage) {
        this.mFreeDiskSpaceWarningLevel = percentage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addBreadcrumbListener(BreadcrumbListener listener) {
        this.mBreadcrumbListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeBreadcrumbListener(BreadcrumbListener listener) {
        this.mBreadcrumbListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startLoading() {
        //Show/Hide views
        this.post(new Runnable() {
            @Override
            public void run() {
                BusProvider.postEvent(new FilesystemStatusUpdateEvent(
                        FilesystemStatusUpdateEvent.INDICATOR_REFRESHING));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endLoading() {
        //Show/Hide views
        this.post(new Runnable() {
            @Override
            public void run() {
                BusProvider.postEvent(new FilesystemStatusUpdateEvent(
                        FilesystemStatusUpdateEvent.INDICATOR_STOP_REFRESHING));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void setNavigationFragment(NavigationFragment fragment) {
        this.mNavigationFragment = fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changeBreadcrumbPath(final String newPath, final boolean chRooted) {
        //Sets the current path
        this.mCurrentPath = newPath;

        //Update the mount point information
        updateMountPointInfo();

        ArrayList<File> breadcrumbFiles = new ArrayList<File>();

        // The first is always the root (except if we are in a ChRooted environment)
        if (!chRooted) {
            breadcrumbFiles.add(new File(FileHelper.ROOT_DIRECTORY));
        }

        //Add the rest of the path
        String[] dirs = newPath.split(File.separator);
        int cc = dirs.length;
        if (chRooted) {
            for (int i = 1; i < cc; i++) {
                File f = createFile(dirs, i);
                if (StorageHelper.isPathInStorageVolume(f.getAbsolutePath())) {
                    breadcrumbFiles.add(f);
                }
            }
        } else {
            for (int i = 1; i < cc; i++) {
                breadcrumbFiles.add(createFile(dirs, i));
            }
        }

        // Now apply the theme to the breadcrumb
        //applyTheme();

        mAdapter = new BreadcrumbSpinnerAdapter(mContext, breadcrumbFiles);
        this.setAdapter(mAdapter);
        // Don't perform selection logic for the initial setSelection
        mPauseSpinnerClicks = true;
        this.setSelection(breadcrumbFiles.size()-1);
    }

    /**
     * Method that refreshes the breadcrumb based on the current directory,
     * which is useful when orientation changes (and the resulting text size changes)
     * are triggered.
     */
    public void refresh () {
        changeBreadcrumbPath(mCurrentPath, mNavigationFragment.mChRooted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void updateMountPointInfo() {
        //Cancel the current execution (if any) and launch again
        if (this.mFilesystemAsyncTask != null && this.mFilesystemAsyncTask.isRunning()) {
           this.mFilesystemAsyncTask.cancel(true);
        }
        this.mFilesystemAsyncTask =
                new FilesystemAsyncTask(
                        getContext(), this, this.mFreeDiskSpaceWarningLevel);
        this.mFilesystemAsyncTask.execute(this.mCurrentPath);
    }

    /**
     * Method that creates the a new file reference for a partial
     * breadcrumb item.
     *
     * @param dirs The split strings directory
     * @param pos The position up to which to create
     * @return File The file reference
     */
    @SuppressWarnings("static-method")
    private File createFile(String[] dirs, int pos) {
        File parent = new File(FileHelper.ROOT_DIRECTORY);
        for (int i = 1; i < pos; i++) {
            parent = new File(parent, dirs[i]);
        }
        return new File(parent, dirs[pos]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MountPoint getMountPointInfo() {
        return this.mMountPointInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMountPointInfo(MountPoint mountPointInfo) {
        this.mMountPointInfo = mountPointInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DiskUsage getDiskUsageInfo() {
        return this.mDiskUsageInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDiskUsageInfo(DiskUsage diskUsageInfo) {
        this.mDiskUsageInfo = diskUsageInfo;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!mPauseSpinnerClicks) {
            int cc = this.mBreadcrumbListeners.size();
            for (int i = 0; i < cc; i++) {
                this.mBreadcrumbListeners.get(i).onBreadcrumbItemClick((File) mAdapter.getItem(position));
            }
        }
        mPauseSpinnerClicks = false;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
