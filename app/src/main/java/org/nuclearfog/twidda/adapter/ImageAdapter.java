package org.nuclearfog.twidda.adapter;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.lang.ref.WeakReference;


public class ImageAdapter extends Adapter<ImageAdapter.ImageHolder> {

    private WeakReference<OnImageClickListener> itemClickListener;
    private Bitmap[] images;


    public ImageAdapter(OnImageClickListener l) {
        itemClickListener = new WeakReference<>(l);
        images = new Bitmap[0];
    }


    public void setImages(@NonNull Bitmap[] images) {
        this.images = images;
    }


    @Override
    public int getItemCount() {
        return images.length;
    }


    @NonNull
    @Override
    public ImageAdapter.ImageHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        return new ImageHolder(imageView);
    }


    @Override
    public void onBindViewHolder(@NonNull final ImageAdapter.ImageHolder vh, int index) {
        final Bitmap image = images[index];
        float ratio = image.getHeight() / 256.0f;
        int destWidth = (int) (image.getWidth() / ratio);
        Bitmap result = Bitmap.createScaledBitmap(image, destWidth, 256, false);

        vh.item.setImageBitmap(result);
        vh.item.setBackgroundColor(0xffffffff);
        vh.item.setPadding(1, 1, 1, 1);
        vh.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener.get() != null)
                    itemClickListener.get().onImageClick(image);
            }
        });
        vh.item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener.get() != null)
                    return itemClickListener.get().onImageTouch(image);
                return false;
            }
        });
    }


    class ImageHolder extends ViewHolder {
        final ImageView item;

        ImageHolder(ImageView item) {
            super(item);
            this.item = item;
        }
    }


    public interface OnImageClickListener {
        /**
         * simple click on image_add
         *
         * @param image selected image_add bitmap
         */
        void onImageClick(Bitmap image);

        /**
         * long touch on image_add
         *
         * @param image selected image_add bitmap
         * @return perform onImageClick ?
         */
        boolean onImageTouch(Bitmap image);
    }
}