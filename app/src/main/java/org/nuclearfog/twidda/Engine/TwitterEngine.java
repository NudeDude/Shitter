package org.nuclearfog.twidda.Engine;

import org.nuclearfog.twidda.DataBase.TrendDatabase;
import org.nuclearfog.twidda.DataBase.TweetDatabase;
import org.nuclearfog.twidda.MainActivity;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.ViewAdapter.TimelineAdapter;
import org.nuclearfog.twidda.ViewAdapter.TrendsAdapter;

import android.support.v4.widget.SwipeRefreshLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.content.Context;
import android.os.AsyncTask;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TwitterEngine extends AsyncTask<Integer, Void, Boolean>
{
    private TwitterStore twitterStore;
    private Context context;

    private SwipeRefreshLayout timelineRefresh, trendRefresh, mentionRefresh;
    private ListView timelineList, trendList, mentionList;
    private TimelineAdapter timelineAdapter, mentionAdapter;
    private TrendsAdapter trendsAdapter;


    /**
     * Main View
     * @see MainActivity
     */
    public TwitterEngine(Context context) {
        this.context=context;
        twitterStore = TwitterStore.getInstance(context);
        twitterStore.init();
    }

    @Override
    protected void onPreExecute() {
        // Timeline Tab
        timelineRefresh = (SwipeRefreshLayout)((MainActivity)context).findViewById(R.id.timeline);
        timelineList = (ListView)((MainActivity)context).findViewById(R.id.tl_list);
        // Trend Tab
        trendRefresh = (SwipeRefreshLayout)((MainActivity)context).findViewById(R.id.trends);
        trendList = (ListView)((MainActivity)context).findViewById(R.id.tr_list);
        // Mention Tab
        mentionRefresh = (SwipeRefreshLayout)((MainActivity)context).findViewById(R.id.mention);
        mentionList = (ListView)((MainActivity)context).findViewById(R.id.m_list);
    }

    /**
     * @param args [0] Executing Mode: (0)HomeTL, (1)Trend, (2)Mention
     * @return success
     */
    @Override
    protected Boolean doInBackground(Integer... args) {
        Twitter twitter = twitterStore.getTwitter();
        try {
            if(args[0]==0) {
                TweetDatabase mTweets = new TweetDatabase(twitter.getHomeTimeline(), context,TweetDatabase.HOME_TL,0);
                timelineAdapter = new TimelineAdapter(context,mTweets);
            }
            else if(args[0]==1) {
                TrendDatabase trend = new TrendDatabase(twitter.getPlaceTrends(23424829),context); //Germany by default
                trendsAdapter = new TrendsAdapter(context,trend);
            }
            else if(args[0]==2) {
                TweetDatabase mention = new TweetDatabase(twitter.getMentionsTimeline(), context,TweetDatabase.GET_MENT,0);
                mentionAdapter = new TimelineAdapter(context,mention);
            }
        } catch (TwitterException e) {
            return false;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if(success) {
            if(timelineAdapter != null)
                timelineList.setAdapter(timelineAdapter);
            else if(trendsAdapter != null)
                trendList.setAdapter(trendsAdapter);
            else if(mentionAdapter != null)
                mentionList.setAdapter(mentionAdapter);
        } else {
            Toast.makeText(context, context.getString(R.string.connection_failure), Toast.LENGTH_LONG).show();
        }
        if(timelineRefresh.isRefreshing())
            timelineRefresh.setRefreshing(false);
        else if(mentionRefresh.isRefreshing())
            mentionRefresh.setRefreshing(false);
        else if(trendRefresh.isRefreshing())
            trendRefresh.setRefreshing(false);
    }
}