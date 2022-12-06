package com.carbongators.myfootprint.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carbongators.myfootprint.FriendModel;
import com.carbongators.myfootprint.Friend_RecyclerViewAdapter;
import com.carbongators.myfootprint.R;
import com.carbongators.myfootprint.databinding.FragmentSocialBinding;

import java.util.ArrayList;

public class SocialFragment extends Fragment {

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
        Friend_RecyclerViewAdapter friendAdapter = new Friend_RecyclerViewAdapter(getActivity(), friendModels);
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
        String[] friendNames = {"Beau Phid"};
        String[] friendEmails = {"neezduts@gmail.com"};
        String[] friendCodes = {"696969"};
        int[] images = {R.drawable.unknown_user_48dp};
        for(int i = 0; i < friendNames.length; i++)
            friendModels.add(new FriendModel(friendNames[i], friendEmails[i], friendCodes[i], images[i]));
    }
}
