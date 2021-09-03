package com.erhannis.lancopy.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.erhannis.lancopy.databinding.NodeListItemBinding;

import java.util.List;
import java.util.function.Consumer;

// https://dbremes.wordpress.com/2017/11/12/data-binding-android-listviews/
public class NodeListAdapter extends BaseAdapter {
    private List<NodeLine> mNodeLines;
    private LayoutInflater mLayoutInflater;
    private Consumer<NodeLine> onClickHandler;
    private Consumer<NodeLine> onLongClickHandler;

    public NodeListAdapter(List<NodeLine> nodeLines, Consumer<NodeLine> onClickHandler, Consumer<NodeLine> onLongClickHandler) {
        mNodeLines = nodeLines;
        this.onClickHandler = onClickHandler;
        this.onLongClickHandler = onLongClickHandler;
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
            onClickHandler.accept(((NodeLine)getItem(i)));
        });
        result.setOnLongClickListener(v -> {
            onLongClickHandler.accept(((NodeLine)getItem(i)));
            return true;
        });
        binding.setNodeLine(mNodeLines.get(i));
        return result;
    }}
