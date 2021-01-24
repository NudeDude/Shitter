package org.nuclearfog.twidda.activity;

import android.graphics.Bitmap;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.ImageAdapter;
import org.nuclearfog.twidda.adapter.ImageAdapter.OnImageClickListener;
import org.nuclearfog.twidda.backend.ImageLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.holder.ImageHolder;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.zoomview.ZoomView;

import static android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START;
import static android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START;
import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

/**
 * Media viewer activity for images and videos
 *
 * @author nuclearfog
 */
public class MediaViewer extends MediaActivity implements OnImageClickListener,
        OnPreparedListener, OnInfoListener, OnErrorListener {

    /**
     * Key for the media URL, local or online, required
     */
    public static final String KEY_MEDIA_LINK = "media_link";

    /**
     * Key for the media type, required
     * {@link #MEDIAVIEWER_IMG_S}, {@link #MEDIAVIEWER_IMAGE}, {@link #MEDIAVIEWER_VIDEO} or {@link #MEDIAVIEWER_ANGIF}
     */
    public static final String KEY_MEDIA_TYPE = "media_type";

    /**
     * setup media viewer for images from storage
     */
    public static final int MEDIAVIEWER_IMG_S = 1;

    /**
     * setup media viewer for images from twitter
     */
    public static final int MEDIAVIEWER_IMAGE = 2;

    /**
     * setup media viewer for videos
     */
    public static final int MEDIAVIEWER_VIDEO = 3;

    /**
     * setup media viewer for GIF animation
     */
    public static final int MEDIAVIEWER_ANGIF = 4;

    private ImageLoader imageAsync;


    private ProgressBar video_progress;
    private ProgressBar image_progress;
    private MediaController videoController;
    private View imageWindow, videoWindow;
    private RecyclerView imageList;
    private ImageAdapter adapter;
    private VideoView videoView;
    private ZoomView zoomImage;

    private String[] mediaLinks;
    private int type;
    private int videoPos = 0;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_media);
        imageList = findViewById(R.id.image_list);
        imageWindow = findViewById(R.id.image_window);
        videoWindow = findViewById(R.id.video_window);
        image_progress = findViewById(R.id.image_load);
        video_progress = findViewById(R.id.video_load);
        zoomImage = findViewById(R.id.image_full);
        videoView = findViewById(R.id.video_view);
        videoController = new MediaController(this);
        adapter = new ImageAdapter(getApplicationContext(), this);
        videoView.setZOrderOnTop(true);
        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);

        Bundle param = getIntent().getExtras();
        if (param != null && param.containsKey(KEY_MEDIA_LINK)) {
            mediaLinks = param.getStringArray(KEY_MEDIA_LINK);
            type = param.getInt(KEY_MEDIA_TYPE);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (imageWindow.getVisibility() != VISIBLE && videoWindow.getVisibility() != VISIBLE) {
            if (mediaLinks != null && mediaLinks.length > 0) {
                switch (type) {
                    case MEDIAVIEWER_IMG_S:
                        adapter.disableSaveButton();
                    case MEDIAVIEWER_IMAGE:
                        imageWindow.setVisibility(VISIBLE);
                        imageList.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
                        imageList.setAdapter(adapter);
                        if (imageAsync == null) {
                            imageAsync = new ImageLoader(this);
                            imageAsync.execute(mediaLinks);
                        }
                        break;

                    case MEDIAVIEWER_VIDEO:
                        videoView.setMediaController(videoController);
                    case MEDIAVIEWER_ANGIF:
                        videoWindow.setVisibility(VISIBLE);
                        Uri video = Uri.parse(mediaLinks[0]);
                        videoView.setVideoURI(video);
                        break;
                }
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (type == MEDIAVIEWER_VIDEO) {
            videoPos = videoView.getCurrentPosition();
            videoView.pause();
        }
    }


    @Override
    protected void onDestroy() {
        if (imageAsync != null && imageAsync.getStatus() == RUNNING)
            imageAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    protected void onAttachLocation(@Nullable Location location) {
    }


    @Override
    protected void onMediaFetched(int resultType, String path) {
    }


    @Override
    public void onImageClick(Bitmap image) {
        zoomImage.reset();
        zoomImage.setImageBitmap(image);
    }


    @Override
    public void onImageSave(Bitmap image, int pos) {
        String link = mediaLinks[pos];
        String name = "shitter_" + link.substring(link.lastIndexOf('/') + 1);
        storeImage(image, name);
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        if (type == MEDIAVIEWER_ANGIF) {
            mp.setLooping(true);
        } else {
            videoController.show(0);
            if (videoPos > 0) {
                mp.seekTo(videoPos);
            }
        }
        mp.setOnInfoListener(this);
        mp.start();
    }


    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MEDIA_INFO_BUFFERING_END:
            case MEDIA_INFO_VIDEO_RENDERING_START:
                video_progress.setVisibility(INVISIBLE);
                return true;

            case MEDIA_INFO_BUFFERING_START:
                video_progress.setVisibility(VISIBLE);
                return true;
        }
        return false;
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MEDIA_ERROR_UNKNOWN) {
            Toast.makeText(this, R.string.error_cant_load_video, Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        return false;
    }

    /**
     * Called from {@link ImageLoader} when all images are downloaded successfully
     */
    public void onSuccess() {
        adapter.disableLoading();
    }

    /**
     * Called from {@link ImageLoader} when an error occurs
     *
     * @param err Exception caught by {@link ImageLoader}
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(getApplicationContext(), err);
        finish();
    }

    /**
     * set downloaded image into preview list
     *
     * @param image Image container
     */
    public void setImage(ImageHolder image) {
        if (adapter.isEmpty()) {
            zoomImage.reset();
            zoomImage.setImageBitmap(image.getMiddleSize());
            image_progress.setVisibility(View.INVISIBLE);
        }
        adapter.addLast(image);
    }
}