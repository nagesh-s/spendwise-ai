package com.spendwise.ai.gamification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<Achievement> achievements;

    public AchievementAdapter(List<Achievement> achievements) {
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);

        holder.iconBadge.setImageResource(achievement.getIconRes());
        holder.textTitle.setText(achievement.getTitle());
        holder.textDescription.setText(achievement.getDescription());

        if (achievement.isUnlocked()) {
            holder.itemView.setAlpha(1.0f);
            holder.iconBadge.clearColorFilter();
            // Optional: Add glow or distinct color?
        } else {
            holder.itemView.setAlpha(0.4f);
            // Gray out icon if desired
            holder.iconBadge.setColorFilter(android.graphics.Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconBadge;
        final TextView textTitle;
        final TextView textDescription;

        ViewHolder(View view) {
            super(view);
            iconBadge = view.findViewById(R.id.icon_badge);
            textTitle = view.findViewById(R.id.text_title);
            textDescription = view.findViewById(R.id.text_description);
        }
    }
}
