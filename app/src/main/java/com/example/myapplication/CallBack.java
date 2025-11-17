package com.example.myapplication;

public interface CallBack {
    /* this CallBack interface is used for synchronize the progress of the apps and database.
    Otherwise, the asynchronization cause bugs.
    For example, using UserManager.currentUser.firstTime before the reading is done would cause bug.
    Callback make it possible that the code would executed only after the reading is done.
    */
    void onComplete();
}
