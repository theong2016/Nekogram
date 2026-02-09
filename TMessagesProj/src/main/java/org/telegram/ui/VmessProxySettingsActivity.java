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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

public class VmessProxySettingsActivity extends BaseFragment {

    private static final int FIELD_SERVER = 0;
    private static final int FIELD_PORT = 1;
    private static final int FIELD_UUID = 2;
    private static final int FIELD_SECURITY = 3;

    private EditTextBoldCursor[] inputFields;
    private TextCheckCell tlsCell;
    private ActionBarMenuItem doneItem;

    private boolean addingNewProxy;
    private SharedConfig.VmessProxyInfo currentProxyInfo;

    private static final int done_button = 1;

    public VmessProxySettingsActivity() {
        super();
        currentProxyInfo = new SharedConfig.VmessProxyInfo("", 443, "", "auto", false);
        addingNewProxy = true;
    }

    public VmessProxySettingsActivity(SharedConfig.VmessProxyInfo proxyInfo) {
        super();
        currentProxyInfo = proxyInfo;
    }

    @Override
    public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.VmessProxyDetails));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    saveProxy();
                    finishFragment();
                }
            }
        });

        doneItem = actionBar.createMenu().addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56));
        doneItem.setContentDescription(LocaleController.getString(R.string.Done));

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HeaderCell headerCell = new HeaderCell(context);
        headerCell.setText(LocaleController.getString(R.string.VmessProxySettings));
        linearLayout.addView(headerCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        LinearLayout inputFieldsContainer = new LinearLayout(context);
        inputFieldsContainer.setOrientation(LinearLayout.VERTICAL);
        inputFieldsContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        linearLayout.addView(inputFieldsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        inputFields = new EditTextBoldCursor[4];
        for (int a = 0; a < inputFields.length; a++) {
            FrameLayout container = new FrameLayout(context);
            inputFieldsContainer.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

            inputFields[a] = new EditTextBoldCursor(context);
            inputFields[a].setTag(a);
            inputFields[a].setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            inputFields[a].setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
            inputFields[a].setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            inputFields[a].setBackground(null);
            inputFields[a].setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            inputFields[a].setCursorSize(AndroidUtilities.dp(20));
            inputFields[a].setCursorWidth(1.5f);
            inputFields[a].setSingleLine(true);
            inputFields[a].setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            inputFields[a].setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
            inputFields[a].setTransformHintToHeader(true);
            inputFields[a].setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular));
            inputFields[a].setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            inputFields[a].setPadding(0, 0, 0, 0);
            inputFields[a].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateDoneEnabled();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            container.addView(inputFields[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 17, a == FIELD_SERVER ? 12 : 0, 17, 0));

            if (a == FIELD_SERVER) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_URI);
                inputFields[a].setHintText(LocaleController.getString(R.string.VmessProxyServer));
                inputFields[a].setText(currentProxyInfo.server);
            } else if (a == FIELD_PORT) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_NUMBER);
                inputFields[a].setHintText(LocaleController.getString(R.string.VmessProxyPort));
                inputFields[a].setText(String.valueOf(currentProxyInfo.port));
            } else if (a == FIELD_UUID) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                inputFields[a].setHintText(LocaleController.getString(R.string.VmessProxyUuid));
                inputFields[a].setText(currentProxyInfo.uuid);
            } else if (a == FIELD_SECURITY) {
                inputFields[a].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                inputFields[a].setHintText(LocaleController.getString(R.string.VmessProxySecurity));
                inputFields[a].setText(currentProxyInfo.security);
            }
            inputFields[a].setSelection(inputFields[a].length());
        }

        tlsCell = new TextCheckCell(context);
        tlsCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        tlsCell.setTextAndCheck(LocaleController.getString(R.string.VmessProxyTls), currentProxyInfo.tls, false);
        tlsCell.setOnClickListener(v -> {
            currentProxyInfo.tls = !currentProxyInfo.tls;
            tlsCell.setChecked(currentProxyInfo.tls);
        });
        linearLayout.addView(tlsCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ShadowSectionCell shadowCell = new ShadowSectionCell(context);
        linearLayout.addView(shadowCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateDoneEnabled();
        return fragmentView;
    }

    private void updateDoneEnabled() {
        if (doneItem == null || inputFields == null) {
            return;
        }
        boolean enabled = inputFields[FIELD_SERVER].length() > 0 && Utilities.parseInt(inputFields[FIELD_PORT].getText().toString()) > 0;
        doneItem.setEnabled(enabled);
        doneItem.setAlpha(enabled ? 1f : 0.5f);
    }

    private void saveProxy() {
        currentProxyInfo.server = inputFields[FIELD_SERVER].getText().toString();
        currentProxyInfo.port = Utilities.parseInt(inputFields[FIELD_PORT].getText().toString());
        currentProxyInfo.uuid = inputFields[FIELD_UUID].getText().toString();
        currentProxyInfo.security = inputFields[FIELD_SECURITY].getText().toString();

        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        SharedPreferences.Editor editor = preferences.edit();
        boolean enabled;
        if (addingNewProxy) {
            SharedConfig.addVmessProxy(currentProxyInfo);
            SharedConfig.currentVmessProxy = currentProxyInfo;
            editor.putBoolean("vmess_proxy_enabled", true);
            enabled = true;
        } else {
            enabled = preferences.getBoolean("vmess_proxy_enabled", false);
            SharedConfig.saveVmessProxyList();
        }
        if (addingNewProxy || SharedConfig.currentVmessProxy == currentProxyInfo) {
            editor.putString("vmess_proxy_server", currentProxyInfo.server);
            editor.putInt("vmess_proxy_port", currentProxyInfo.port);
            editor.putString("vmess_proxy_uuid", currentProxyInfo.uuid);
            editor.putString("vmess_proxy_security", currentProxyInfo.security);
            editor.putBoolean("vmess_proxy_tls", currentProxyInfo.tls);
            editor.putBoolean("vmess_proxy_enabled", enabled);
        }
        editor.apply();
    }
}
