package com.amplifyframework.datastore.debug;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.datastore.R;

import java.util.ArrayList;
import java.util.List;

public class DataStoreDebuggerDetailActivity extends AppCompatActivity {
    private DataStoreDetailRecyclerViewAdapter adapter;
    private TextView emptyView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datastore_debugger);
        String modelName = getIntent().getStringExtra("modelName");
        setTitle(modelName);

        RecyclerView recyclerView = findViewById(R.id.item_list);
        this.adapter = new DataStoreDetailRecyclerViewAdapter();

        ModelElement element = DataStoreDebugger.instance().getElement(modelName);
        List<DataStoreEvent> events = element.getEvents();
        this.adapter.setItems(element.getEvents());
        recyclerView.setAdapter(adapter);
        this.adapter.notifyDataSetChanged();

        this.emptyView = findViewById(R.id.empty_view);
        emptyView.setText("No data.");
        if(events.size() > 0) {
            emptyView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.VISIBLE);
        }
    }

    private class DataStoreDetailRecyclerViewAdapter extends RecyclerView.Adapter<DataStoreDetailRecyclerViewAdapter.ViewHolder> {
        private List<DataStoreEvent> items;

        public void setItems(List<DataStoreEvent> items) {
            this.items = new ArrayList<>(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new DataStoreDetailRecyclerViewAdapter.ViewHolder(inflater.inflate(R.layout.list_content, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DataStoreEvent event = items.get(position);
            holder.contentView.setText(event.getDescription());
            switch(event.getEventState()) {
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
