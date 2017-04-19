package com.fyp.faaiz.ets.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fyp.faaiz.ets.R;
import com.fyp.faaiz.ets.model.Employee;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zubairibrahim on 4/15/17.
 */

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.MVH> {

    Context myContext;
    ArrayList<Employee> mdata;

    public EmployeeAdapter(Context context, ArrayList<Employee> data) {
        this.myContext = context;
        this.mdata = data;
    }

    @Override
    public MVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.employee_list_item, parent, false);
        MVH mvh = new MVH(v);
        return mvh;
    }

    @Override
    public void onBindViewHolder(MVH holder, int position) {
        holder.title.setText(mdata.get(position).getFullName());
        holder.desc.setText(mdata.get(position).getEmail());
    }

    @Override
    public int getItemCount() {
        return mdata.size();
    }

    class MVH extends RecyclerView.ViewHolder {
        TextView title;
        TextView desc;
        ImageView imageView;

        public MVH(View itemView) {
            super(itemView);
            this.title = (TextView) itemView.findViewById(R.id.employee_name);
            this.desc = (TextView) itemView.findViewById(R.id.employee_desc);
            this.imageView = (ImageView) itemView.findViewById(R.id.employee_image);
        }
    }
}