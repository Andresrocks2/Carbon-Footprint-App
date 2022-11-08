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

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationServiceDiscovery;

import org.joda.time.format.DateTimeFormat;
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
