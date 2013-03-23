/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.brandroidtools.filemanager.adapters;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.brandroidtools.filemanager.fragments.NavigationFragment;

/**
 * Implementation of {@link android.support.v4.view.PagerAdapter} that
 * represents each page as a {@link Fragment} that is persistently kept in the
 * fragment manager as long as the user can return to the page.
 *
 * <p>
 * This version of the pager is best for use when there are a handful of
 * typically more static fragments to be paged through, such as a set of tabs.
 * The fragment of each page the user visits will be kept in memory, though its
 * view hierarchy may be destroyed when not visible. This can result in using a
 * significant amount of memory since fragment instances can hold on to an
 * arbitrary amount of state. For larger sets of pages, consider
 * {@link FragmentStatePagerAdapter}.
 *
 * <p>
 * When using FragmentPagerAdapter the host ViewPager must have a valid ID set.
 * </p>
 *
 * <p>
 * Subclasses only need to implement {@link #getItem(int)} and
 * {@link #getCount()} to have a working adapter.
 *
 * <p>
 * Here is an example implementation of a pager containing fragments of lists:
 *
 * {@sample
 * development/samples/Support4Demos/src/com/example/android/supportv4/app/
 * FragmentPagerSupport.java complete}
 *
 * <p>
 * The <code>R.layout.fragment_pager</code> resource of the top-level fragment
 * is:
 *
 * {@sample development/samples/Support4Demos/res/layout/fragment_pager.xml
 * complete}
 *
 * <p>
 * The <code>R.layout.fragment_pager_list</code> resource containing each
 * individual fragment's layout is:
 *
 * {@sample development/samples/Support4Demos/res/layout/fragment_pager_list.xml
 * complete}
 */
public class NavigationFragmentPagerAdapter extends PagerAdapter {
    private static final String TAG = "FragmentPagerAdapter";
    private static final boolean DEBUG = false;

    private final FragmentManager mFragmentManager;
    private FragmentTransaction mCurTransaction = null;
    private Fragment mCurrentPrimaryItem = null;

    private int mNumPages;

    public NavigationFragmentPagerAdapter(Context context, FragmentManager fm, int numPages) {
        mFragmentManager = fm;
        this.mNumPages = numPages;
    }

    /**
     * Return the Fragment associated with a specified position.
     *
     * @param position the integer position of the requested fragment within the Pager Adapter
     */
    public Fragment getItem(int position) {
        NavigationFragment myFragment = NavigationFragment.newInstance(position);
        return myFragment;
    }

    /**
     * This function is the reason why this class is a full copy of
     * FragmentPagerAdapter and not an implementation. The app required the
     * ability to call functions on each FileListFragment from the main class.
     * This function needed to be able to reference mFragmentManger.
     *
     * @param container
     * @param position
     * @return FileListFragment
     */
    public NavigationFragment getFragment(ViewGroup container, int position) {
        String name = makeFragmentName(container.getId(), position);
        NavigationFragment fragment = (NavigationFragment) mFragmentManager.findFragmentByTag(name);
        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return mNumPages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startUpdate(ViewGroup container) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }

        // Do we already have this fragment?
        String name = makeFragmentName(container.getId(), position);
        Fragment fragment = mFragmentManager.findFragmentByTag(name);
        if (fragment != null) {
            if (DEBUG)
                Log.v(TAG, "Attaching item #" + position + ": f=" + fragment);
            mCurTransaction.attach(fragment);
        } else {
            fragment = getItem(position);
            if (DEBUG)
                Log.v(TAG, "Adding item #" + position + ": f=" + fragment);
            mCurTransaction.add(container.getId(), fragment, makeFragmentName(container.getId(), position));
        }
        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }

        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        if (DEBUG)
            Log.v(TAG, "Detaching item #" + position + ": f=" + object + " v=" + ((Fragment) object).getView());
        mCurTransaction.detach((Fragment) object);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finishUpdate(ViewGroup container) {
        if (mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            mFragmentManager.executePendingTransactions();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((Fragment) object).getView() == view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parcelable saveState() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }

    private static String makeFragmentName(int viewId, int index) {
        return "android:switcher:" + viewId + ":" + index;
    }
}