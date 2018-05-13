package ch.uzh.ifi.seal.ase.cscc;

import cc.kave.commons.model.events.completionevents.CompletionEvent;

public interface OnEventFoundCallback {
    void onEventFound(CompletionEvent evt);
}
