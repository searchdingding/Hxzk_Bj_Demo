package com.hxzk_bj_demo.ui.activity

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hxzk_bj_demo.R
import com.hxzk_bj_demo.javabean.Data
import com.hxzk_bj_demo.javabean.PointRankBean
import com.hxzk_bj_demo.mvp.constract.RankConstract
import com.hxzk_bj_demo.mvp.model.RankModel
import com.hxzk_bj_demo.mvp.presenter.RankPresenter
import com.hxzk_bj_demo.network.BaseResponse
import com.hxzk_bj_demo.network.HttpRequest
import com.hxzk_bj_demo.ui.activity.base.BaseKtMvpActivity
import com.hxzk_bj_demo.utils.ProgressDialogUtil
import com.hxzk_bj_demo.utils.activity.ActivityJump
import com.hxzk_bj_demo.utils.toastutil.ToastCustomUtil
import com.scwang.smartrefresh.layout.SmartRefreshLayout
import com.scwang.smartrefresh.layout.api.RefreshFooter
import com.scwang.smartrefresh.layout.api.RefreshHeader
import com.scwang.smartrefresh.layout.footer.ClassicsFooter
import com.scwang.smartrefresh.layout.header.ClassicsHeader
import rx.Observable

/**
 * 排行榜Activity
 */
class RankActivity : BaseKtMvpActivity<RankPresenter>() ,RankConstract.RankView {



    lateinit var  smartRefresh : SmartRefreshLayout
    lateinit var  recyclerView: RecyclerView
    lateinit var  tvBack :TextView
    lateinit var  tvTitle :TextView

    lateinit var observable : Observable<BaseResponse<PointRankBean>>

    /**
     * 是否是刷新
     */
    var isRefreshing =false
    /**
     * 是否是加载更多
     */
    var isLoadMore =false

    /**
     * 总页数
     */
    var allPageNum = 0
    /**
     * 当前页数
     */
    var currentPage = 1

    /**
     * 每页显示的个数
     */
    var pageSize = 30

    lateinit var  dataList : MutableList<Data>

    override fun setLayoutId(): Int {
        return R.layout.activity_rank
    }

    override fun initView() {
        dataList = mutableListOf()
        val  rankModel  = RankModel()
        presenter = RankPresenter(rankModel)
        presenter!!.onAttachView(this)
        observable = presenter!!.rankListP(pageNum = currentPage, mContext = this)

        smartRefresh =findViewById(R.id.smartRefresh)
        recyclerView =findViewById(R.id.recycler)
        tvBack =findViewById(R.id.tv_titlebar_back)
        tvTitle= findViewById(R.id.tv_titlebar_title)
    }

    override fun initEvent() {
        tvBack.setOnClickListener {
         ActivityJump.Back(this)
        }
    }

    override fun initData() {
        smartRefresh.let {
            //设置头部刷新样式
            it.setRefreshHeader(ClassicsHeader(this))
            //设置底部刷新样式
            it.setRefreshFooter(ClassicsFooter(this))
            it.setOnRefreshListener {
                isRefreshing = true
                currentPage = 1
                observable = presenter!!.rankListP(pageNum = currentPage, mContext = this)
            }

            it.setOnLoadMoreListener {
                isLoadMore = true
               if(currentPage < allPageNum){
                   currentPage++
                   observable = presenter!!.rankListP(pageNum = currentPage, mContext = this)
               }
            }
        }


    }

    override fun notifyByThemeChanged() {

    }

    override fun onShowLoading() {
       ProgressDialogUtil.getInstance().mshowDialog(this)
    }

    override fun onHiddenLoading() {
        ProgressDialogUtil.getInstance().mdismissDialog()
    }

    override fun onFail(throwable: Throwable?) {
        ToastCustomUtil.showLongToast(throwable?.message)
    }

    override fun rankSuccess(jsonBean: BaseResponse<PointRankBean>) {
        if(jsonBean.isOk){
            allPageNum = if(jsonBean.data.total % pageSize  == 0) (jsonBean.data.total % pageSize ) else (jsonBean.data.total % pageSize + 1)
            if(jsonBean.data.datas.size > 0){
                dataList.addAll(jsonBean.data.datas)

            }
        }else{
            ToastCustomUtil.showLongToast(jsonBean.msg)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        HttpRequest.getInstance().unsubscribe(observable)
    }
    
    }