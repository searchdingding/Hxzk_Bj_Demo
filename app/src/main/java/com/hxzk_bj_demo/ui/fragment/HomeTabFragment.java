package com.hxzk_bj_demo.ui.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.loadmore.LoadMoreView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;
import com.hxzk.bj.common.X5ActionMessage;
import com.hxzk_bj_demo.R;
import com.hxzk_bj_demo.common.Const;
import com.hxzk_bj_demo.javabean.PublicListData;
import com.hxzk_bj_demo.network.BaseResponse;
import com.hxzk_bj_demo.network.BaseSubscriber;
import com.hxzk_bj_demo.network.HttpRequest;
import com.hxzk_bj_demo.ui.adapter.WechatItemAdapter;
import com.hxzk_bj_demo.ui.fragment.base.BaseFragment;
import com.hxzk_bj_demo.utils.PixelUtil;
import com.hxzk_bj_demo.utils.SPUtils;
import com.hxzk_bj_demo.utils.toastutil.ToastCustomUtil;
import com.wenld.wenldbanner.DefaultPageIndicator;
import com.wenld.wenldbanner.WenldBanner;
import com.wenld.wenldbanner.helper.Holder;
import com.wenld.wenldbanner.helper.ViewHolder;
import com.xzt.xrouter.router.Xrouter;
import com.xzt.xrouter.router.XrouterRequest;
import com.xzt.xrouter.router.XrouterResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * 作者：created by ${zjt} on 2019/2/28
 * 描述:首页中的TabFragment
 */
public class HomeTabFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, BaseQuickAdapter.RequestLoadMoreListener{




    private static final String TAG = "OntherFragment";
    /** 轮播控件WenldBanner*/
    WenldBanner wenldBanner ;
    /**RecyclerView */
    RecyclerView mRecyclerInvest;
    /**@BindView(R.id.swiperrl_invest)*/
    SwipeRefreshLayout mSwiperrlInvest;
    FloatingActionButton mFloatingActionButton;
    NestedScrollView mNestedScrollView;
    /**适配器*/
    WechatItemAdapter mWechatItemAdapter;
    /**只是在初始化的时候调用了,没有实际意义.相当于占位符*/
    private List<PublicListData.DatasBean> mListBeanList;


    /**加载没有数据的view*/
    private View notDataView;
    /**加载错误的view*/
    private View errorView;
    /**Subscriber 所实现的另一个接口,用于取消订阅*/
    private Subscription mSubscription;
    /**页码*/
    private int mPageMark = 1;
    /**刷新状态的标识*/
    public boolean mRefreshMark;
    /**缓存线程池对象**/
    ExecutorService cacheThreadPool;
    //区分公众号的标识
    String publicId;


    Observable<BaseResponse<PublicListData>> mObservable;
    Subscriber<BaseResponse<PublicListData>>  mSubscriber;

    //收藏文章
    Observable<JsonObject> articalObservable;
    Subscriber<JsonObject> articalSubscriber;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Bundle mIntent =getArguments();
        publicId= (String) mIntent.get("publicId");
    }


    @Override
    protected int getLayoutId() {
        return R.layout.fragment_onther;
    }


    @Override
    protected void initView(View view, Bundle savedInstanceState) {
        mRecyclerInvest = (RecyclerView) view.findViewById(R.id.recycler_invest);
        mSwiperrlInvest = (SwipeRefreshLayout) view.findViewById(R.id.swiperrl_invest);
        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab_invest);
        mNestedScrollView = (NestedScrollView) view.findViewById(R.id.nestedsv_invest);
        wenldBanner=view.findViewById(R.id.commonBanner);
    }

    @Override
    protected void initEvent() {
        //FAB的监听
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWechatItemAdapter != null && mWechatItemAdapter.getData() != null && mWechatItemAdapter.getData().size() > 0) {
                    mRecyclerInvest.scrollToPosition(0);
                    ToastCustomUtil.showShortToast("已经返回列表顶部");
                }
            }
        });
    }


    @Override
    protected void initData() {
        //初始化SwipRefrshLayout,默认刷新关闭,设置了主题样式和刷新的监听
        initSwipeRefresh();
        //初始化加载失败和加载数据为空的view
        initEmptyView();
        //初始化RecyclerView
        initRecyclerWechat();
        //初始化Adapater加载更多的监听以及一些属性
        initAdapterLadeMore();
        //刚进入界面执行Loading
        onLoading();
        //创建线程池,此处建议使用ThreadPoolExecutor
        cacheThreadPool= Executors.newCachedThreadPool();
        //请求数据
        requestData();
        //初始化Banner
        initBanner();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //此方法主要作用取消观察者和被观察者的引用,避免内存泄露
        HttpRequest.getInstance().unsubscribe(mObservable);
        HttpRequest.getInstance().unsubscribe(articalObservable);

    }


    @Override
    public void notifyByThemeChanged() {

    }

    /**
     * 初始化Banner
     */
    private void initBanner() {
        List bannerList = new LinkedList<>();
        bannerList.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=255672777,4242622346&fm=27&gp=0.jpg");
        bannerList.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=2232245861,612896867&fm=27&gp=0.jpg");
        bannerList.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2621991350,510510471&fm=27&gp=0.jpg");
        bannerList.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=952379700,3301162203&fm=27&gp=0.jpg");
        bannerList.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=4223445256,1173646510&fm=27&gp=0.jpg");

        Holder holder = new Holder<String>() {
            @Override
            public ViewHolder createView(Context context, ViewGroup parent, int pos, int viewType) {
                return ViewHolder.createViewHolder(context, parent, R.layout.item_vp_home, getViewType(pos));
            }

            @Override
            public void UpdateUI(final Context context, final ViewHolder viewHolder, final int position, final String data) {

                try {
                    cacheThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                //升级到4.8版本之后的新写法
                                RequestOptions requestOptions = new RequestOptions()
                                        .placeholder(R.mipmap.ic_launcher)
                                        .error(R.drawable.errorview)
                                        .centerCrop()
                                        .fallback(new ColorDrawable(Color.RED));

                                final Bitmap myBitmap = Glide.with(context)
                                        .asBitmap() //升级到4.8必须放到这个位置
                                        .load(data)
                                        .apply(requestOptions)
                                        .into(500, 500)
                                        .get();
                                if(myBitmap != null){
                                    final ImageView imageView =viewHolder.getView(R.id.iv_item_vp_home);
                                    //调用View的Post方法更新Ui
                                    imageView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            imageView.setImageBitmap(myBitmap);
                                            imageView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    ToastCustomUtil.showLongToast(position+"");
                                                }
                                            });
                                        }
                                    });

                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                }  catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public int getViewType(int position) {
                return 0;
            }
        };
        //初始化指示器
        DefaultPageIndicator defaultPageIndicator = new DefaultPageIndicator(mContext);
        //设置指示器样式  选中图标与未选中图标
        defaultPageIndicator.setPageIndicator(new int[]{R.mipmap.ic_page_indicator, R.mipmap.ic_page_indicator_focused});

        //设置 view 与 数据
        wenldBanner.setPages(holder, bannerList);
        wenldBanner
                //设置指示器监听
                .setPageIndicatorListener(defaultPageIndicator)
                //设置指示器VIew
                .setIndicatorView(defaultPageIndicator)
                //设置指示器位置
                .setPageIndicatorAlign(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.CENTER_HORIZONTAL);
    }


    /**
     * 加载的loading
     */
    private void onLoading() {
        mWechatItemAdapter.setEmptyView(R.layout.loading_view,
                (ViewGroup) mRecyclerInvest.getParent());
    }


    private void setNewDataAddList( PublicListData  publicListData) {
        if (publicListData != null) {
            mPageMark++;
            List<PublicListData.DatasBean> newData = publicListData.getDatas();
            if (mRefreshMark) {
                //刷新填充数据
                mWechatItemAdapter.setNewData(newData);
                mRefreshMark = false;
            } else {
                //进入fragment走此方法,加载更多填充数据
                mWechatItemAdapter.addData(newData);
            }
            //结束刷新
            if (mSwiperrlInvest.isRefreshing()) {
                mSwiperrlInvest.setRefreshing(false);
            }
            //允许刷新
            if (!mSwiperrlInvest.isEnabled()) {
                mSwiperrlInvest.setEnabled(true);
            }
            //加载更多完成
            if (mWechatItemAdapter.isLoading()) {
                mWechatItemAdapter.loadMoreComplete();
            }
            //允许加载更多
            if (!mWechatItemAdapter.isLoadMoreEnable()) {
                mWechatItemAdapter.setEnableLoadMore(true);
            }
        } else {
            //如果是加载更多就提示加载更多失败
            if (mWechatItemAdapter.isLoading()) {
                //应该是加载更多失败,是框架默认的方法
                mWechatItemAdapter.loadMoreFail();
            } else {
                //没有更多数据
                mWechatItemAdapter.setEmptyView(notDataView);
                if (mSwiperrlInvest.isRefreshing()) {
                    mSwiperrlInvest.setRefreshing(false);
                }
                if (mSwiperrlInvest.isEnabled()) {
                    mSwiperrlInvest.setEnabled(false);
                }
            }
        }

    }


    /**
     * 初始化Adapater加载更多的监听
     */
    private void initAdapterLadeMore() {

        // 设置自定义加载布局  改一
        mWechatItemAdapter.setLoadMoreView(new CustomLoadMoreView());

        //拖拽和滑动删除的回调方法
        //dragAndSwipeMethod();


        mWechatItemAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                mRecyclerInvest.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //关闭下拉刷新
                        mRecyclerInvest.setEnabled(false);
                        requestData();
                    }

                }, 2000);
            }
        }, mRecyclerInvest);


    }



    /**
     * 自定义加载更多的布局
     */
    public final class CustomLoadMoreView extends LoadMoreView {

        @Override
        public int getLayoutId() {
            return R.layout.quick_view_load_more;
        }

        /**
         * 如果返回true，数据全部加载完毕后会隐藏加载更多
         * 如果返回false，数据全部加载完毕后会显示getLoadEndViewId()布局
         */
        @Override
        public boolean isLoadEndGone() {
            return true;
        }

        @Override
        protected int getLoadingViewId() {
            return R.id.load_more_loading_view;
        }

        @Override
        protected int getLoadFailViewId() {
            return R.id.load_more_load_fail_view;
        }

        /**
         * isLoadEndGone()为true，可以返回0
         * isLoadEndGone()为false，不能返回0
         */
        @Override
        protected int getLoadEndViewId() {
            return 0;
        }
    }


    /**
     * 设置RecyclerView属性
     */
    private void initRecyclerWechat() {
        //item点击、长按事件可以使recyclerview也可以是adapter
        // mQuickAdapter.setOnRecyclerViewItemClickListener();
        // mQuickAdapter.setOnRecyclerViewItemLongClickListener();
        mRecyclerInvest.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListBeanList = new ArrayList<>();
        //是否加载图片
        boolean isNotLoad = (boolean) SPUtils.get(getActivity(), Const.SLLMS, false);
        //获取屏幕宽度
        int imgWidth = PixelUtil.getWindowWidth();
        //设置图片的高度,为宽度的3/4
        int imgHeight = imgWidth * 3 / 4;
        mWechatItemAdapter = new WechatItemAdapter(mListBeanList, isNotLoad, imgWidth, imgHeight);

        // mWechatItemAdapter.setOnLoadMoreListener(this);
        // 开启动画(默认为渐显效果)
        mWechatItemAdapter.openLoadAnimation(BaseQuickAdapter.SLIDEIN_RIGHT);
        //动画默认只执行一次,如果想重复执行可设置(即下拉返回的时候)
        mWechatItemAdapter.isFirstOnly(true);
        //设置不显示动画数量(没明白?)
        mWechatItemAdapter.setNotDoAnimationCount(1);
        // 默认提供5种方法（渐显、缩放、从下到上，从左到右、从右到左）
        /* mWechatItemAdapter.openLoadAnimation(BaseQuickAdapter.ALPHAIN);*/
        //使用它添加头部添加尾部
       /* mQuickAdapter.addHeaderView(getView());
        mQuickAdapter.addFooterView(getView());*/

        //setAdapter
        mRecyclerInvest.setAdapter(mWechatItemAdapter);
//        //默认第一次加载会进入加载更多的回调，如果不需要可以配置：
//        mWechatItemAdapter.disableLoadMoreIfNotFullPage();

        //RecyclerView短桉监听.不适用与嵌套recycleView
        mRecyclerInvest.addOnItemTouchListener(new OnItemClickListener() {
            @Override
            public void onSimpleItemClick(BaseQuickAdapter adapter, View view, int position) {
               PublicListData.DatasBean publicListData = (PublicListData.DatasBean) adapter.getData().get(position);
                //封装传递的请求数据到XrouterRequest
                XrouterRequest mXrouterRequest =XrouterRequest.create().putData("data",publicListData.getLink()).putActionName(X5ActionMessage.X5ACTIONNAME);
                XrouterResponse mXrouterResponse=Xrouter.getInstance().senMessage(mContext,mXrouterRequest);
            }
        });


        //Item点击事件.长按监听,适用于嵌套recycleView
        mWechatItemAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {

                return false;
            }
        });

        //item子控件的监听
        mWechatItemAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                switch (view.getId()) {
                    case R.id.title_wechat_style1:
                        ToastCustomUtil.showShortToast("点击了item");
                        break;
                    case R.id.img_collection_invest:
                        PublicListData.DatasBean datasBean = (PublicListData.DatasBean) adapter.getItem(position);
                        collectArtical(datasBean.getId()+"");
                        break;
                }

            }
        });

    }

    /**
     * 设置加载失败和数据为空的View
     */
    private void initEmptyView() {
        // 网络请求没有新数据
        notDataView = getActivity().getLayoutInflater().inflate(R.layout.empty_view, (ViewGroup) mSwiperrlInvest.getParent(), false);
        notDataView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestData();
            }
        });

        // 网络请求错误
        errorView = getActivity().getLayoutInflater().inflate(R.layout.error_view, (ViewGroup) mSwiperrlInvest.getParent(), false);
        errorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestData();
            }
        });
    }


    /**
     * 网络请求失败
     */
    private void onErrorView() {
        mWechatItemAdapter.setEmptyView(errorView);
    }


    /**
     * 设置SwipeRefresh属性
     */
    private void initSwipeRefresh() {
        mSwiperrlInvest.setOnRefreshListener(this);
        mSwiperrlInvest.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);
        mSwiperrlInvest.setEnabled(false);
    }



    /**
     * 请求获取数据
     */
    private void requestData() {
        mSubscriber =new BaseSubscriber<BaseResponse<PublicListData>>(mContext) {
            @Override
            public void onShowLoading() {

            }

            @Override
            public void onHiddenLoading() {

            }

            @Override
            public void onResult(BaseResponse<PublicListData> publicListDataBaseResponse) {
                PublicListData  publicListData =publicListDataBaseResponse.getData();
               setNewDataAddList(publicListData);
            }


            @Override
            public void onFail(Throwable e) {
                onErrorView();
                //判断当前的状态是否是刷新状态
                if (mSwiperrlInvest.isRefreshing()) {
                    mSwiperrlInvest.setRefreshing(false);
                }
                //判断是否禁用此属性
                if (mSwiperrlInvest.isEnabled()) {
                    mSwiperrlInvest.setEnabled(false);
                }
            }

//            @Override
//            public void onFail(ExceptionHandle.ResponeThrowable e) {
//
//            }


        };

        mObservable= HttpRequest.getInstance().getServiceInterface().publicList(publicId,mPageMark+"");
        mSubscription = HttpRequest.getInstance().toSubscribe(mObservable, mSubscriber);
    }

    @Override
    public void onRefresh() {
        mWechatItemAdapter.setEnableLoadMore(false);
        mRefreshMark = true; //是否再刷新的标识
        mPageMark = 1;
        requestData();
    }

    @Override
    public void onLoadMoreRequested() {
        //关闭下拉刷新
        mSwiperrlInvest.setEnabled(false);
        requestData();
    }


    /**
     * 收藏文章列表
     * @param articalId
     */
    private void collectArtical(String articalId){
        articalSubscriber =new Subscriber<JsonObject>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                ToastCustomUtil.showLongToast(e.getMessage());

            }

            @Override
            public void onNext(JsonObject jsonObject) {
                try {
                    JSONObject mJSONObject =new JSONObject(jsonObject.toString());
                    String code=mJSONObject.getString("errorCode");
                    String message=mJSONObject.getString("errorMsg");
                    if(!code.equals("0")){
                        ToastCustomUtil.showLongToast(message);
                    }else{
                        ToastCustomUtil.showLongToast(getString(R.string.toast_collectsuccess));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

        articalObservable =HttpRequest.getInstance().getServiceInterface().collectArical(articalId);
        HttpRequest.getInstance().toSubscribe(articalObservable,articalSubscriber);
    }
}
