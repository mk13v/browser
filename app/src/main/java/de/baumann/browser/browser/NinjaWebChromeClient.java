package de.baumann.browser.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.webkit.*;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.NinjaWebView;

public class NinjaWebChromeClient extends WebChromeClient {

    private final NinjaWebView ninjaWebView;

    public NinjaWebChromeClient(NinjaWebView ninjaWebView) {
        super();
        this.ninjaWebView = ninjaWebView;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        ninjaWebView.updateTitle(progress);
        if (Objects.requireNonNull(view.getTitle()).isEmpty()) {
            ninjaWebView.updateTitle(view.getUrl());
        } else {
            ninjaWebView.updateTitle(view.getTitle());
        }
        ninjaWebView.updateFavicon();
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg) {

        Context context = view.getContext();
        WebView newWebView = new WebView(context);
        view.addView(newWebView);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(newWebView);
        resultMsg.sendToTarget();

        newWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Intent browserIntent = new Intent(context, BrowserActivity.class);
                browserIntent.setAction(Intent.ACTION_VIEW);
                browserIntent.setData(request.getUrl());
                context.startActivity(browserIntent);
                return true;
            }
        });
        return true;
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        ninjaWebView.getBrowserController().onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        ninjaWebView.getBrowserController().onHideCustomView();
        super.onHideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        ninjaWebView.getBrowserController().showFileChooser(filePathCallback);
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Activity activity =  (Activity) ninjaWebView.getContext();
        HelperUnit.grantPermissionsLoc(activity);
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request){
        Activity activity =  (Activity) ninjaWebView.getContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ninjaWebView.getContext());
        String[] resources = request.getResources();
        boolean videoGranted=false;
        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                if (sp.getBoolean("sp_camera", false)) {
                    //Reminder: switch off setMediaPlaybackRequiresUserGesture; done in NinjaWebView if sp_camera TRUE
                    HelperUnit.grantPermissionsCam(activity);
                    videoGranted = true;
                    request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE,PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                }
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                if (sp.getBoolean("sp_microphone", false)) {
                    HelperUnit.grantPermissionsMic(activity);
                    if (!videoGranted) request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});  //otherwise crash:  Either grant() or deny() has been already called.
                }
            } else if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(resource)) {

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ninjaWebView.getContext());
                builder.setMessage(R.string.hint_DRM_Media);
                builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                    request.grant(new String[]{PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID});
                });
                builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> {
                    request.deny();
                });
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        }

    }

    @Override
    public void onReceivedIcon(WebView view, Bitmap icon) {
        super.onReceivedIcon(view, icon);
        ninjaWebView.setFavicon(icon);
    }

    @Override
    public void onReceivedTitle(WebView view, String sTitle) {
        super.onReceivedTitle(view, sTitle);
    }
}
