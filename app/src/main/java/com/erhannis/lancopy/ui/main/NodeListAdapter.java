package com.erhannis.lancopy.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.erhannis.lancopy.databinding.NodeListItemBinding;

import java.util.List;

// https://dbremes.wordpress.com/2017/11/12/data-binding-android-listviews/
public class NodeListAdapter extends BaseAdapter {
    private List<NodeLine> mNodeLines;
    private LayoutInflater mLayoutInflater;

    public NodeListAdapter(List<NodeLine> nodeLines) {
        mNodeLines = nodeLines;
    }

    public void setList(List<NodeLine> nodeLines) {
        mNodeLines = nodeLines;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mNodeLines.size();
    }

    @Override
    public Object getItem(int i) {
        return mNodeLines.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, final ViewGroup viewGroup) {
        View result = view;
        NodeListItemBinding binding;
        if (result == null) {
            if (mLayoutInflater == null) {
                mLayoutInflater = (LayoutInflater) viewGroup.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            binding = NodeListItemBinding.inflate(mLayoutInflater, viewGroup, false);
            result = binding.getRoot();
            result.setTag(binding);
        }
        else {
            binding = (NodeListItemBinding) result.getTag();
        }
        result.setOnClickListener(v -> {
            Context context = viewGroup.getContext();
            Toast toast = Toast.makeText(context,
                    "Position=" + i + ": " + ((NodeLine)getItem(i)).toString(),
                    Toast.LENGTH_SHORT);
            toast.show();
        });
        binding.setNodeLine(mNodeLines.get(i));
        return result;
    }}
