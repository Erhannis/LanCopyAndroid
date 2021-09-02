package com.erhannis.lancopy.ui.main;

import com.erhannis.lancopy.refactor.Summary;

//TODO Extract to another intermediate library?
public class NodeLine {
    public final Summary summary;

    public NodeLine(Summary summary) {
        this.summary = summary;
    }

    @Override
    public String toString() {
        return summary.timestamp + "|" + summary.id + " - " + summary.summary;
    }
}
