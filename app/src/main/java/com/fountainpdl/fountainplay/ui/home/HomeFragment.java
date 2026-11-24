package com.fountainpdl.fountainplay.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import com.fountainpdl.fountainplay.R;
import com.fountainpdl.fountainplay.adapter.MediaCardAdapter;
import com.fountainpdl.fountainplay.model.MediaItem;
import com.fountainpdl.fountainplay.util.Favourites;
import com.fountainpdl.fountainplay.util.Fx;
import com.fountainpdl.fountainplay.util.LibraryRepo;
import com.fountainpdl.fountainplay.util.MediaLauncher;
import com.fountainpdl.fountainplay.util.PlayQueue;
import com.fountainpdl.fountainplay.util.ResumeStore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment implements LibraryRepo.Listener {

    private LibraryRepo repo;
    private View sectionContinue, sectionVideos, sectionMusic, sectionFavs, emptyView, quickActions;
    private RecyclerView rvContinue, rvVideos, rvMusic, rvFavs;
    private TextView tvGreeting, tvSub;
    private boolean animatedOnce = false;
    private List<MediaItem> continueItems = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_home, c, false);
        repo = LibraryRepo.get(requireContext());

        tvGreeting = v.findViewById(R.id.tv_greeting);
        tvSub = v.findViewById(R.id.tv_subgreeting);
        sectionContinue = v.findViewById(R.id.section_continue);
        sectionVideos = v.findViewById(R.id.section_videos);
        sectionMusic = v.findViewById(R.id.section_music);
        sectionFavs = v.findViewById(R.id.section_favs);
        emptyView = v.findViewById(R.id.home_empty);
        rvContinue = v.findViewById(R.id.rv_continue);
        rvVideos = v.findViewById(R.id.rv_new_videos);
        rvMusic = v.findViewById(R.id.rv_new_music);
        rvFavs = v.findViewById(R.id.rv_favs);
        quickActions = v.findViewById(R.id.btn_shuffle_all);

        tvGreeting.setText(greeting());

        v.findViewById(R.id.btn_home_menu).setOnClickListener(this::showMenu);
        v.findViewById(R.id.btn_shuffle_all).setOnClickListener(x -> {
            List<MediaItem> songs = repo.getAudio();
            if (songs.isEmpty()) toast("No music found yet");
            else MediaLauncher.shuffleAudio(requireContext(), songs);
        });
        v.findViewById(R.id.btn_resume).setOnClickListener(x -> resumeLast());
        v.findViewById(R.id.btn_home_refresh).setOnClickListener(x -> {
            repo.refresh();
            toast("Refreshing…");
        });
        Fx.press(v.findViewById(R.id.btn_shuffle_all));
        Fx.press(v.findViewById(R.id.btn_resume));
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        repo.addListener(this);
        repo.ensureLoaded();
        bind();
    }

    @Override public void onStop() {
        super.onStop();
        repo.removeListener(this);
    }

    @Override public void onLibraryChanged() { if (isAdded()) bind(); }

    private String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 12) return "Good morning";
        if (h >= 12 && h < 17) return "Good afternoon";
        if (h >= 17 && h < 22) return "Good evening";
        return "Good night";
    }

    private void bind() {
        final Context ctx = requireContext();
        List<MediaItem> songs = repo.getAudio();
        List<MediaItem> videos = repo.getVideo();

        tvSub.setText(!repo.isLoaded() ? "Loading your library…"
            : songs.isEmpty() && videos.isEmpty() ? "Let's find something to play"
            : songs.size() + " songs · " + videos.size() + " videos");

        // Continue watching: videos with a saved position
        continueItems = new ArrayList<>();
        ResumeStore store = new ResumeStore(ctx);
        for (ResumeStore.Entry e : store.getRecent(12)) {
            MediaItem match = null;
            for (MediaItem m : videos) if (e.path.equals(m.getPath())) { match = m; break; }
            if (match == null) {
                match = new MediaItem(0, e.title, "", e.path, e.duration, MediaItem.TYPE_VIDEO);
                if (!new java.io.File(e.path).exists()) continue;
            }
            continueItems.add(match);
        }
        setCarousel(sectionContinue, rvContinue, continueItems, true, true);
        setCarousel(sectionVideos, rvVideos, head(videos, 12), true, false);
        setCarousel(sectionMusic, rvMusic, head(songs, 12), false, false);

        boolean empty = repo.isLoaded() && songs.isEmpty() && videos.isEmpty() && continueItems.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);

        loadFavourites();

        if (!animatedOnce && repo.isLoaded()) {
            animatedOnce = true;
            ViewGroup content = (ViewGroup) requireView().findViewById(R.id.home_content);
            Fx.enterChildren(content);
        }
    }

    private void loadFavourites() {
        final Context app = requireContext().getApplicationContext();
        new Thread(() -> {
            final List<MediaItem> favs = Favourites.getAll(app);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                setCarousel(sectionFavs, rvFavs, favs, false, false);
            });
        }).start();
    }

    private void setCarousel(View section, RecyclerView rv, List<MediaItem> items, boolean video, boolean progress) {
        if (items.isEmpty()) { section.setVisibility(View.GONE); return; }
        section.setVisibility(View.VISIBLE);
        rv.setAdapter(new MediaCardAdapter(items, video, progress,
            (list, pos) -> MediaLauncher.open(requireContext(), list, pos)));
    }

    private static List<MediaItem> head(List<MediaItem> l, int n) {
        return new ArrayList<>(l.subList(0, Math.min(n, l.size())));
    }

    private void resumeLast() {
        if (!continueItems.isEmpty()) {
            MediaLauncher.open(requireContext(), continueItems, 0);
        } else if (PlayQueue.get().current() != null) {
            startActivity(MediaLauncher.audioIntent(requireContext(), null));
        } else {
            toast("Nothing to resume yet");
        }
    }

    private void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, "History");
        pm.getMenu().add(0, 2, 1, "Manage folders");
        pm.getMenu().add(0, 3, 2, "Refresh library");
        pm.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1: NavHostFragment.findNavController(this).navigate(R.id.nav_history); break;
                case 2: NavHostFragment.findNavController(this).navigate(R.id.nav_manage_folders); break;
                case 3: repo.refresh(); toast("Refreshing…"); break;
            }
            return true;
        });
        pm.show();
    }

    private void toast(String m) { Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show(); }
}
