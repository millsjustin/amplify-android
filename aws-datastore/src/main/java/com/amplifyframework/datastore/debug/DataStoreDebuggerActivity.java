package com.amplifyframework.datastore.debug;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.datastore.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DataStoreDebuggerActivity extends AppCompatActivity {
    private DataStoreEventRecyclerViewAdapter adapter;
    private TextView emptyView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datastore_debugger);

        RecyclerView recyclerView = findViewById(R.id.item_list);
        this.adapter = new DataStoreEventRecyclerViewAdapter();
        recyclerView.setAdapter(adapter);

        this.emptyView = findViewById(R.id.empty_view);

        DataStoreDebugger.instance().listen(value -> {
            runOnUiThread(() -> {
                this.adapter.setItems(value);
                this.adapter.notifyDataSetChanged();
                emptyView.setText("No data.  Call DataStore.start().");
                if(value.size() > 0) {
                    emptyView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    public class DataStoreEventRecyclerViewAdapter extends RecyclerView.Adapter<DataStoreEventRecyclerViewAdapter.ViewHolder> {
        private List<ModelElement> items;

        public void setItems(List<ModelElement> items) {
            this.items = new ArrayList<>(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ViewHolder(inflater.inflate(R.layout.list_content, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ModelElement element = items.get(position);
            holder.contentView.setText(element.getTitle());
            switch(element.getModelState()) {
                case NOT_STARTED:
                    holder.itemView.setBackgroundResource(R.color.white);
                    break;
                case IN_PROGRESS:
                    holder.itemView.setBackgroundResource(R.color.gold);
                    break;
                case ERROR:
                    holder.itemView.setBackgroundResource(R.color.tomato);
                    break;
                case SUCCESS:
                    holder.itemView.setBackgroundResource(R.color.medium_spring_green);
                    break;
            }
            if(element.getTitle().contains("PENDING_SYNC")) {
                holder.itemView.setBackgroundResource(R.color.gold);
            }
            holder.contentView.setOnClickListener(v -> {
                Intent intent = new Intent(DataStoreDebuggerActivity.this, DataStoreDebuggerDetailActivity.class);
                intent.putExtra("modelName", element.getModelName());
                startActivity(intent);
            });

        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView contentView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                contentView = itemView.findViewById(R.id.content);
            }
        }
    }
}
