package com.carbongators.myfootprint;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class News_RecyclerViewAdapter extends RecyclerView.Adapter<News_RecyclerViewAdapter.MyViewHolder>{
    Context context;
    ArrayList<NewsArticleModel> newsArticleModels;
    public News_RecyclerViewAdapter(Context context, ArrayList<NewsArticleModel> newsArticleModels) {
        this.context = context;
        this.newsArticleModels = newsArticleModels;
    }

    @NonNull
    @Override
    public News_RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.newsrow, parent, false);
        return new News_RecyclerViewAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull News_RecyclerViewAdapter.MyViewHolder holder, int position) {
        holder.newsSource.setText(newsArticleModels.get(position).getNewsSource());
        holder.articleTitle.setText(newsArticleModels.get(position).getArticleTitle());
        holder.imageView.setImageResource(newsArticleModels.get(position).getImage());
    }

    @Override
    public int getItemCount() {
        return newsArticleModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView newsSource, articleTitle;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView2);
            newsSource = itemView.findViewById(R.id.textView21);
            articleTitle = itemView.findViewById(R.id.textView22);
        }
    }
}
