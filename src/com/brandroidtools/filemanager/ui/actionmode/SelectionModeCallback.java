package com.brandroidtools.filemanager.ui.actionmode;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.*;
import android.widget.ShareActionProvider;
import com.brandroidtools.filemanager.FileManagerApplication;
import com.brandroidtools.filemanager.R;
import com.brandroidtools.filemanager.listeners.OnRequestRefreshListener;
import com.brandroidtools.filemanager.listeners.OnSelectionListener;
import com.brandroidtools.filemanager.model.FileSystemObject;
import com.brandroidtools.filemanager.model.SystemFile;
import com.brandroidtools.filemanager.preferences.AccessMode;
import com.brandroidtools.filemanager.ui.dialogs.InputNameDialog;
import com.brandroidtools.filemanager.ui.policy.*;
import com.brandroidtools.filemanager.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectionModeCallback implements ActionMode.Callback {
    private MenuItem mCut;
    private MenuItem mCopy;
    private MenuItem mPaste;
    private MenuItem mArchive;
    private MenuItem mDelete;
    private MenuItem mShare;
    private MenuItem mRename;
    private MenuItem mDetails;
    private ShareActionProvider mShareActionProvider;

    private boolean pasteReady = false;

    boolean mClosedByUser = true;
    private Activity mActivity;
    private ActionMode mSelectionMode;
    private final Boolean mGlobal;
    private final Boolean mSearch;
    private final Boolean mChRooted;

    /**
     * @hide
     */
    OnRequestRefreshListener mOnRequestRefreshListener;
    /**
     * @hide
     */
    OnSelectionListener mOnSelectionListener;
    private FileSystemObject mFso;

    /**
     * Constructor of <code>ActionsDialog</code>.
     *
     * @param activity The current Activity context
     * @param global If the menu to display will be the global one (Global actions)
     * @param search If the call is from search activity
     */
    public SelectionModeCallback (Activity activity, Boolean global, Boolean search) {
        this.mActivity = activity;
        this.mGlobal = global;
        this.mSearch = search;
        this.mChRooted = FileManagerApplication.getAccessMode().compareTo(AccessMode.SAFE) == 0;
    }

    /**
     * Method that sets the listener for communicate a refresh request.
     *
     * @param onRequestRefreshListener The request refresh listener
     */
    public void setOnRequestRefreshListener(OnRequestRefreshListener onRequestRefreshListener) {
        this.mOnRequestRefreshListener = onRequestRefreshListener;
    }

    /**
     * Method that sets the listener for requesting selection data
     *
     * @param onSelectionListener The request selection data  listener
     */
    public void setOnSelectionListener(OnSelectionListener onSelectionListener) {
        this.mOnSelectionListener = onSelectionListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mSelectionMode = mode;

        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.actions, menu);

        // Set file with share history to the provider and set the share intent.
//        mShareActionProvider = (ShareActionProvider) mShare.getActionProvider();
//        mShareActionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Get selection
        List<FileSystemObject> selection = null;
        if (this.mOnSelectionListener != null) {
            selection = this.mOnSelectionListener.onRequestSelectedFiles();
        }

        /*
         * SINGLE FILE ACTIONS
         */

        if (selection.size() == 1) {
            this.mFso = selection.get(0);
        } else {
            this.mFso = null;
        }

        //- Check actions that needs a valid reference
        if (!this.mGlobal && this.mFso != null) {
            //- Select/Deselect -> Only one of them
            if (this.mOnSelectionListener != null) {
                boolean selected =
                        SelectionHelper.isFileSystemObjectSelected(
                                this.mOnSelectionListener.onRequestSelectedFiles(),
                                this.mFso);
                menu.removeItem(selected ? R.id.mnu_actions_select : R.id.mnu_actions_deselect);

            } else {
                // Remove both menus
                menu.removeItem(R.id.mnu_actions_select);
                menu.removeItem(R.id.mnu_actions_deselect);

                // Not allowed because we need a list of the current files (only from navigation
                // activity)
                menu.removeItem(R.id.mnu_actions_rename);
                menu.removeItem(R.id.mnu_actions_create_copy);
            }

            //- Open/Open with -> Only when the fso is not a folder and is not a system file
            if (FileHelper.isDirectory(this.mFso) || FileHelper.isSystemFile(this.mFso)) {
                menu.removeItem(R.id.mnu_actions_open);
                menu.removeItem(R.id.mnu_actions_open_with);
                menu.removeItem(R.id.mnu_actions_send);
            }

            // Create link (not allow in storage volume)
            if (StorageHelper.isPathInStorageVolume(this.mFso.getFullPath())) {
                menu.removeItem(R.id.mnu_actions_create_link);
            }

            //Execute only if mime/type category is EXEC
            MimeTypeHelper.MimeTypeCategory category = MimeTypeHelper.getCategory(this.mActivity, this.mFso);
            if (category.compareTo(MimeTypeHelper.MimeTypeCategory.EXEC) != 0) {
                menu.removeItem(R.id.mnu_actions_execute);
            }
        }

        //- Add to bookmarks -> Only directories
        if (this.mFso != null && FileHelper.isRootDirectory(this.mFso)) {
            menu.removeItem(R.id.mnu_actions_add_to_bookmarks);
            menu.removeItem(R.id.mnu_actions_add_to_bookmarks_current_folder);
        }

        /*
         * MULTI FILE ACTIONS
         */

        //- Paste/Move only when have a selection
        if (this.mGlobal) {
            if (selection == null || selection.size() == 0 ||
                    (this.mFso != null && !FileHelper.isDirectory(this.mFso))) {
                // Remove paste/move actions
                menu.removeItem(R.id.mnu_actions_paste_selection);
                menu.removeItem(R.id.mnu_actions_move_selection);
                menu.removeItem(R.id.mnu_actions_delete_selection);
            }
        }
        //- Create link
        if (this.mGlobal && (selection == null || selection.size() == 0 || selection.size() > 1)) {
            // Only when one item is selected
            menu.removeItem(R.id.mnu_actions_create_link_global);
        } else if (this.mGlobal  && selection != null) {
            // Create link (not allow in storage volume)
            FileSystemObject fso = selection.get(0);
            if (StorageHelper.isPathInStorageVolume(fso.getFullPath())) {
                menu.removeItem(R.id.mnu_actions_create_link);
            }
        } else if (!this.mGlobal) {
            // Create link (not allow in storage volume)
            if (StorageHelper.isPathInStorageVolume(this.mFso.getFullPath())) {
                menu.removeItem(R.id.mnu_actions_create_link);
            }
        }

        //- Compress/Uncompress (only when selection is available)
        if (this.mOnSelectionListener != null) {
            //Compress
            if (this.mGlobal) {
                if (selection == null || selection.size() == 0) {
                    menu.removeItem(R.id.mnu_actions_compress_selection);
                }
            } else {
                // Ignore for system files
                if (this.mFso instanceof SystemFile) {
                    menu.removeItem(R.id.mnu_actions_compress);
                }
            }
            //Uncompress (Only supported files)
            if (!this.mGlobal && !FileHelper.isSupportedUncompressedFile(this.mFso)) {
                menu.removeItem(R.id.mnu_actions_extract);
            }

            // Send multiple (only regular files)
            if (this.mGlobal) {
                if (selection == null || selection.size() == 0) {
                    menu.removeItem(R.id.mnu_actions_send_selection);
                } else {
                    boolean areAllFiles = true;
                    int cc = selection.size();
                    for (int i = 0; i < cc; i++) {
                        FileSystemObject fso = selection.get(i);
                        if (FileHelper.isDirectory(fso)) {
                            areAllFiles = false;
                            break;
                        }
                    }
                    if (!areAllFiles) {
                        menu.removeItem(R.id.mnu_actions_send_selection);
                    }
                }
            }
        }

        // Not allowed in search
        if (this.mSearch) {
            menu.removeItem(R.id.mnu_actions_extract);
            menu.removeItem(R.id.mnu_actions_compress);
            menu.removeItem(R.id.mnu_actions_create_link);
        }

        // Not allowed if not in search
        if (!this.mSearch) {
            menu.removeItem(R.id.mnu_actions_open_parent_folder);
        }

        // Remove not-ChRooted actions (actions that can't be present when running in
        // unprivileged mode)
        if (this.mChRooted) {
            menu.removeItem(R.id.mnu_actions_create_link);
            menu.removeItem(R.id.mnu_actions_create_link_global);
            menu.removeItem(R.id.mnu_actions_execute);

            // NOTE: This actions are not implemented in chrooted environments. The reason is
            // that the main target of this application is CyanogenMod (a rooted environment).
            // Adding this actions requires the use of commons-compress, an external Apache
            // library that will add more size to the ending apk.
            // For now, will maintain without implementation. Maybe, in the future.
            menu.removeItem(R.id.mnu_actions_compress);
            menu.removeItem(R.id.mnu_actions_compress_selection);
            menu.removeItem(R.id.mnu_actions_extract);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {

        switch (menuItem.getItemId()) {
            //- Create new object
            case R.id.mnu_actions_new_directory:
            case R.id.mnu_actions_new_file:
                if (this.mOnSelectionListener != null) {
                    showInputNameDialog(menuItem);
                    return true;
                }
                break;

            //- Rename
            case R.id.mnu_actions_rename:
                if (this.mOnSelectionListener != null) {
                    showFsoInputNameDialog(menuItem, this.mFso, false);
                    return true;
                }
                break;

            //- Create link
            case R.id.mnu_actions_create_link:
                if (this.mOnSelectionListener != null) {
                    showFsoInputNameDialog(menuItem, this.mFso, true);
                    return true;
                }
                break;
            case R.id.mnu_actions_create_link_global:
                if (this.mOnSelectionListener != null) {
                    // The selection must be only 1 item
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    if (selection != null && selection.size() == 1) {
                        showFsoInputNameDialog(menuItem, selection.get(0), true);
                    }
                    return true;
                }
                break;

            //- Delete
            case R.id.mnu_actions_delete:
                DeleteActionPolicy.removeFileSystemObject(
                        this.mActivity,
                        this.mFso,
                        this.mOnSelectionListener,
                        this.mOnRequestRefreshListener,
                        null);
                break;

            //- Refresh
            case R.id.mnu_actions_refresh:
                if (this.mOnRequestRefreshListener != null) {
                    this.mOnRequestRefreshListener.onRequestRefresh(null, false); //Refresh all
                }
                break;

            //- Select/Deselect
            case R.id.mnu_actions_select:
            case R.id.mnu_actions_deselect:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onToggleSelection(this.mFso);
                }
                break;
            case R.id.mnu_actions_select_all:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onSelectAllVisibleItems();
                }
                break;
            case R.id.mnu_actions_deselect_all:
                if (this.mOnSelectionListener != null) {
                    this.mOnSelectionListener.onDeselectAllVisibleItems();
                }
                break;

            //- Open
            case R.id.mnu_actions_open:
                IntentsActionPolicy.openFileSystemObject(
                        this.mActivity, this.mFso, false, null, null);
                break;
            //- Open with
            case R.id.mnu_actions_open_with:
                IntentsActionPolicy.openFileSystemObject(
                        this.mActivity, this.mFso, true, null, null);
                break;

            //- Execute
            case R.id.mnu_actions_execute:
                ExecutionActionPolicy.execute(this.mActivity, this.mFso);
                break;

            //- Send
            case R.id.mnu_actions_send:
                IntentsActionPolicy.sendFileSystemObject(
                        this.mActivity, this.mFso, null, null);
                break;
            case R.id.mnu_actions_send_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    if (selection.size() == 1) {
                        IntentsActionPolicy.sendFileSystemObject(
                                this.mActivity, selection.get(0), null, null);
                    } else {
                        IntentsActionPolicy.sendMultipleFileSystemObject(
                                this.mActivity, selection, null, null);
                    }
                }
                break;

            // Paste selection
            case R.id.mnu_actions_paste_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    CopyMoveActionPolicy.copyFileSystemObjects(
                            this.mActivity,
                            createLinkedResource(selection, this.mFso),
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;
            // Move selection
            case R.id.mnu_actions_move_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    CopyMoveActionPolicy.moveFileSystemObjects(
                            this.mActivity,
                            createLinkedResource(selection, this.mFso),
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;
            // Delete selection
            case R.id.mnu_actions_delete_selection:
                if (this.mOnSelectionListener != null) {
                    List<FileSystemObject> selection =
                            this.mOnSelectionListener.onRequestSelectedFiles();
                    DeleteActionPolicy.removeFileSystemObjects(
                            this.mActivity,
                            selection,
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener,
                            null);
                }
                break;

            //- Uncompress
            case R.id.mnu_actions_extract:
                CompressActionPolicy.uncompress(
                        this.mActivity,
                        this.mFso,
                        this.mOnRequestRefreshListener);
                break;
            //- Compress
            case R.id.mnu_actions_compress:
                if (this.mOnSelectionListener != null) {
                    CompressActionPolicy.compress(
                            this.mActivity,
                            this.mFso,
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;
            case R.id.mnu_actions_compress_selection:
                if (this.mOnSelectionListener != null) {
                    CompressActionPolicy.compress(
                            this.mActivity,
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;

            //- Create copy
            case R.id.mnu_actions_create_copy:
                // Create a copy of the fso
                if (this.mOnSelectionListener != null) {
                    CopyMoveActionPolicy.createCopyFileSystemObject(
                            this.mActivity,
                            this.mFso,
                            this.mOnSelectionListener,
                            this.mOnRequestRefreshListener);
                }
                break;

            //- Add to bookmarks
            case R.id.mnu_actions_add_to_bookmarks:
            case R.id.mnu_actions_add_to_bookmarks_current_folder:
                BookmarksActionPolicy.addToBookmarks(this.mActivity, this.mFso);
                break;

            //- Add shortcut
            case R.id.mnu_actions_add_shortcut:
            case R.id.mnu_actions_add_shortcut_current_folder:
                IntentsActionPolicy.createShortcut(this.mActivity, this.mFso);
                break;

            //- Properties
            case R.id.mnu_actions_properties:
            case R.id.mnu_actions_properties_current_folder:
                InfoActionPolicy.showPropertiesDialog(
                        this.mActivity, this.mFso, this.mOnRequestRefreshListener);
                break;

            //- Navigate to parent
            case R.id.mnu_actions_open_parent_folder:
                NavigationActionPolicy.openParentFolder(
                        this.mActivity, this.mFso, this.mOnRequestRefreshListener);
                break;

            default:
                break;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Clear this before onDeselectAll() to prevent onDeselectAll() from
        // trying to close the
        // contextual mode again.
        mSelectionMode = null;
        if (mClosedByUser) {
            // Clear selection, only when the contextual mode is explicitly
            // closed by the user.
            //
            // We close the contextual mode when the fragment becomes
            // temporary invisible
            // (i.e. mIsVisible == false) too, in which case we want to keep
            // the selection.
            mOnSelectionListener.onDeselectAll();
        }
    }

    public void finish() {
        mSelectionMode.finish();
    }

    public void setClosedByUser(boolean closedByUser) {
        this.mClosedByUser = closedByUser;
    }

    public boolean inSelectionMode() {
        return mSelectionMode != null;
    }

    public void refresh() {
        mSelectionMode.invalidate();
    }

    /**
     * Method that show a new dialog for input a name.
     *
     * @param menuItem The item menu associated
     */
    private void showInputNameDialog(final MenuItem menuItem) {

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        this.mActivity,
                        this.mOnSelectionListener.onRequestCurrentItems(),
                        menuItem.getTitle().toString());
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                try {
                    String name = inputNameDialog.getName();
                    createNewFileSystemObject(menuItem.getItemId(), name);

                } catch (InflateException e) {
                    //TODO handle this exception properly
                }
            }
        });
        inputNameDialog.show();
    }

    /**
     * Method that show a new dialog for input a name for an existing fso.
     *
     * @param menuItem The item menu associated
     * @param fso The file system object
     * @param allowFsoName If allow that the name of the fso will be returned
     */
    private void showFsoInputNameDialog(
            final MenuItem menuItem, final FileSystemObject fso, final boolean allowFsoName) {

        //Show the input name dialog
        final InputNameDialog inputNameDialog =
                new InputNameDialog(
                        this.mActivity,
                        this.mOnSelectionListener.onRequestCurrentItems(),
                        fso,
                        allowFsoName,
                        menuItem.getTitle().toString());
        inputNameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //Retrieve the name an execute the action
                try {
                    String name = inputNameDialog.getName();
                    switch (menuItem.getItemId()) {
                        case R.id.mnu_actions_rename:
                            // Rename the fso
                            if (SelectionModeCallback.this.mOnSelectionListener != null) {
                                CopyMoveActionPolicy.renameFileSystemObject(
                                        SelectionModeCallback.this.mActivity,
                                        inputNameDialog.mFso,
                                        name,
                                        SelectionModeCallback.this.mOnSelectionListener,
                                        SelectionModeCallback.this.mOnRequestRefreshListener);
                            }
                            break;

                        case R.id.mnu_actions_create_link:
                        case R.id.mnu_actions_create_link_global:
                            // Create a link to the fso
                            if (SelectionModeCallback.this.mOnSelectionListener != null) {
                                NewActionPolicy.createSymlink(
                                        SelectionModeCallback.this.mActivity,
                                        inputNameDialog.mFso,
                                        name,
                                        SelectionModeCallback.this.mOnSelectionListener,
                                        SelectionModeCallback.this.mOnRequestRefreshListener);
                            }
                            break;

                        default:
                            break;
                    }

                } catch (InflateException e) {
                    //TODO: Catch this exception properly
                }
            }
        });
        inputNameDialog.show();
    }

    /**
     * Method that create the a new file system object.
     *
     * @param menuId The menu identifier (need to determine the fso type)
     * @param name The name of the file system object
     * @hide
     */
    void createNewFileSystemObject(final int menuId, final String name) {
        switch (menuId) {
            case R.id.mnu_actions_new_directory:
                NewActionPolicy.createNewDirectory(
                        this.mActivity, name,
                        this.mOnSelectionListener, this.mOnRequestRefreshListener);
                break;
            case R.id.mnu_actions_new_file:
                NewActionPolicy.createNewFile(
                        this.mActivity, name,
                        this.mOnSelectionListener, this.mOnRequestRefreshListener);
                break;
            default:
                break;
        }
    }

    /**
     * Method that creates a {@link com.brandroidtools.filemanager.ui.policy.CopyMoveActionPolicy.LinkedResource} for the list of object to the
     * destination directory
     *
     * @param items The list of the source items
     * @param directory The destination directory
     */
    private static List<CopyMoveActionPolicy.LinkedResource> createLinkedResource(
            List<FileSystemObject> items, FileSystemObject directory) {
        List<CopyMoveActionPolicy.LinkedResource> resources =
                new ArrayList<CopyMoveActionPolicy.LinkedResource>(items.size());
        int cc = items.size();
        for (int i = 0; i < cc; i++) {
            FileSystemObject fso = items.get(i);
            File src = new File(fso.getFullPath());
            File dst = new File(directory.getFullPath(), fso.getName());
            resources.add(new CopyMoveActionPolicy.LinkedResource(src, dst));
        }
        return resources;
    }
}
