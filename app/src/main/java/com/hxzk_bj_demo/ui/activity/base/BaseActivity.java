package com.hxzk_bj_demo.ui.activity.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.hxzk.bj.x5webview.statusbartextcolor.StatebusTextColorUtil;
import com.hxzk_bj_demo.R;

import com.hxzk_bj_demo.common.Const;
import com.hxzk_bj_demo.common.MainApplication;
import com.hxzk_bj_demo.interfaces.ThemeChangeObserver;
import com.hxzk_bj_demo.javabean.EventBuseBean;
import com.hxzk_bj_demo.network.interceptor.AddInterceptor;
import com.hxzk_bj_demo.ui.activity.LoginActivity;
import com.hxzk_bj_demo.ui.activity.MainActivity;
import com.hxzk_bj_demo.utils.LogUtil;
import com.hxzk_bj_demo.utils.activity.ActivityJump;
import com.umeng.analytics.MobclickAgent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.hxzk.bj.x5webview.statusbartextcolor.StatebusTextColorUtil.setLightStatusBar;
import static com.hxzk_bj_demo.common.Const.KEY_COOKIE;

/**
 * Created by Ding on 2017/12/24
 * 作用：
 */

public abstract class BaseActivity extends AppCompatActivity implements ThemeChangeObserver {
    private static final String TAG = "BaseActivity";

    /**
     * 接受子viwe的上下文
     */
    protected Context _context;
    /**
     * 加载子视图内容区域
     */
    LinearLayout layout_ContentView_Base;
    /**
     * buterknife绑定对象
     */
    Unbinder unbinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //注册当前Activity主题的监听，并设置相关主题
        setupActivityBeforeCreate();
        super.onCreate(savedInstanceState);
        //将当前界面子类的Activity添加到栈中
        ActivityJump.AddToTack(this);
        //打印栈中的activity
        ActivityJump.LogAllActivityNames();
        //竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_base);
        layout_ContentView_Base = findViewById(R.id.layout_contentview_base);
        if (setLayoutId() != 0) {
            View contentView = LayoutInflater.from(this).inflate(setLayoutId(), null);
            layout_ContentView_Base.addView(contentView);
            //绑定Butterknife
            unbinder = ButterKnife.bind(this);

            initView();
            initEvent();
            initData();
        }
    }

    /**
     * 友盟Session启动、App使用时长等基础数据统计接口API：
     */
    @Override
    protected void onResume() {
        super.onResume();
        _context = this;
        MobclickAgent.onResume(this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }


    @Override
    protected void onDestroy() {
        ((MainApplication) getApplication()).unregisterObserver(this);
        super.onDestroy();
        unbinder.unbind();
    }


    /**
     * 先将当前Activity注册
     */
    private void setupActivityBeforeCreate() {
        ((MainApplication) getApplication()).registerObserver(this);
        loadingCurrentTheme();
    }


    @Override
    public void loadingCurrentTheme() {
        //夜间模式
        if (MainApplication.getAppTheme()) {
            setTheme(R.style.Base_CustomTheme_Night);
            StatebusTextColorUtil.setStatusBarColor(this, R.color.custom_color_app_status_bg_night);
            setLightStatusBar(this, false);
        } else {//白天模式
            setTheme(R.style.Base_CustomTheme_Day);
            StatebusTextColorUtil.setStatusBarColor(this, R.color.custom_color_app_status_bg_day);
            setLightStatusBar(this, true);
        }

    }


    /**
     * 设置layout布局文件
     */
    protected abstract int setLayoutId();

    /**
     * 初始化控件
     */
    protected abstract void initView();

    /**
     * 初始化监听事件
     */
    protected abstract void initEvent();

    /**
     * 初始化数据
     */
    protected abstract void initData();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            doActivityResult(requestCode, resultCode, data);
        }
    }

    protected void doActivityResult(int requestCode, int resultCode, Intent intent) {
    }


    /**
     * activity页面登陆失效
     */
    public void resetLoginFun() {
        AddInterceptor.clearCookie(this,KEY_COOKIE);
    ActivityJump.BackToAppointActivity(this, LoginActivity.class);
    }

}
