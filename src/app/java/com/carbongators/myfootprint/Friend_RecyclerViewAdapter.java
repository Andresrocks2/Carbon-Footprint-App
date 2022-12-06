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

public class Friend_RecyclerViewAdapter extends RecyclerView.Adapter<Friend_RecyclerViewAdapter.MyViewHolder>{
    Context context;
    ArrayList<FriendModel> friendModels;
    public Friend_RecyclerViewAdapter(Context context, ArrayList<FriendModel> friendModels) {
        this.context = context;
        this.friendModels = friendModels;
    }

    @NonNull
    @Override
    public Friend_RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.friendrow, parent, false);

        return new Friend_RecyclerViewAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Friend_RecyclerViewAdapter.MyViewHolder holder, int position) {
        holder.name.setText(friendModels.get(position).getName());
        holder.email.setText(friendModels.get(position).getEmail());
        holder.friendCode.setText(friendModels.get(position).getFriendCode());
        holder.imageView.setImageResource(friendModels.get(position).getImage());
    }

    @Override
    public int getItemCount() {
        return friendModels.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView name, email, friendCode;
        public MyViewHolder(@NonNull View itemView) {
            super (itemView);

            imageView = itemView.findViewById(R.id.friendPic);
            name = itemView.findViewById(R.id.friendName);
            email = itemView.findViewById(R.id.friendEmail);
            friendCode = itemView.findViewById(R.id.friendCode);
        }
    }
}
