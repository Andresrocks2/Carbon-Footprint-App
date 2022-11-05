package com.carbongators.myfootprint.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.carbongators.myfootprint.databinding.FragmentSocialBinding;

public class AccountFragment extends Fragment {

    private FragmentSocialBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountViewModel socialViewModel =
                new ViewModelProvider(this).get(AccountViewModel.class);

        binding = FragmentSocialBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textSocial;
        socialViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
