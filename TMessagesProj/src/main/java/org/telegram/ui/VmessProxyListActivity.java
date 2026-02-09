/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.List;

public class VmessProxyListActivity extends BaseFragment {
    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;
    private int useVmessRow;
    private int useVmessShadowRow;
    private int vmessStartRow;
    private int vmessEndRow;
    private int vmessAddRow;
    private int vmessShadowRow;

    private List<SharedConfig.VmessProxyInfo> vmessList = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.VmessProxySettings));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new RecyclerListView(context);
        listView = (RecyclerListView) fragmentView;
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setGlowColor(Theme.getColor(Theme.key_actionBarDefault));
        listView.setAdapter(listAdapter = new ListAdapter(context));

        listView.setOnItemClickListener((view, position) -> {
            if (position == useVmessRow) {
                boolean enabled = !SharedConfig.isVmessProxyEnabled();
                if (enabled && SharedConfig.currentVmessProxy == null && !vmessList.isEmpty()) {
                    setCurrentProxy(vmessList.get(0));
                }
                SharedConfig.setVmessProxyEnabled(enabled && SharedConfig.currentVmessProxy != null);
                listAdapter.notifyItemChanged(useVmessRow);
                listAdapter.notifyDataSetChanged();
            } else if (position == vmessAddRow) {
                presentFragment(new VmessProxySettingsActivity());
            } else if (position >= vmessStartRow && position < vmessEndRow) {
                SharedConfig.VmessProxyInfo info = vmessList.get(position - vmessStartRow);
                setCurrentProxy(info);
                SharedConfig.setVmessProxyEnabled(true);
                listAdapter.notifyItemChanged(useVmessRow);
                listAdapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            if (position >= vmessStartRow && position < vmessEndRow) {
                SharedConfig.VmessProxyInfo info = vmessList.get(position - vmessStartRow);
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(info.server + ":" + info.port);
                builder.setItems(new CharSequence[]{
                        LocaleController.getString(R.string.Edit),
                        LocaleController.getString(R.string.Delete)
                }, (dialog, which) -> {
                    if (which == 0) {
                        presentFragment(new VmessProxySettingsActivity(info));
                    } else {
                        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(getParentActivity());
                        confirmBuilder.setTitle(LocaleController.getString(R.string.DeleteVmessProxyTitle));
                        confirmBuilder.setMessage(LocaleController.getString(R.string.DeleteVmessProxyConfirm));
                        confirmBuilder.setPositiveButton(LocaleController.getString(R.string.Delete), (confirmDialog, confirmWhich) -> {
                            SharedConfig.deleteVmessProxy(info);
                            reloadData();
                        });
                        confirmBuilder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                        showDialog(confirmBuilder.create());
                    }
                });
                showDialog(builder.create());
                return true;
            }
            return false;
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    private void reloadData() {
        SharedConfig.loadVmessProxyList();
        vmessList = new ArrayList<>(SharedConfig.vmessProxyList);
        updateRows();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;
        useVmessRow = rowCount++;
        useVmessShadowRow = rowCount++;
        if (!vmessList.isEmpty()) {
            vmessStartRow = rowCount;
            rowCount += vmessList.size();
            vmessEndRow = rowCount;
        } else {
            vmessStartRow = -1;
            vmessEndRow = -1;
        }
        vmessAddRow = rowCount++;
        vmessShadowRow = rowCount++;
    }

    private void setCurrentProxy(SharedConfig.VmessProxyInfo info) {
        SharedConfig.currentVmessProxy = info;
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putString("vmess_proxy_server", info.server);
        editor.putInt("vmess_proxy_port", info.port);
        editor.putString("vmess_proxy_uuid", info.uuid);
        editor.putString("vmess_proxy_security", info.security);
        editor.putBoolean("vmess_proxy_tls", info.tls);
        editor.apply();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        private ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == useVmessShadowRow || position == vmessShadowRow) {
                return 0;
            } else if (position == useVmessRow) {
                return 1;
            } else {
                return 2;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == useVmessRow || position == vmessAddRow || (position >= vmessStartRow && position < vmessEndRow);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 1:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                default:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == useVmessRow) {
                TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                checkCell.setTextAndCheck(LocaleController.getString(R.string.UseVmessProxy), SharedConfig.isVmessProxyEnabled(), false);
                return;
            }
            if (position == vmessAddRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.AddVmessProxy), false);
                return;
            }
            if (position >= vmessStartRow && position < vmessEndRow) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                SharedConfig.VmessProxyInfo info = vmessList.get(position - vmessStartRow);
                String title = info.server + ":" + info.port;
                String uuid = info.uuid;
                if (!TextUtils.isEmpty(uuid) && uuid.length() > 8) {
                    uuid = uuid.substring(0, 8) + "...";
                }
                boolean divider = position != vmessEndRow - 1;
                cell.setTextAndValue(title, uuid, divider);
                if (SharedConfig.currentVmessProxy == info) {
                    cell.getValueImageView().setImageResource(R.drawable.msg_check_s);
                    cell.getValueImageView().setVisibility(View.VISIBLE);
                } else {
                    cell.getValueImageView().setVisibility(View.INVISIBLE);
                }
            }
        }
    }
}
