package com.carbongators.myfootprint.ui.home;

import android.content.Intent;
import android.net.Uri;
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

import com.carbongators.myfootprint.NewsArticleModel;
import com.carbongators.myfootprint.NewsRecyclerInterface;
import com.carbongators.myfootprint.News_RecyclerViewAdapter;
import com.carbongators.myfootprint.R;
import com.carbongators.myfootprint.TipModel;
import com.carbongators.myfootprint.Tip_RecyclerViewAdapter;
import com.carbongators.myfootprint.databinding.FragmentHomeBinding;

import java.util.ArrayList;

public class HomeFragment extends Fragment implements NewsRecyclerInterface {

    private FragmentHomeBinding binding;
    ArrayList<NewsArticleModel> newsArticleModels = new ArrayList<>();
    ArrayList<TipModel> generalTips = new ArrayList<>();
    ArrayList<TipModel> personalTips = new ArrayList<>();
    int[] newsImages = {R.drawable.bill, R.drawable.marine};

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        RecyclerView recyclerView = root.findViewById(R.id.newsRecycler);
        setUpNewsModels();
        News_RecyclerViewAdapter adapter = new News_RecyclerViewAdapter(getActivity(), newsArticleModels, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //Tip Setup
        RecyclerView dailyTipRecyclerView = root.findViewById(R.id.dailytips);
        RecyclerView personalTipRecyclerView = root.findViewById(R.id.personaltips);
        setUpTipModels();
        Tip_RecyclerViewAdapter dailyTipAdapter = new Tip_RecyclerViewAdapter(getActivity(), generalTips);
        Tip_RecyclerViewAdapter personalTipAdapter = new Tip_RecyclerViewAdapter(getActivity(), personalTips);
        dailyTipRecyclerView.setAdapter(dailyTipAdapter);
        dailyTipRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        personalTipRecyclerView.setAdapter(personalTipAdapter);
        personalTipRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setUpNewsModels() {
        String[] newsSources = getResources().getStringArray(R.array.news_source);
        String[] articleTitles = getResources().getStringArray(R.array.article_titles);
        String[] articleLinks = getResources().getStringArray(R.array.article_links);
        newsArticleModels.clear();
        for(int i = 0; i < newsSources.length; i++)
        {
            newsArticleModels.add(new NewsArticleModel(articleTitles[i], newsSources[i], newsImages[i], articleLinks[i]));
        }
    }
    private void setUpTipModels() {
        String[] tipText = getResources().getStringArray(R.array.tips);
        String[] tipType = getResources().getStringArray(R.array.tip_category);
        int[] tipImages = new int[tipText.length];
        generalTips.clear();
        personalTips.clear();
        for(int i = 0; i < tipImages.length; i++)
        {
            if(tipType[i].equals("Energy"))
            {
                tipImages[i] = R.drawable.energy;
            }
            if(tipType[i].equals("Recycling"))
            {
                tipImages[i] = R.drawable.recycle;
            }
            if(tipType[i].equals("Transportation"))
            {
                tipImages[i] = R.drawable.transportation;
            }
        }
        String date = java.time.LocalDate.now().toString();
        int shift = Integer.parseInt(date.substring(date.length()-2));
        for(int i = 0; i < 3; i++)
        {
            generalTips.add(new TipModel(tipText[(7*i+shift) % 10], tipType[(7*i+shift) % 10], tipImages[(7*i+shift) % 10]));
            personalTips.add(new TipModel(tipText[(7*i+shift+1) % 10], tipType[(7*i+shift+1) % 10], tipImages[(7*i+shift+1) % 10]));
        }
    }

    @Override
    public void onItemClick(int position) {
        String s = newsArticleModels.get(position).getLink();
        Uri uri = Uri.parse(s);
        startActivity(new Intent(Intent.ACTION_VIEW,uri));
    }
}
