package org.nuclearfog.twidda.activity;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;
import com.squareup.picasso.Picasso;

import org.nuclearfog.tag.Tagger;
import org.nuclearfog.tag.Tagger.OnTagClickListener;
import org.nuclearfog.textviewtool.LinkAndScrollMovement;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.FragmentAdapter;
import org.nuclearfog.twidda.backend.UserAction;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.items.Relation;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.DialogBuilder;
import org.nuclearfog.twidda.backend.utils.DialogBuilder.OnDialogClick;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import static android.content.Intent.ACTION_VIEW;
import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.widget.LinearLayout.LayoutParams.WRAP_CONTENT;
import static android.widget.Toast.LENGTH_SHORT;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_LINK;
import static org.nuclearfog.twidda.activity.MediaViewer.KEY_MEDIA_TYPE;
import static org.nuclearfog.twidda.activity.MediaViewer.MEDIAVIEWER_IMAGE;
import static org.nuclearfog.twidda.activity.MessagePopup.KEY_DM_PREFIX;
import static org.nuclearfog.twidda.activity.SearchPage.KEY_SEARCH_QUERY;
import static org.nuclearfog.twidda.activity.TweetActivity.KEY_TWEET_ID;
import static org.nuclearfog.twidda.activity.TweetActivity.KEY_TWEET_NAME;
import static org.nuclearfog.twidda.activity.TweetActivity.LINK_PATTERN;
import static org.nuclearfog.twidda.activity.TweetPopup.KEY_TWEETPOPUP_TEXT;
import static org.nuclearfog.twidda.activity.UserDetail.KEY_USERDETAIL_ID;
import static org.nuclearfog.twidda.activity.UserDetail.KEY_USERDETAIL_MODE;
import static org.nuclearfog.twidda.activity.UserDetail.USERLIST_FOLLOWER;
import static org.nuclearfog.twidda.activity.UserDetail.USERLIST_FRIENDS;
import static org.nuclearfog.twidda.activity.UserLists.KEY_USERLIST_OWNER_ID;
import static org.nuclearfog.twidda.backend.UserAction.Action.ACTION_BLOCK;
import static org.nuclearfog.twidda.backend.UserAction.Action.ACTION_FOLLOW;
import static org.nuclearfog.twidda.backend.UserAction.Action.ACTION_MUTE;
import static org.nuclearfog.twidda.backend.UserAction.Action.ACTION_UNBLOCK;
import static org.nuclearfog.twidda.backend.UserAction.Action.ACTION_UNFOLLOW;
import static org.nuclearfog.twidda.backend.UserAction.Action.ACTION_UNMUTE;
import static org.nuclearfog.twidda.backend.UserAction.Action.PROFILE_DB;
import static org.nuclearfog.twidda.backend.UserAction.Action.PROFILE_lOAD;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.PROFILE_BLOCK;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.PROFILE_MUTE;
import static org.nuclearfog.twidda.backend.utils.DialogBuilder.DialogType.PROFILE_UNFOLLOW;
import static org.nuclearfog.twidda.database.GlobalSettings.BANNER_IMG_HIGH_RES;
import static org.nuclearfog.twidda.database.GlobalSettings.PROFILE_IMG_HIGH_RES;

/**
 * Activity class for user profile page
 */
public class UserProfile extends AppCompatActivity implements OnClickListener, OnTagClickListener,
        OnTabSelectedListener, OnDialogClick {

    /**
     * Key for the user ID
     */
    public static final String KEY_PROFILE_ID = "profile_id";

    /**
     * Alternative Key for the screen name
     */
    public static final String KEY_PROFILE_NAME = "profile_name";

    /**
     * key for user object, alternative to {@link #KEY_PROFILE_ID} and {@link #KEY_PROFILE_NAME}
     */
    public static final String KEY_PROFILE_DATA = "profile_data";

    /**
     * request code for {@link ProfileEditor}
     */
    public static final int REQUEST_PROFILE_CHANGED = 1;

    /**
     * return code if {@link ProfileEditor} changed profile information
     */
    public static final int RETURN_PROFILE_CHANGED = 2;

    /**
     * background color mask for TextView backgrounds
     */
    private static final int TRANSPARENCY = 0xafffffff;

    private FragmentAdapter adapter;
    private GlobalSettings settings;
    private UserAction profileAsync;

    private TextView[] tabTweetCount;
    private TextView txtUser, txtScrName;
    private TextView txtLocation, txtCreated, lnkTxt, bioTxt, follow_back;
    private Button following, follower;
    private ImageView profileImage, bannerImage;
    private View profile_head, profile_layer;
    private ViewPager pager;
    private TabLayout tabLayout;
    private Dialog unfollowConfirm, blockConfirm, muteConfirm;

    @Nullable
    private Relation relation;
    @Nullable
    private User user;

    private String username;
    private long userId;
    private boolean isHomeProfile;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_profile);
        Toolbar tool = findViewById(R.id.profile_toolbar);
        View root = findViewById(R.id.user_view);
        tabLayout = findViewById(R.id.profile_tab);
        bioTxt = findViewById(R.id.bio);
        following = findViewById(R.id.following);
        follower = findViewById(R.id.follower);
        lnkTxt = findViewById(R.id.links);
        profileImage = findViewById(R.id.profile_img);
        bannerImage = findViewById(R.id.profile_banner);
        txtUser = findViewById(R.id.profile_username);
        txtScrName = findViewById(R.id.profile_screenname);
        txtLocation = findViewById(R.id.location);
        profile_head = findViewById(R.id.profile_header);
        profile_layer = findViewById(R.id.profile_layer);
        txtCreated = findViewById(R.id.profile_date);
        follow_back = findViewById(R.id.follow_back);
        pager = findViewById(R.id.profile_pager);

        tool.setTitle("");
        setSupportActionBar(tool);
        settings = GlobalSettings.getInstance(this);

        following.setCompoundDrawablesWithIntrinsicBounds(R.drawable.following, 0, 0, 0);
        follower.setCompoundDrawablesWithIntrinsicBounds(R.drawable.follower, 0, 0, 0);
        txtCreated.setCompoundDrawablesWithIntrinsicBounds(R.drawable.calendar, 0, 0, 0);
        txtLocation.setCompoundDrawablesWithIntrinsicBounds(R.drawable.userlocation, 0, 0, 0);
        lnkTxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.link, 0, 0, 0);
        txtUser.setBackgroundColor(settings.getBackgroundColor() & TRANSPARENCY);
        txtScrName.setBackgroundColor(settings.getBackgroundColor() & TRANSPARENCY);
        follow_back.setBackgroundColor(settings.getBackgroundColor() & TRANSPARENCY);
        bioTxt.setMovementMethod(LinkAndScrollMovement.getInstance());
        lnkTxt.setTextColor(settings.getHighlightColor());
        bioTxt.setLinkTextColor(settings.getHighlightColor());
        AppStyles.setTheme(settings, root);

        adapter = new FragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(pager);
        unfollowConfirm = DialogBuilder.create(this, PROFILE_UNFOLLOW, this);
        blockConfirm = DialogBuilder.create(this, PROFILE_BLOCK, this);
        muteConfirm = DialogBuilder.create(this, PROFILE_MUTE, this);

        Bundle param = getIntent().getExtras();
        if (param != null) {
            Object data = param.getSerializable(KEY_PROFILE_DATA);
            if (data instanceof User) {
                user = (User) data;
                userId = user.getId();
                username = user.getScreenname();
            } else {
                userId = param.getLong(KEY_PROFILE_ID, -1);
                username = param.getString(KEY_PROFILE_NAME, "");
            }
            adapter.setupProfilePage(userId, username);
            tabTweetCount = AppStyles.createTabIcon(tabLayout, settings, R.array.profile_tab_icons);
            isHomeProfile = userId == settings.getCurrentUserId();
        }

        tabLayout.addOnTabSelectedListener(this);
        following.setOnClickListener(this);
        follower.setOnClickListener(this);
        profileImage.setOnClickListener(this);
        bannerImage.setOnClickListener(this);
        lnkTxt.setOnClickListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (profileAsync == null) {
            profileAsync = new UserAction(this, userId, username);
            if (user == null) {
                profileAsync.execute(PROFILE_DB);
            } else {
                profileAsync.execute(PROFILE_lOAD);
                setUser(user);
            }
        }
    }


    @Override
    protected void onDestroy() {
        if (profileAsync != null && profileAsync.getStatus() == RUNNING)
            profileAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onActivityResult(int reqCode, int returnCode, @Nullable Intent i) {
        if (reqCode == REQUEST_PROFILE_CHANGED && returnCode == RETURN_PROFILE_CHANGED) {
            adapter.notifySettingsChanged();
            profileAsync = null;
        }
        super.onActivityResult(reqCode, returnCode, i);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.profile, m);
        AppStyles.setMenuIconColor(m, settings.getIconColor());
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        if (user != null) {
            if (user.followRequested()) {
                MenuItem followIcon = m.findItem(R.id.profile_follow);
                AppStyles.setMenuItemColor(followIcon, Color.YELLOW);
                followIcon.setTitle(R.string.menu_follow_requested);
            }
            if (user.isLocked() && !isHomeProfile) {
                MenuItem listItem = m.findItem(R.id.profile_lists);
                listItem.setVisible(false);
            }
        }
        if (relation != null) {
            if (relation.isFriend()) {
                MenuItem followIcon = m.findItem(R.id.profile_follow);
                MenuItem listItem = m.findItem(R.id.profile_lists);
                AppStyles.setMenuItemColor(followIcon, Color.CYAN);
                followIcon.setTitle(R.string.menu_user_unfollow);
                listItem.setVisible(true);
            }
            if (relation.isBlocked()) {
                MenuItem blockIcon = m.findItem(R.id.profile_block);
                blockIcon.setTitle(R.string.menu_user_unblock);
            }
            if (relation.isMuted()) {
                MenuItem muteIcon = m.findItem(R.id.profile_mute);
                muteIcon.setTitle(R.string.menu_unmute_user);
            }
            if (relation.canDm()) {
                MenuItem dmIcon = m.findItem(R.id.profile_message);
                dmIcon.setVisible(true);
            }
            if (relation.isFollower()) {
                follow_back.setVisibility(VISIBLE);
            }
        }
        if (isHomeProfile) {
            MenuItem dmIcon = m.findItem(R.id.profile_message);
            MenuItem setting = m.findItem(R.id.profile_settings);
            dmIcon.setVisible(true);
            setting.setVisible(true);
        } else {
            MenuItem followIcon = m.findItem(R.id.profile_follow);
            MenuItem blockIcon = m.findItem(R.id.profile_block);
            MenuItem muteIcon = m.findItem(R.id.profile_mute);
            followIcon.setVisible(true);
            blockIcon.setVisible(true);
            muteIcon.setVisible(true);
        }
        return super.onPrepareOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // write tweet
        if (item.getItemId() == R.id.profile_tweet) {
            Intent tweet = new Intent(this, TweetPopup.class);
            if (!isHomeProfile && user != null) {
                // add username to tweet
                String tweetPrefix = user.getScreenname() + " ";
                tweet.putExtra(KEY_TWEETPOPUP_TEXT, tweetPrefix);
            }
            startActivity(tweet);
        }
        // follow / unfollow user
        else if (item.getItemId() == R.id.profile_follow) {
            if (user != null && relation != null) {
                if (!relation.isFriend()) {
                    profileAsync = new UserAction(this, user);
                    profileAsync.execute(ACTION_FOLLOW);
                } else if (!unfollowConfirm.isShowing()) {
                    unfollowConfirm.show();
                }
            }
        }
        // mute user
        else if (item.getItemId() == R.id.profile_mute) {
            if (user != null && relation != null) {
                if (relation.isMuted()) {
                    profileAsync = new UserAction(this, user);
                    profileAsync.execute(ACTION_UNMUTE);
                } else if (!muteConfirm.isShowing()) {
                    muteConfirm.show();
                }
            }
        }
        // block user
        else if (item.getItemId() == R.id.profile_block) {
            if (user != null && relation != null) {
                if (relation.isBlocked()) {
                    profileAsync = new UserAction(this, user);
                    profileAsync.execute(ACTION_UNBLOCK);
                } else if (!blockConfirm.isShowing()) {
                    blockConfirm.show();
                }
            }
        }
        // open profile editor
        else if (item.getItemId() == R.id.profile_settings) {
            Intent editProfile = new Intent(this, ProfileEditor.class);
            startActivityForResult(editProfile, REQUEST_PROFILE_CHANGED);
        }
        // open direct message
        else if (item.getItemId() == R.id.profile_message) {
            if (user != null) {
                Intent dmPage;
                if (isHomeProfile) {
                    dmPage = new Intent(this, DirectMessage.class);
                } else {
                    dmPage = new Intent(this, MessagePopup.class);
                    dmPage.putExtra(KEY_DM_PREFIX, user.getScreenname());
                }
                startActivity(dmPage);
            }
        }
        // open users list
        else if (item.getItemId() == R.id.profile_lists) {
            Intent listPage = new Intent(this, UserLists.class);
            listPage.putExtra(KEY_USERLIST_OWNER_ID, userId);
            startActivity(listPage);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (tabLayout.getSelectedTabPosition() > 0) {
            pager.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void onTagClick(String text) {
        Intent intent = new Intent(this, SearchPage.class);
        intent.putExtra(KEY_SEARCH_QUERY, text);
        startActivity(intent);
    }


    @Override
    public void onLinkClick(String tag) {
        String shortLink;
        // remove query from link if exists
        int cut = tag.indexOf('?');
        if (cut > 0) {
            shortLink = tag.substring(0, cut);
        } else {
            shortLink = tag;
        }
        // link points to a tweet
        if (LINK_PATTERN.matcher(shortLink).matches()) {
            String name = shortLink.substring(20, shortLink.indexOf('/', 20));
            long id = Long.parseLong(shortLink.substring(shortLink.lastIndexOf('/') + 1));
            Intent intent = new Intent(this, TweetActivity.class);
            intent.putExtra(KEY_TWEET_ID, id);
            intent.putExtra(KEY_TWEET_NAME, name);
            startActivity(intent);
        }
        // open link in browser
        else {
            Uri link = Uri.parse(tag);
            Intent intent = new Intent(Intent.ACTION_VIEW, link);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException err) {
                Toast.makeText(this, R.string.error_connection_failed, LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onClick(View v) {
        // open following page
        if (v.getId() == R.id.following) {
            if (user != null && relation != null) {
                if (!user.isLocked() || relation.isFriend() || isHomeProfile) {
                    Intent following = new Intent(this, UserDetail.class);
                    following.putExtra(KEY_USERDETAIL_ID, userId);
                    following.putExtra(KEY_USERDETAIL_MODE, USERLIST_FRIENDS);
                    startActivity(following);
                }
            }
        }
        // open follower page
        else if (v.getId() == R.id.follower) {
            if (user != null && relation != null) {
                if (!user.isLocked() || relation.isFriend() || isHomeProfile) {
                    Intent follower = new Intent(this, UserDetail.class);
                    follower.putExtra(KEY_USERDETAIL_ID, userId);
                    follower.putExtra(KEY_USERDETAIL_MODE, USERLIST_FOLLOWER);
                    startActivity(follower);
                }
            }
        }
        // open link added to profile
        else if (v.getId() == R.id.links) {
            if (user != null && !user.getLink().isEmpty()) {
                String link = user.getLink();
                Intent browserIntent = new Intent(ACTION_VIEW, Uri.parse(link));
                try {
                    startActivity(browserIntent);
                } catch (ActivityNotFoundException err) {
                    Toast.makeText(this, R.string.error_connection_failed, LENGTH_SHORT).show();
                }
            }
        }
        // open profile image
        else if (v.getId() == R.id.profile_img) {
            if (user != null) {
                Intent mediaImage = new Intent(this, MediaViewer.class);
                mediaImage.putExtra(KEY_MEDIA_LINK, new String[]{user.getImageLink()});
                mediaImage.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMAGE);
                startActivity(mediaImage);
            }
        }
        // open banner image
        else if (v.getId() == R.id.profile_banner) {
            if (user != null) {
                Intent mediaBanner = new Intent(this, MediaViewer.class);
                mediaBanner.putExtra(KEY_MEDIA_LINK, new String[]{user.getBannerLink() + BANNER_IMG_HIGH_RES});
                mediaBanner.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMAGE);
                startActivity(mediaBanner);
            }
        }
    }


    @Override
    public void onConfirm(DialogBuilder.DialogType type) {
        if (user != null) {
            profileAsync = new UserAction(this, user);
            // confirmed unfollowing user
            if (type == PROFILE_UNFOLLOW) {
                profileAsync.execute(ACTION_UNFOLLOW);
            }
            // confirmed blocking user
            else if (type == PROFILE_BLOCK) {
                profileAsync.execute(ACTION_BLOCK);
            }
            // confirmed muting user
            else if (type == PROFILE_MUTE) {
                profileAsync.execute(ACTION_MUTE);
            }
        }
    }


    @Override
    public void onTabSelected(Tab tab) {
    }


    @Override
    public void onTabUnselected(Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }


    @Override
    public void onTabReselected(Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }


    /**
     * Set User Information
     *
     * @param user User data
     */
    public void setUser(User user) {
        this.user = user;
        NumberFormat formatter = NumberFormat.getIntegerInstance();
        Spanned bio = Tagger.makeTextWithLinks(user.getBio(), settings.getHighlightColor(), this);

        tabTweetCount[0].setText(formatter.format(user.getTweetCount()));
        tabTweetCount[1].setText(formatter.format(user.getFavorCount()));
        following.setText(formatter.format(user.getFollowing()));
        follower.setText(formatter.format(user.getFollower()));
        txtUser.setText(user.getUsername());
        txtScrName.setText(user.getScreenname());

        if (profile_head.getVisibility() != VISIBLE) {
            profile_head.setVisibility(VISIBLE);
            String date = SimpleDateFormat.getDateTimeInstance().format(user.getCreatedAt());
            txtCreated.setText(date);
        }
        if (user.isVerified()) {
            txtUser.setCompoundDrawablesWithIntrinsicBounds(R.drawable.verify, 0, 0, 0);
            AppStyles.setIconColor(txtUser, settings.getIconColor());
        } else {
            txtUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        if (user.isLocked()) {
            txtScrName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, 0, 0);
            AppStyles.setIconColor(txtScrName, settings.getIconColor());
        } else {
            txtScrName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        if (!user.getLocation().isEmpty()) {
            txtLocation.setText(user.getLocation());
            txtLocation.setVisibility(VISIBLE);
        } else {
            txtLocation.setVisibility(GONE);
        }
        if (!user.getBio().isEmpty()) {
            bioTxt.setVisibility(VISIBLE);
            bioTxt.setText(bio);
        } else {
            bioTxt.setVisibility(GONE);
        }
        if (!user.getLink().isEmpty()) {
            String link = user.getLink();
            if (link.startsWith("http://"))
                lnkTxt.setText(link.substring(7));
            else if (link.startsWith("https://"))
                lnkTxt.setText(link.substring(8));
            else
                lnkTxt.setText(link);
            lnkTxt.setVisibility(VISIBLE);
        } else {
            lnkTxt.setVisibility(GONE);
        }
        if (settings.getImageLoad()) {
            if (user.hasBannerImg()) {
                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int layoutHeight = displaySize.x / 3;
                int buttonHeight = (int) getResources().getDimension(R.dimen.profile_button_height);
                profile_layer.getLayoutParams().height = layoutHeight + buttonHeight;
                String bannerLink = user.getBannerLink() + settings.getBannerSuffix();
                Picasso.get().load(bannerLink).error(R.drawable.no_banner).into(bannerImage);
            } else {
                bannerImage.setImageResource(0);
                profile_layer.getLayoutParams().height = WRAP_CONTENT;
            }
            profile_layer.requestLayout();
            String imgLink = user.getImageLink();
            if (!user.hasDefaultProfileImage())
                imgLink += PROFILE_IMG_HIGH_RES;
            Picasso.get().load(imgLink).error(R.drawable.no_image).into(profileImage);
        }
    }

    /**
     * sets user relation information and checks for status changes
     *
     * @param relation relation to an user
     */
    public void onAction(Relation relation) {
        if (this.relation != null) {
            // check if block status changed
            if (relation.isBlocked() != this.relation.isBlocked()) {
                if (relation.isBlocked()) {
                    Toast.makeText(this, R.string.info_user_blocked, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.info_user_unblocked, Toast.LENGTH_SHORT).show();
                }
            }
            // check if following status changed
            else if (relation.isFriend() != this.relation.isFriend()) {
                if (relation.isFriend()) {
                    Toast.makeText(this, R.string.info_followed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.info_unfollowed, Toast.LENGTH_SHORT).show();
                }
            }
            // check if mute status changed
            else if (relation.isMuted() != this.relation.isMuted()) {
                if (relation.isMuted()) {
                    Toast.makeText(this, R.string.info_user_muted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.info_user_unmuted, Toast.LENGTH_SHORT).show();
                }
            }
        }
        this.relation = relation;
        invalidateOptionsMenu();
    }

    /**
     * called if an error occurs
     *
     * @param err Engine Exception
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(this, err);
        if (user == null || err.resourceNotFound()) {
            finish();
        }
    }
}