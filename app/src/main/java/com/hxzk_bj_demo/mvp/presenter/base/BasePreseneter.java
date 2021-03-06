package com.hxzk_bj_demo.mvp.presenter.base;

import com.hxzk_bj_demo.mvp.view.base.BaseView;

/**
 * 作者：created by ${zjt} on 2019/3/29
 * 描述:
 */
public class BasePreseneter<V extends BaseView> {
    public V mView;

    public void onAttachView(V v){
        this.mView=v;
    }


    public void onDettachView(){
       if(mView != null){
           mView = null;
       }
    }


    public boolean isBindView(){
        return mView != null;
    }
}
