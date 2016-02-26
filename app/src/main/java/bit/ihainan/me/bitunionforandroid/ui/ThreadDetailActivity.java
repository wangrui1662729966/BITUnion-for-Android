package bit.ihainan.me.bitunionforandroid.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.core.type.TypeReference;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bit.ihainan.me.bitunionforandroid.R;
import bit.ihainan.me.bitunionforandroid.adapters.PostListAdapter;
import bit.ihainan.me.bitunionforandroid.models.ThreadReply;
import bit.ihainan.me.bitunionforandroid.ui.assist.SimpleDividerItemDecoration;
import bit.ihainan.me.bitunionforandroid.ui.assist.SwipeActivity;
import bit.ihainan.me.bitunionforandroid.utils.Api;
import bit.ihainan.me.bitunionforandroid.utils.CommonUtils;
import bit.ihainan.me.bitunionforandroid.utils.Global;
import bit.ihainan.me.bitunionforandroid.utils.HtmlUtil;

public class ThreadDetailActivity extends SwipeActivity {
    private final static String TAG = ThreadDetailActivity.class.getSimpleName();

    // UI references
    private TextView mToolbarTitle, mBigTitle;
    private AppBarLayout mAppbar;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ImageButton mChangeOrder;
    private TextView mChangeOrderNew;
    private LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);

    // Bundle tags
    public final static String THREAD_ID_TAG = "THREAD_ID_TAG";
    public final static String THREAD_NAME_TAG = "THREAD_NAME_TAG";
    public final static String THREAD_AUTHOR_NAME_TAG = "THREAD_AUTHOR_NAME_TAG";
    public final static String THREAD_REPLY_COUNT_TAG = "THREAD_REPLY_COUNT_TAG";

    private void getExtra() {
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mTid = bundle.getLong(THREAD_ID_TAG);
        mThreadName = bundle.getString(THREAD_NAME_TAG);
        mReplyCount = bundle.getLong(THREAD_REPLY_COUNT_TAG);
        mAuthorName = bundle.getString(THREAD_AUTHOR_NAME_TAG);
        if (mTid == null) {
            Global.readConfig(this);
            mTid = 10609296l;
        }
    }

    // Data
    private Long mTid, mReplyCount;
    private String mThreadName, mAuthorName;
    private long mCurrentPosition = 0;
    private boolean mIsLoading = false;
    private List<bit.ihainan.me.bitunionforandroid.models.ThreadReply> mThreadPostList = new ArrayList<>();
    private PostListAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thread_detail);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get thread name and id
        getExtra();

        // Toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setTitle("");

        mToolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        mToolbarTitle.setText(CommonUtils.truncateString(mThreadName, 15));
        mBigTitle = (TextView) findViewById(R.id.big_title);
        mBigTitle.setText(mThreadName == null ? "" : mThreadName);
        mAppbar = (AppBarLayout) findViewById(R.id.app_bar);
        mAppbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset <= -(toolbar.getHeight())) {
                    mToolbarTitle.setVisibility(View.VISIBLE);
                } else {
                    mToolbarTitle.setVisibility(View.INVISIBLE);
                }
            }
        });

        if (mThreadName == null || mReplyCount == null) {
            // TODO: 获取标题信息和回复数目信息
        } else {
        }


        // Setup RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.detail_recycler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.home_swipe_refresh_layout);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRecyclerView.smoothScrollToPosition(0);
            }
        });
        setupRecyclerView();
        setupSwipeRefreshLayout();

        // Swipe to back
        setSwipeAnyWhere(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.thread_detail_menu, menu);
        setMenuIcon(menu.findItem(R.id.change_order));
        return true;
    }

    private void setMenuIcon(MenuItem menuItem) {
        if (Global.ascendingOrder)
            menuItem.setIcon(R.drawable.ic_low_priority_white_24dp);
        else
            menuItem.setIcon(R.drawable.ic_high_priority_white_24dp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.change_order:
                Global.ascendingOrder = !Global.ascendingOrder;
                setMenuIcon(item);
                Snackbar.make(mRecyclerView, (Global.ascendingOrder ? "升序" : "降序") + "显示回帖列表", Snackbar.LENGTH_SHORT).show();
                Global.saveConfig(ThreadDetailActivity.this);

                reloadData();
                break;
        }

        return true;
    }

    private void setupRecyclerView() {
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(ThreadDetailActivity.this));
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Adapter
        mAdapter = new PostListAdapter(this, mThreadPostList, mAuthorName, mReplyCount);
        mRecyclerView.setAdapter(mAdapter);

        // 自动加载
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int mLastVisibleItem = mLayoutManager.findLastVisibleItemPosition();
                if (dy > 0 && mLastVisibleItem >= mThreadPostList.size() - 2 && !mIsLoading) {
                    loadMore(true);
                }
            }
        });
    }

    private void setupSwipeRefreshLayout() {
        mSwipeRefreshLayout.setDistanceToTriggerSync(Global.SWIPE_LAYOUT_TRIGGER_DISTANCE);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 重新加载数据
                reloadData();
            }
        });

        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                // 第一次加载数据
                reloadData();
            }
        });
    }

    /**
     * 重新拉取数据
     */
    private void reloadData() {
        mIsLoading = true;
        mSwipeRefreshLayout.setRefreshing(true);
        mThreadPostList.clear();
        mCurrentPosition = Global.ascendingOrder ? 0 : mReplyCount - Global.LOADING_REPLIES_COUNT;
        loadMore(false);
    }

    private void loadMore(boolean isAddProgressBar) {
        // 拉取数据，显示进度
        Log.i(TAG, "onScrolled >> 即将到底，准备请求新数据");
        if (isAddProgressBar) {
            mThreadPostList.add(null);
            mAdapter.notifyItemInserted(mThreadPostList.size() - 1);
            mIsLoading = true;
        }

        refreshData(mCurrentPosition, mCurrentPosition + Global.LOADING_REPLIES_COUNT - 1); // 0 - 9, 10 - 19
    }

    /**
     * 更新列表数据
     */
    private void refreshData(final long from, final long to) {
        long newFrom = from < 0 ? 0 : from;
        long newTo = to > mReplyCount - 1 ? mReplyCount - 1 : to;
        Api.getPostReplies(this, mTid, newFrom, newTo + 1,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mSwipeRefreshLayout.setRefreshing(false);

                        if (Api.checkStatus(response)) {
                            try {
                                JSONArray newListJson = response.getJSONArray("postlist");
                                List<bit.ihainan.me.bitunionforandroid.models.ThreadReply> newThreads = Api.MAPPER.readValue(newListJson.toString(),
                                        new TypeReference<List<ThreadReply>>() {
                                        });

                                // 成功拿到数据，删除 Loading Progress Bar
                                if (mThreadPostList.size() > 0) {
                                    mThreadPostList.remove(mThreadPostList.size() - 1);
                                    mAdapter.notifyItemRemoved(mThreadPostList.size());
                                }

                                CommonUtils.debugToast(ThreadDetailActivity.this, "Loaded " + newThreads.size() + " more item(s)");

                                // 处理数据
                                for (ThreadReply reply : newThreads) {
                                    String body = CommonUtils.decode(reply.message);
                                    reply.useMobile = body.contains("From BIT-Union Open API Project");
                                    HtmlUtil htmlUtil = new HtmlUtil(CommonUtils.decode(reply.message));
                                    reply.message = htmlUtil.makeAll();
                                }

                                // 更新 RecyclerView
                                if (!Global.ascendingOrder) Collections.reverse(newThreads); // 倒序
                                mCurrentPosition += (Global.ascendingOrder ? Global.LOADING_REPLIES_COUNT : -Global.LOADING_REPLIES_COUNT);
                                mThreadPostList.addAll(newThreads);
                                mAdapter.notifyDataSetChanged();

                                // 判断是否到头
                                if (!(Global.ascendingOrder && to >= mReplyCount - 1
                                        || !Global.ascendingOrder && from <= 0)) {
                                    mIsLoading = false;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, getString(R.string.error_parse_json) + "\n" + response, e);

                                if (mThreadPostList.size() > 0) {
                                    mThreadPostList.remove(mThreadPostList.size() - 1);
                                    mAdapter.notifyItemRemoved(mThreadPostList.size());
                                }

                                Snackbar.make(mRecyclerView, getString(R.string.error_parse_json),
                                        Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        loadMore(true);
                                    }
                                }).show();
                            }
                        } else {
                            Log.i(TAG, "refreshData >> " + getString(R.string.error_unknown_json) + "" + response);

                            if (mThreadPostList.size() > 0) {
                                mThreadPostList.remove(mThreadPostList.size() - 1);
                                mAdapter.notifyItemRemoved(mThreadPostList.size());
                            }

                            Snackbar.make(mRecyclerView, getString(R.string.error_unknown_json), Snackbar.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // 服务器请求失败，说明网络不好，只能通过 RETRY 来重新拉取数据
                        if (mThreadPostList.size() > 0) {
                            mThreadPostList.remove(mThreadPostList.size() - 1);
                            mAdapter.notifyItemRemoved(mThreadPostList.size());
                        }

                        mSwipeRefreshLayout.setRefreshing(false);

                        Snackbar.make(mRecyclerView, getString(R.string.error_network), Snackbar.LENGTH_INDEFINITE).setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                loadMore(true);
                            }
                        }).show();

                        Log.e(TAG, getString(R.string.error_network), error);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 友盟 SDK
        if (Global.uploadData)
            MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 友盟 SDK
        if (Global.uploadData)
            MobclickAgent.onPause(this);
    }
}