package com.example.adzan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdzanAdapter extends RecyclerView.Adapter<AdzanAdapter.AdzanViewHolder>{
    private Context context;
    private List<AdzanModel> adzanList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AdzanModel item);
    }

    public AdzanAdapter(Context context, List<AdzanModel> adzanList, OnItemClickListener listener) {
        this.context = context;
        this.adzanList = adzanList;
        this.listener = listener;
    }

    @Override
    public AdzanViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_adzan, parent, false);
        return new AdzanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AdzanViewHolder holder, int position) {
        AdzanModel item = adzanList.get(position);
        holder.textNama.setText(item.getNama());
        holder.imageFlag.setImageResource(item.getImageResId());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return adzanList.size();
    }

    public static class AdzanViewHolder extends RecyclerView.ViewHolder {
        TextView textNama;
        ImageView imageFlag;

        public AdzanViewHolder(View itemView) {
            super(itemView);
            textNama = itemView.findViewById(R.id.textNamaAdzan);
            imageFlag = itemView.findViewById(R.id.imageAdzanIcon);
        }
    }
}
