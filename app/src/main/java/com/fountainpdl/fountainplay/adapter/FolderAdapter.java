package com.fountainpdl.fountainplay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.util.Fx;
import java.util.ArrayList;
import java.util.List;

/** Generic icon + title + subtitle rows: folders, files, playlists, hidden-folder lists. */
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.VH> {

    public static final class Row {
        public String title, subtitle;
        @DrawableRes public int icon = R.drawable.ic_folder;
        @DrawableRes public int actionIcon = 0;   // 0 = no trailing button
        public String actionLabel;
        public Object tag;
        public Row(String title, String subtitle, int icon, Object tag) {
            this.title = title; this.subtitle = subtitle; this.icon = icon; this.tag = tag;
        }
        public Row action(int icon, String label) { this.actionIcon = icon; this.actionLabel = label; return this; }
    }

    public interface Listener {
        void onClick(Row row);
        void onAction(Row row);
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;
    private int lastAnimated = -1;

    public FolderAdapter(Listener l) { listener = l; }

    public void submit(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        lastAnimated = -1;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        Fx.press(v);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Row r = rows.get(position);
        Fx.reset(h.itemView);
        h.title.setText(r.title);
        h.sub.setText(r.subtitle);
        h.sub.setVisibility(r.subtitle == null || r.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        h.icon.setImageResource(r.icon);
        if (r.actionIcon != 0) {
            h.action.setVisibility(View.VISIBLE);
            h.action.setImageResource(r.actionIcon);
            h.action.setContentDescription(r.actionLabel);
            h.action.setOnClickListener(v -> listener.onAction(r));
        } else {
            h.action.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onClick(r));
        if (position > lastAnimated && position < 14) {
            lastAnimated = position;
            Fx.enter(h.itemView, position);
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title, sub; final ImageView icon; final ImageButton action;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tv_folder_name);
            sub = v.findViewById(R.id.tv_folder_sub);
            icon = v.findViewById(R.id.iv_folder_icon);
            action = v.findViewById(R.id.btn_folder_action);
        }
    }
}
