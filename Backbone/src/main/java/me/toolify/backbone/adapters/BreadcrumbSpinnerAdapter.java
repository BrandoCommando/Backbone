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

package me.toolify.backbone.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import me.toolify.backbone.R;
import me.toolify.backbone.util.FileHelper;

public class BreadcrumbSpinnerAdapter extends BaseAdapter implements SpinnerAdapter{

    private Context mContext;
    private ArrayList<File> fileList;

    public BreadcrumbSpinnerAdapter(Context context, ArrayList<File> fileList) {
        this.mContext = context;
        this.fileList = fileList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return fileList.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout row = (RelativeLayout) View.inflate(mContext, R.layout.breadcrumb_spinner_selected_item, null);
        TextView title = (TextView)row.findViewById(R.id.breadcrumb_spinner_item_title);
        TextView subtitle = (TextView)row.findViewById(R.id.breadcrumb_spinner_item_subtitle);
        title.setText(buildTitleString(fileList.get(position)));
        subtitle.setText(buildSubtitleString(fileList.get(position)));
        return row;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) View.inflate(mContext, R.layout.breadcrumb_spinner_dropdown_item, null);
        textView.setText(buildTitleString(fileList.get(position)));
        return textView;
    }

    private String buildTitleString(File file) {
        String fileString;
        if (file.compareTo(new File(FileHelper.ROOT_DIRECTORY)) == 0){
            fileString = "/";
        } else {
            fileString = file.getName();
        }
        return fileString;
    }

    private String buildSubtitleString(File file) {
        String fileString;
        if (file.compareTo(new File(FileHelper.ROOT_DIRECTORY)) == 0){
            fileString = "Filesystem Root";
        } else {
            fileString = file.getParent();
        }
        return fileString;
    }
}
