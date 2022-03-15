package net.kdt.pojavlaunch.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.modmanager.api.ModData;
import net.kdt.pojavlaunch.modmanager.api.ModResult;
import net.kdt.pojavlaunch.modmanager.api.Modrinth;
import net.kdt.pojavlaunch.modmanager.ModManager;

import java.io.IOException;
import java.util.ArrayList;

public class ModsFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_mods, container, false);

        InstalledModAdapter installedModAdapter = new InstalledModAdapter();
        RecyclerView installedModsRecycler = view.findViewById(R.id.installedModsRecycler);
        installedModsRecycler.setLayoutManager(new LinearLayoutManager(installedModsRecycler.getContext()));
        installedModsRecycler.setAdapter(installedModAdapter);

        APIModAdapter modAPIAdapter = new APIModAdapter(installedModAdapter);
        RecyclerView apiModsRecycler = view.findViewById(R.id.apiModsRecycler);
        apiModsRecycler.setLayoutManager(new LinearLayoutManager(apiModsRecycler.getContext()));
        apiModsRecycler.setAdapter(modAPIAdapter);

        Modrinth.addProjectsToRecycler(modAPIAdapter, "1.18.1", 0, "sodium");
        return view;
    }

    public static class APIModViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ImageView icon;
        private final TextView title;
        private final TextView description;
        private final TextView compatLevel;
        private String slug;
        private final InstalledModAdapter adapter;

        public APIModViewHolder(View itemView, InstalledModAdapter adapter) {
            super(itemView);
            icon = itemView.findViewById(R.id.apiModIcon);
            icon.setOnClickListener(this);
            title = itemView.findViewById(R.id.apiModTitle);
            description = itemView.findViewById(R.id.apiModDescription);
            compatLevel = itemView.findViewById(R.id.compatLevel);
            this.adapter = adapter;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        @Override
        public void onClick(View view) {
            //Stop spamming of same mod
            if (ModManager.isDownloading(slug)) {
                return;
            }

            try {
                ModManager.addMod(adapter, "test", slug, "1.18.2");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class InstalledModViewHolder extends RecyclerView.ViewHolder {

        private final ImageView icon;
        private final TextView title;
        private final TextView filename;
        private final Switch activeSwitch;

        public InstalledModViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.installedModIcon);
            title = itemView.findViewById(R.id.installedModTitle);
            filename = itemView.findViewById(R.id.installedModDescription);
            activeSwitch = itemView.findViewById(R.id.active_switch);
        }
    }

    public static class APIModAdapter extends RecyclerView.Adapter<APIModViewHolder> {

        private final ArrayList<ModResult> mods = new ArrayList<>();
        private final InstalledModAdapter adapter;

        public APIModAdapter(InstalledModAdapter adapter) {
            this.adapter = adapter;
        }

        public void addMods(Modrinth.ModrinthSearchResult result) {
            int posStart = mods.size();
            mods.addAll(result.getHits());
            this.notifyItemRangeChanged(posStart, mods.size());
        }

        @Override
        public int getItemViewType(final int position) {
            return R.layout.item_api_mod;
        }

        @NonNull
        @Override
        public APIModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new APIModViewHolder(view, adapter);
        }

        @Override
        public void onBindViewHolder(@NonNull APIModViewHolder holder, int position) {
            if (mods.size() > position) {
                ModResult modResult = mods.get(position);
                holder.title.setText(modResult.getTitle());
                holder.description.setText(modResult.getDescription());
                holder.compatLevel.setText(ModManager.getModCompat(modResult.getSlug()));
                holder.setSlug(modResult.getSlug());

                if (!modResult.getIconUrl().isEmpty()) {
                    Picasso.get().load(modResult.getIconUrl()).placeholder(R.drawable.ic_menu_no_news).into(holder.icon);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mods.size();
        }
    }

    public static class InstalledModAdapter extends RecyclerView.Adapter<InstalledModViewHolder> {

        private final ArrayList<ModData> mods = new ArrayList<>();

        public InstalledModAdapter() {
            int posStart = mods.size();
            mods.addAll(ModManager.listInstalledMods("test"));
            this.notifyItemRangeChanged(posStart, mods.size());
        }

        public void addMod(ModData modData) {
            int posStart = mods.size();
            mods.add(modData);
            this.notifyItemRangeChanged(posStart, mods.size());
        }

        @Override
        public int getItemViewType(final int position) {
            return R.layout.item_installed_mod;
        }

        @NonNull
        @Override
        public InstalledModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new InstalledModViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InstalledModViewHolder holder, int position) {
            if (mods.size() > position) {
                ModData modData = mods.get(position);
                holder.title.setText(modData.getName());
                holder.filename.setText(modData.getFilename());
                holder.activeSwitch.setChecked(modData.isActive());

                if (!modData.getIconUrl().isEmpty()) {
                    Picasso.get().load(modData.getIconUrl()).placeholder(R.drawable.ic_menu_no_news).into(holder.icon);
                }
            }
        }

        @Override
        public int getItemCount() {
            return mods.size();
        }
    }
}
