package com.carbongators.myfootprint;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Tip_RecyclerViewAdapter extends RecyclerView.Adapter<Tip_RecyclerViewAdapter.MyViewHolder>{

    Context context;
    ArrayList<TipModel> tipModels;
    public Tip_RecyclerViewAdapter(Context context, ArrayList<TipModel> tipModels) {
        this.context = context;
        this.tipModels = tipModels;
    }

    @NonNull
    @Override
    public Tip_RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.tiprow, parent, false);
        return new Tip_RecyclerViewAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Tip_RecyclerViewAdapter.MyViewHolder holder, int position) {
        holder.tipText.setText(tipModels.get(position).getTipText());
        holder.tipType.setText(tipModels.get(position).getTipType());
        holder.imageView.setImageResource(tipModels.get(position).getImage());
    }

    @Override
    public int getItemCount() {
        return tipModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView tipText, tipType;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.tipImage);
            tipText = itemView.findViewById(R.id.tipText);
            tipType = itemView.findViewById(R.id.tipType);

        }
    }
}
