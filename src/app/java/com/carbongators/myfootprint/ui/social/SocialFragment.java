package com.carbongators.myfootprint.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carbongators.myfootprint.FriendModel;
import com.carbongators.myfootprint.FriendRecyclerInterface;
import com.carbongators.myfootprint.Friend_RecyclerViewAdapter;
import com.carbongators.myfootprint.R;
import com.carbongators.myfootprint.databinding.FragmentSocialBinding;

import java.util.ArrayList;

public class SocialFragment extends Fragment implements FriendRecyclerInterface {

    private FragmentSocialBinding binding;

    ArrayList<FriendModel> friendModels = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SocialViewModel socialViewModel =
                new ViewModelProvider(this).get(SocialViewModel.class);

        binding = FragmentSocialBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSocial;
        socialViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        RecyclerView friendRecycler = root.findViewById(R.id.friendRecycler);
        setUpFriendModels();
        Friend_RecyclerViewAdapter friendAdapter = new Friend_RecyclerViewAdapter(getActivity(), friendModels, this);
        friendRecycler.setAdapter(friendAdapter);
        friendRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setUpFriendModels() {
        String[] friendNames = {"Beau Phid", "Barry Dee"};
        String[] friendEmails = {"neezduts@gmail.com", "samirjames@gmail.com"};
        String[] friendCodes = {"690.0", "420.0"};
        int[] images = {R.drawable.unknown_user_48dp, R.drawable.ic_baseline_android_24};
        for(int i = 0; i < friendNames.length; i++)
            friendModels.add(new FriendModel(friendNames[i], friendEmails[i], friendCodes[i], images[i]));
    }

    @Override
    public void onFriendClick(int position) {
        ScrollView homeScreen = (ScrollView) binding.getRoot().findViewById(R.id.socialPage);
        ScrollView friendPage = (ScrollView) binding.getRoot().findViewById(R.id.friendPage);
        TextView friendName = (TextView)  binding.getRoot().findViewById(R.id.fPageName);
        TextView friendEmail = (TextView)  binding.getRoot().findViewById(R.id.fPageEmail);
        TextView friendCode = (TextView)  binding.getRoot().findViewById(R.id.fPageCode);
        ImageView friendImage = (ImageView) binding.getRoot().findViewById(R.id.fImage);
        friendName.setText(friendModels.get(position).getName());
        friendEmail.setText(friendModels.get(position).getEmail());
        friendCode.setText(friendModels.get(position).getFriendCode());
        friendImage.setImageResource(friendModels.get(position).getImage());
        homeScreen.setVisibility(View.GONE);
        friendPage.setVisibility(View.VISIBLE);
    }
}
