package com.tongji.wangjimin.tongjinews.fragment;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tongji.wangjimin.tongjinews.NewsApplication;
import com.tongji.wangjimin.tongjinews.NewsContentActivity;
import com.tongji.wangjimin.tongjinews.R;
import com.tongji.wangjimin.tongjinews.adapter.ImportNewsAdapter;
import com.tongji.wangjimin.tongjinews.data.ImportNewsLoaderWithCache;
import com.tongji.wangjimin.tongjinews.data.NewsReaderDbHelper;
import com.tongji.wangjimin.tongjinews.net.News;
import com.tongji.wangjimin.tongjinews.view.RefreshRecyclerView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by wangjimin on 17/2/28.
 * ImportNewsFragment.
 */

public class ImportNewsFragment extends Fragment {
    /**
     * NoLeak Handler
     */
    private static class ReceiveHandler extends Handler {
        private final WeakReference<ImportNewsFragment> fragment;
        ReceiveHandler(ImportNewsFragment fragment){
            this.fragment = new WeakReference<>(fragment);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ImportNewsFragment actualFragment = fragment.get();
            if(actualFragment != null){
                if(msg.what == 0){
                    actualFragment.mAdapter.setDataAndNotify(NewsApplication.getInstance().getNewsList());
                    actualFragment.mSwipeLayout.setRefreshing(false);
                }
                else if(msg.what == 1){
                    actualFragment.mAdapter.addAll(actualFragment.mNewsList);
                    //不需要删除列表中最后一项的加载页面，因为 Notify 信息源变更后，信息会自动更新，加载页面会自动更新到新的最后的位置。
//                    actualFragment.mAdapter.removeData(msg.what);
                } else {
                    actualFragment.mSwipeLayout.setRefreshing(false);
                }
            }
        }
    }
    private final ReceiveHandler mHandler = new ReceiveHandler(this);
    private RefreshRecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeLayout;
    private ImportNewsLoaderWithCache mNewsListLoader;
    private ImportNewsAdapter mAdapter;
    private List<News> mNewsList;
    private boolean isFristVisiable = true;
    private NewsReaderDbHelper mDbHelper;
    private SQLiteDatabase mDb;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNewsListLoader = ImportNewsLoaderWithCache.getInstance(getContext());
        mAdapter = new ImportNewsAdapter(getContext());
        mDbHelper = new NewsReaderDbHelper(getContext());
        //todo Background?
        mDb = mDbHelper.getWritableDatabase();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_importnews, container, false);
        mRecyclerView = (RefreshRecyclerView) root.findViewById(R.id.recyclerview_main);
        mSwipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.swiperefresh_main);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mNewsListLoader.loadRefresh(null);
                        mHandler.sendEmptyMessage(2);
                    }
                }).start();
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        mAdapter.setOnItemClickListener(position -> {
            Intent intent = new Intent(getActivity(), NewsContentActivity.class);
            //Activity 之间传递对象.
            intent.putExtra("newsinfo", mAdapter.getNews(position));
            startActivity(intent);
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setRefreshWork(new RefreshRecyclerView.Refresher() {
            @Override
            public void refresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mNewsListLoader.loadWithNet(new ImportNewsLoaderWithCache.ILoadingWithCacheDone() {
                            @Override
                            public void loadDone(List<News> newsList) {
                                mNewsList = newsList;
                                mHandler.sendEmptyMessage(1);
                                mRecyclerView.setLoadingDone();
                            }
                        }, false);
                    }
                }).start();
            }
        });
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if(getUserVisibleHint() && isFristVisiable){
            isFristVisiable = false;
            mSwipeLayout.setRefreshing(true);
            loadData();
        }
    }

    /**
     * Quick scroll to top of the screen.
     */
    public void scrollToTop(){
        if(mRecyclerView == null){
            return;
        }
        LinearLayoutManager lManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int firstvisableItem = lManager.findFirstVisibleItemPosition();
        int visibleCount = lManager.findLastVisibleItemPosition() - firstvisableItem;
        if(firstvisableItem > visibleCount*2){
            mRecyclerView.scrollToPosition(visibleCount*2);
        }
        mRecyclerView.smoothScrollToPosition(0);
    }

    private void loadData(){
        new FirstLoadTask().execute(this);
    }

    /**
     * No Memory Leak.
     */
    private static class FirstLoadTask extends AsyncTask<Object, Void, Void>{
        private WeakReference<ImportNewsFragment> fragment;
        @Override
        protected Void doInBackground(Object... params) {
            fragment = new WeakReference<>((ImportNewsFragment) params[0]);
            ImportNewsFragment actFragment = fragment.get();
            if(actFragment != null){
                NewsApplication.getInstance().setNewsList(
                        actFragment.mNewsListLoader.loadWithCache(new ImportNewsLoaderWithCache.ILoadingWithCacheDone() {
                    @Override
                    public void loadDone(List<News> newsList) {
                        NewsApplication.getInstance().setNewsList(newsList);
                        actFragment.mHandler.sendEmptyMessage(0);
                    }
                }));
                actFragment.mHandler.sendEmptyMessage(0);
            }
            return null;
        }
    }

    public void reloadData(){
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
