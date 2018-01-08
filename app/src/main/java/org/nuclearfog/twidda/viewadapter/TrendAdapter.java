package org.nuclearfog.twidda.viewadapter;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.database.ColorPreferences;
import org.nuclearfog.twidda.database.TrendDatabase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TrendAdapter extends ArrayAdapter {
    private TrendDatabase trend;
    private Context context;
    private ColorPreferences mcolor;

    public TrendAdapter(Context context, TrendDatabase trend) {
        super(context, R.layout.trend);
        this.trend = trend;
        this.context = context;
        mcolor = ColorPreferences.getInstance(context);
    }

    public TrendDatabase getDatabase() {
        return trend;
    }

    @Override
    public int getCount() {
        return trend.getSize();
    }

    @NonNull
    @Override
    public View getView(int position, View v, @NonNull ViewGroup parent) {
        if(v == null) {
            LayoutInflater inf=(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inf.inflate(R.layout.trend, parent,false);
            v.setBackgroundColor(mcolor.getBackgroundColor());
        }
        String trendName = trend.getTrendname(position);
        ((TextView) v.findViewById(R.id.trendname)).setText(trendName);
        return v;
    }
}