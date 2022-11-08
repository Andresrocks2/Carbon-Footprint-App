package com.carbongators.myfootprint.ui.account;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.carbongators.myfootprint.GlideApp;
import com.carbongators.myfootprint.MainActivity;
import com.carbongators.myfootprint.R;
import com.carbongators.myfootprint.databinding.FragmentAccountBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;
    View root;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel accountViewModel =
                new ViewModelProvider(this).get(AccountViewModel.class);
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        MainActivity mActivity = (MainActivity) requireActivity();


        Button signOutButton = (Button) root.findViewById(R.id.sign_out);
        signOutButton.setOnClickListener((View view) -> mActivity.signOut());


        TextView tokenExpiresAtText = (TextView) root.findViewById(R.id.token_expires_at);


        tokenExpiresAtText.setText(mActivity.getAccessTokenExp());


        mActivity.fetchUserInfo();

        View userInfoCard = root.findViewById(R.id.userinfo_card);
        JSONObject userInfo = mActivity.mUserInfoJson.get();

        if (userInfo == null) {
            userInfoCard.setVisibility(View.INVISIBLE);
        } else {
            try {
                String name = "???";
                if (userInfo.has("name")) {
                    name = userInfo.getString("name");
                }
                ((TextView) root.findViewById(R.id.userinfo_name)).setText(name);

                if (userInfo.has("picture")) {
                    GlideApp.with(this)
                        .load(Uri.parse(userInfo.getString("picture")))
                        .fitCenter()
                        .into((ImageView) root.findViewById(R.id.userinfo_profile));
                }

                ((TextView) root.findViewById(R.id.userinfo_json)).setText(userInfo.toString());
                userInfoCard.setVisibility(View.VISIBLE);
            } catch (JSONException ex) {
                Log.e("AccountFragment", "Failed to read userinfo JSON", ex);
            }
        }



        //final TextView textView = binding.textAccount;
        //accountViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
